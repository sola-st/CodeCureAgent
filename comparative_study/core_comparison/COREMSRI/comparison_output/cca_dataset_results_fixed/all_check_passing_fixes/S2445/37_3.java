package org.opentripplanner.graph_builder.module.ned;

import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.geotools.geometry.DirectPosition2D;
import org.opengis.coverage.Coverage;
import org.opengis.coverage.PointOutsideCoverageException;
import org.opengis.referencing.operation.TransformException;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.ElevationFlattened;
import org.opentripplanner.graph_builder.issues.ElevationPropagationLimit;
import org.opentripplanner.graph_builder.issues.Graphwide;
import org.opentripplanner.graph_builder.module.extra_elevation_data.ElevationPoint;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetWithElevationEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.util.PolylineEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.opentripplanner.util.ElevationUtils.computeEllipsoidToGeoidDifference;

/**
 * THIS CLASS IS MULTI-THREADED
 * (When configured to do so, it uses parallel streams to distribute elevation calculation tasks for edges.)
 *
 * {@link org.opentripplanner.graph_builder.services.GraphBuilderModule} plugin that applies elevation data to street
 * data that has already been loaded into a (@link Graph}, creating elevation profiles for each Street encountered
 * in the Graph. Data sources that could be used include auto-downloaded and cached National Elevation Dataset (NED)
 * raster data or a GeoTIFF file. The elevation profiles are stored as {@link PackedCoordinateSequence} objects, where
 * each (x,y) pair represents one sample, with the x-coord representing the distance along the edge measured from the
 * start, and the y-coord representing the sampled elevation at that point (both in meters).
 */
public class ElevationModule implements GraphBuilderModule {

    private static final Logger log = LoggerFactory.getLogger(ElevationModule.class);

    /** The elevation data to be used in calculating elevations. */
    private final ElevationGridCoverageFactory gridCoverageFactory;
    /* Whether or not to attempt reading in a file of cached elevations */
    private final boolean readCachedElevations;
    /* Whether or not to attempt writing out a file of cached elevations */
    private final boolean writeCachedElevations;
    /* The file of cached elevations */
    private final File cachedElevationsFile;
    /* Whether or not to include geoid difference values in individual elevation calculations */
    private final boolean includeEllipsoidToGeoidDifference;
    /*
     * Whether or not to use multi-threading when calculating the elevations. For unknown reasons that seem to depend on
     * data and machine settings, it might be faster to use a single processor.
     */
    private final boolean multiThreadElevationCalculations;

    private DataImportIssueStore issueStore;

    /**
     * A map of PackedCoordinateSequence values identified by Strings of encoded polylines.
     *
     * Note: Since this map has a key of only the encoded polylines, it is assumed that all other inputs are the same as
     * those that occurred in the graph build that produced this data.
     */
    private HashMap<String, PackedCoordinateSequence> cachedElevations;

    // Keep track of the proportion of elevation fetch operations that fail so we can issue warnings. AtomicInteger is
    // used to provide thread-safe updating capabilities.
    private AtomicInteger nEdgesProcessed = new AtomicInteger(0);
    private final AtomicInteger nPointsEvaluated = new AtomicInteger(0);
    private final AtomicInteger nPointsOutsideDEM = new AtomicInteger(0);
    /** keeps track of the total amount of elevation edges for logging purposes */
    private int totalElevationEdges = Integer.MAX_VALUE;

    // the first coordinate in the first StreetWithElevationEdge which is used for initializing coverage instances
    private Coordinate examplarCoordinate;

    private final double distanceBetweenSamplesM;

    /**
     * Unit conversion multiplier for elevation values. No conversion needed if the elevation values
     * are defined in meters in the source data. If, for example, decimetres are used in the source data,
     * this should be set to 0.1 in build-config.json.
     */
    private final double elevationUnitMultiplier;

    /** the graph being built */
    private Graph graph;

    /** A concurrent hashmap used for storing geoid difference values at various coordinates */
    private final ConcurrentHashMap<Integer, Double> geoidDifferenceCache = new ConcurrentHashMap<>();

    /** Used only when the ElevationModule is requested to be ran with a single thread */
    private Coverage singleThreadedCoverageInterpolator;

    private ThreadLocal<Coverage> coverageInterpolatorThreadLocal = new ThreadLocal<>();

    /** used only for testing purposes */
    public ElevationModule(ElevationGridCoverageFactory factory) {
        this(
            factory,
            null,
            false,
            false,
            1,
            10,
            true,
            false
        );
    }

    public ElevationModule(
        ElevationGridCoverageFactory factory,
        File cachedElevationsFile,
        boolean readCachedElevations,
        boolean writeCachedElevations,
        double elevationUnitMultiplier,
        double distanceBetweenSamplesM,
        boolean includeEllipsoidToGeoidDifference,
        boolean multiThreadElevationCalculations
    ) {
        gridCoverageFactory = factory;
        this.cachedElevationsFile = cachedElevationsFile;
        this.readCachedElevations = readCachedElevations;
        this.writeCachedElevations = writeCachedElevations;
        this.elevationUnitMultiplier = elevationUnitMultiplier;
        this.includeEllipsoidToGeoidDifference = includeEllipsoidToGeoidDifference;
        this.multiThreadElevationCalculations = multiThreadElevationCalculations;
        this.distanceBetweenSamplesM = distanceBetweenSamplesM;
    }

    /**
     * Gets the desired amount of processors to use for elevation calculations from the build-config setting. It will
     * return at least 1 processor and no more than the maximum available processors. The default return value is 1
     * processor.
     */
    public static int fromConfig(JsonNode elevationModuleParallelism) {
        int maxProcessors = Runtime.getRuntime().availableProcessors();
        int minimumProcessors = 1;
        return Math.max(minimumProcessors, Math.min(elevationModuleParallelism.asInt(minimumProcessors), maxProcessors));
    }

    @Override
    public void buildGraph(
            Graph graph,
            HashMap<Class<?>, Object> extra,
            DataImportIssueStore issueStore
    ) {
        this.issueStore = issueStore;
        this.graph = graph;
        gridCoverageFactory.fetchData(graph);

        graph.setDistanceBetweenElevationSamples(this.distanceBetweenSamplesM);

        // try to load in the cached elevation data
        if (readCachedElevations) {
            // try to load in the cached elevation data
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(cachedElevationsFile));
                cachedElevations = (HashMap<String, PackedCoordinateSequence>) in.readObject();
                log.info("Cached elevation data loaded into memory!");
            } catch (IOException | ClassNotFoundException e) {
                issueStore.add(new Graphwide(
                    String.format("Cached elevations file could not be read in due to error: %s!", e.getMessage())));
            }
        }
        log.info("Setting street elevation profiles from digital elevation model...");

        // At first, set the totalElevationEdges to the total number of edges in the graph.
        totalElevationEdges = graph.countEdges();
        List<StreetWithElevationEdge> streetsWithElevationEdges = new LinkedList<>();
        for (Vertex gv : graph.getVertices()) {
            for (Edge ee : gv.getOutgoing()) {
                if (ee instanceof StreetWithElevationEdge) {
                    if (multiThreadElevationCalculations) {
                        // Multi-threaded execution requested, check and prepare a few things that are used only during
                        // multi-threaded runs.
                        if (examplarCoordinate == null) {
                            // store the first coordinate of the first StreetEdge for later use in initializing coverage
                            // instances
                            examplarCoordinate = ee.getGeometry().getCoordinates()[0];
                        }
                    }
                    streetsWithElevationEdges.add((StreetWithElevationEdge) ee);
                }
            }
        }
        // update this value to the now-known amount of edges that are StreetWithElevation edges
        totalElevationEdges = streetsWithElevationEdges.size();

        if (multiThreadElevationCalculations) {
            // Multi-threaded execution
            streetsWithElevationEdges.parallelStream().forEach(ee -> processEdgeWithProgress(ee));
        } else {
            // If using just a single thread, process each edge inline
            for (StreetWithElevationEdge ee : streetsWithElevationEdges) {
                processEdgeWithProgress(ee);
            }
        }

        double failurePercentage = nPointsOutsideDEM.get() / nPointsEvaluated.get() * 100;
        if (failurePercentage > 50) {
            issueStore.add(new Graphwide(
                String.format(
                    "Fetching elevation failed at %d/%d points (%d%%)",
                    nPointsOutsideDEM, nPointsEvaluated, failurePercentage
                )
            ));
            log.warn("Elevation is missing at a large number of points. DEM may be for the wrong region. " +
                "If it is unprojected, perhaps the axes are not in (longitude, latitude) order.");
        }

        // Iterate again to find edges that had elevation calculated.
        LinkedList<StreetEdge> edgesWithCalculatedElevations = new LinkedList<>();
        for (StreetWithElevationEdge edgeWithElevation : streetsWithElevationEdges) {
            if (edgeWithElevation.hasPackedElevationProfile() && !edgeWithElevation.isElevationFlattened()) {
                edgesWithCalculatedElevations.add(edgeWithElevation);
            }
        }

        if (writeCachedElevations) {
            // write information from edgesWithElevation to a new cache file for subsequent graph builds
            HashMap<String, PackedCoordinateSequence> newCachedElevations = new HashMap<>();
            for (StreetEdge streetEdge : edgesWithCalculatedElevations) {
                newCachedElevations.put(PolylineEncoder.createEncodings(streetEdge.getGeometry()).getPoints(),
                    streetEdge.getElevationProfile());
            }
            try {
                ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(cachedElevationsFile)));
                out.writeObject(newCachedElevations);
                out.close();
            } catch (IOException e) {
                log.error(e.getMessage());
                issueStore.add(new Graphwide("Failed to write cached elevation file!"));
            }
        }
        @SuppressWarnings("unchecked")
        HashMap<Vertex, Double> extraElevation = (HashMap<Vertex, Double>) extra.get(ElevationPoint.class);
        assignMissingElevations(graph, edgesWithCalculatedElevations, extraElevation);
    }

    class ElevationRepairState {
        /* This uses an intuitionist approach to elevation inspection */
        public StreetEdge backEdge;

        public ElevationRepairState backState;

        public Vertex vertex;

        public double distance;

        public double initialElevation;

        public ElevationRepairState(StreetEdge backEdge, ElevationRepairState backState,
                Vertex vertex, double distance, double initialElevation) {
            this.backEdge = backEdge;
            this.backState = backState;
            this.vertex = vertex;
            this.distance = distance;
            this.initialElevation = initialElevation;
        }
    }

    /**
     * Assign missing elevations by interpolating from nearby points with known
     * elevation; also handle osm ele tags
     */
    private void assignMissingElevations(Graph graph, List<StreetEdge> edgesWithElevation, HashMap<Vertex, Double> knownElevations) {

        log.debug("Assigning missing elevations");

        BinHeap<ElevationRepairState> pq = new BinHeap<ElevationRepairState>();

        // elevation for each vertex (known or interpolated)
        // knownElevations will be null if there are no ElevationPoints in the data
        // for instance, with the Shapefile loader.)
        HashMap<Vertex, Double> elevations; 
        if (knownElevations != null)
            elevations = (HashMap<Vertex, Double>) knownElevations.clone();
        else
            elevations = new HashMap<Vertex, Double>();

        // If including the EllipsoidToGeoidDifference, subtract these from the known elevations found in OpenStreetMap
        // data.
        if (includeEllipsoidToGeoidDifference) {
            elevations.forEach((vertex, elevation) -> {
                try {
                    elevations.put(
                        vertex, elevation - getApproximateEllipsoidToGeoidDifference(vertex.getY(), vertex.getX())
                    );
                } catch (TransformException e) {
                    log.error(
                        "Error processing elevation for known elevation at vertex: {} due to error: {}",
                        vertex,
                        e
                    );
                }
            });
        }

        HashSet<Vertex> closed = new HashSet<Vertex>();

        // initialize queue with all vertices which already have known elevation
        for (StreetEdge e : edgesWithElevation) {
            PackedCoordinateSequence profile = e.getElevationProfile();

            if (!elevations.containsKey(e.getFromVertex())) {
                double firstElevation = profile.getOrdinate(0, 1);
                ElevationRepairState state = new ElevationRepairState(null, null,
                        e.getFromVertex(), 0, firstElevation);
                pq.insert(state, 0);
                elevations.put(e.getFromVertex(), firstElevation);
            }

            if (!elevations.containsKey(e.getToVertex())) {
                double lastElevation = profile.getOrdinate(profile.size() - 1, 1);
                ElevationRepairState state = new ElevationRepairState(null, null, e.getToVertex(),
                        0, lastElevation);
                pq.insert(state, 0);
                elevations.put(e.getToVertex(), lastElevation);
            }
        }

        // Grow an SPT outward from vertices with known elevations into regions where the
        // elevation is not known. when a branch hits a region with known elevation, follow the
        // back pointers through the region of unknown elevation, setting elevations via interpolation.
        while (!pq.empty()) {
            ElevationRepairState state = pq.extract_min();

            if (closed.contains(state.vertex)) continue;
            closed.add(state.vertex);

            ElevationRepairState curState = state;
            Vertex initialVertex = null;
            while (curState != null) {
                initialVertex = curState.vertex;
                curState = curState.backState;
            }

            double bestDistance = Double.MAX_VALUE;
            double bestElevation = 0;
            for (Edge e : state.vertex.getOutgoing()) {
                if (!(e instanceof StreetEdge)) {
                    continue;
                }
                StreetEdge edge = (StreetEdge) e;
                Vertex tov = e.getToVertex();
                if (tov == initialVertex)
                    continue;

                Double elevation = elevations.get(tov);
                if (elevation != null) {
                    double distance = e.getDistanceMeters();
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestElevation = elevation;
                    }
                } else {
                    // continue
                    ElevationRepairState newState = new ElevationRepairState(edge, state, tov,
                            e.getDistanceMeters() + state.distance, state.initialElevation);
                    pq.insert(newState, e.getDistanceMeters() + state.distance);
                }
            } // end loop over outgoing edges

            for (Edge e : state.vertex.getIncoming()) {
                if (!(e instanceof StreetEdge)) {
                    continue;
                }
                StreetEdge edge = (StreetEdge) e;
                Vertex fromv = e.getFromVertex();
                if (fromv == initialVertex)
                    continue;
                Double elevation = elevations.get(fromv);
                if (elevation != null) {
                    double distance = e.getDistanceMeters();
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestElevation = elevation;
                    }
                } else {
                    // continue
                    ElevationRepairState newState = new ElevationRepairState(edge, state, fromv,
                            e.getDistanceMeters() + state.distance, state.initialElevation);
                    pq.insert(newState, e.getDistanceMeters() + state.distance);
                }
            } // end loop over incoming edges

            //limit elevation propagation to at max 2km; this prevents an infinite loop
    private final Object graphLock = new Object();

    // ... rest of the class ...

    private void setEdgeElevationProfile(StreetWithElevationEdge ee, PackedCoordinateSequence elevPCS, Graph graph) {
        if(ee.setElevationProfile(elevPCS, false)) {
            synchronized (graphLock) {
                issueStore.add(new ElevationFlattened(ee));
            }
        }
    }
