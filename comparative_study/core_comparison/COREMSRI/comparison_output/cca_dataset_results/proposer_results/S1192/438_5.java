package org.opentripplanner.routing.algorithm;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.algorithm.astar.strategies.MultiTargetTerminationStrategy;
import org.opentripplanner.routing.algorithm.astar.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SimpleConcreteEdge;
import org.opentripplanner.routing.graph.SimpleConcreteVertex;
import org.opentripplanner.routing.graph.TemporaryConcreteEdge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.NonLocalizedString;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AStarTest {

    private Graph graph;

    private static final String V_56TH_24TH = "56th_24th";
    private static final String V_56TH_22ND = "56th_22nd";
    private static final String V_56TH_20TH = "56th_20th";
    private static final String V_MARKET_24TH = "market_24th";
    private static final String V_MARKET_BALLARD = "market_ballard";
    private static final String V_MARKET_22ND = "market_22nd";
    private static final String V_MARKET_LEARY = "market_leary";
    private static final String V_MARKET_RUSSELL = "market_russell";
    private static final String V_MARKET_20TH = "market_20th";
    private static final String V_SHILSHOLE_24TH = "shilshole_24th";
    private static final String V_SHILSHOLE_22ND = "shilshole_22nd";
    private static final String V_SHILSHOLE_VERNON = "shilshole_vernon";
    private static final String V_SHILSHOLE_20TH = "shilshole_20th";
    private static final String V_BALLARD_TURN = "ballard_turn";
    private static final String V_BALLARD_22ND = "ballard_22nd";
    private static final String V_BALLARD_VERNON = "ballard_vernon";
    private static final String V_BALLARD_20TH = "ballard_20th";
    private static final String V_LEARY_VERNON = "leary_vernon";
    private static final String V_LEARY_20TH = "leary_20th";
    private static final String V_RUSSELL_20TH = "russell_20th";

    @Before
    public void before() {

        graph = new Graph();

        vertex(V_56TH_24TH, 47.669457, -122.387577);
        vertex(V_56TH_22ND, 47.669462, -122.384739);
        vertex(V_56TH_20TH, 47.669457, -122.382106);

        vertex(V_MARKET_24TH, 47.668690, -122.387577);
        vertex(V_MARKET_BALLARD, 47.668683, -122.386096);
        vertex(V_MARKET_22ND, 47.668686, -122.384749);
        vertex(V_MARKET_LEARY, 47.668669, -122.384392);
        vertex(V_MARKET_RUSSELL, 47.668655, -122.382997);
        vertex(V_MARKET_20TH, 47.668684, -122.382117);

        vertex(V_SHILSHOLE_24TH, 47.668419, -122.387534);
        vertex(V_SHILSHOLE_22ND, 47.666519, -122.384744);
        vertex(V_SHILSHOLE_VERNON, 47.665938, -122.384048);
        vertex(V_SHILSHOLE_20TH, 47.664356, -122.382192);

        vertex(V_BALLARD_TURN, 47.668509, -122.386069);
        vertex(V_BALLARD_22ND, 47.667624, -122.384744);
        vertex(V_BALLARD_VERNON, 47.666422, -122.383158);
        vertex(V_BALLARD_20TH, 47.665476, -122.382128);

        vertex(V_LEARY_VERNON, 47.666863, -122.382353);
        vertex(V_LEARY_20TH, 47.666682, -122.382160);

        vertex(V_RUSSELL_20TH, 47.667846, -122.382128);

        edges(V_56TH_24TH, V_56TH_22ND, V_56TH_20TH);

        edges(V_56TH_24TH, V_MARKET_24TH);
        edges(V_56TH_22ND, V_MARKET_22ND);
        edges(V_56TH_20TH, V_MARKET_20TH);

        edges(V_MARKET_24TH, V_MARKET_BALLARD, V_MARKET_22ND, V_MARKET_LEARY, V_MARKET_RUSSELL,
                V_MARKET_20TH);
        edges(V_MARKET_24TH, V_SHILSHOLE_24TH, V_SHILSHOLE_22ND, V_SHILSHOLE_VERNON,
                V_SHILSHOLE_20TH);
        edges(V_MARKET_BALLARD, V_BALLARD_TURN, V_BALLARD_22ND, V_BALLARD_VERNON, V_BALLARD_20TH);
        edges(V_MARKET_LEARY, V_LEARY_VERNON, V_LEARY_20TH);
        edges(V_MARKET_RUSSELL, V_RUSSELL_20TH);

        edges(V_MARKET_22ND, V_BALLARD_22ND, V_SHILSHOLE_22ND);
        edges(V_LEARY_VERNON, V_BALLARD_VERNON, V_SHILSHOLE_VERNON);
        edges(V_MARKET_20TH, V_RUSSELL_20TH, V_LEARY_20TH, V_BALLARD_20TH, V_SHILSHOLE_20TH);

    }

    @Test
    public void testForward() {
        RoutingRequest options = new RoutingRequest();
        options.walkSpeed = 1.0;
        options.setRoutingContext(graph, graph.getVertex(V_56TH_24TH), graph.getVertex(V_LEARY_20TH));
        ShortestPathTree tree = new AStar().getShortestPathTree(options);

        GraphPath path = tree.getPath(graph.getVertex(V_LEARY_20TH), false);

        List<State> states = path.states;

        assertEquals(7, states.size());

        assertEquals(V_56TH_24TH, states.get(0).getVertex().getLabel());
        assertEquals(V_MARKET_24TH, states.get(1).getVertex().getLabel());
        assertEquals(V_MARKET_BALLARD, states.get(2).getVertex().getLabel());
        assertEquals(V_MARKET_22ND, states.get(3).getVertex().getLabel());
        assertEquals(V_MARKET_LEARY, states.get(4).getVertex().getLabel());
        assertEquals(V_LEARY_VERNON, states.get(5).getVertex().getLabel());
        assertEquals(V_LEARY_20TH, states.get(6).getVertex().getLabel());
    }

    @Test
    public void testBack() {

        RoutingRequest options = new RoutingRequest();
        options.walkSpeed = 1.0;
        options.setArriveBy(true);
        options.setRoutingContext(graph, graph.getVertex(V_56TH_24TH),
                graph.getVertex(V_LEARY_20TH));
        ShortestPathTree tree = new AStar().getShortestPathTree(options);

        GraphPath path = tree.getPath(graph.getVertex(V_56TH_24TH), false);

        List<State> states = path.states;

        assertTrue(states.size() == 6 || states.size() == 7);

        assertEquals(V_56TH_24TH, states.get(0).getVertex().getLabel());

        int n;
        // we could go either way around the block formed by 56th, 22nd, market, and 24th.
        if (states.size() == 7) {
            assertEquals(V_MARKET_24TH, states.get(1).getVertex().getLabel());
            assertEquals(V_MARKET_BALLARD, states.get(2).getVertex().getLabel());
            n = 0;
        } else {
            assertEquals(V_56TH_22ND, states.get(1).getVertex().getLabel());
            n = -1;
        }

        assertEquals(V_MARKET_22ND, states.get(n + 3).getVertex().getLabel());
        assertEquals(V_MARKET_LEARY, states.get(n + 4).getVertex().getLabel());
        assertEquals(V_LEARY_VERNON, states.get(n + 5).getVertex().getLabel());
        assertEquals(V_LEARY_20TH, states.get(n + 6).getVertex().getLabel());
    }

    @Test
    public void testForwardExtraEdges() {

        RoutingRequest options = new RoutingRequest();
        options.walkSpeed = 1.0;

        TemporaryStreetLocation from = new TemporaryStreetLocation("near_shilshole_22nd",
                new Coordinate(-122.385050, 47.666620), new NonLocalizedString("near_shilshole_22nd"), false);
        new TemporaryConcreteEdge(from, graph.getVertex(V_SHILSHOLE_22ND));

        TemporaryStreetLocation to = new TemporaryStreetLocation("near_56th_20th",
                new Coordinate(-122.382347, 47.669518), new NonLocalizedString("near_56th_20th"), true);
        new TemporaryConcreteEdge(graph.getVertex(V_56TH_20TH), to);

        options.setRoutingContext(graph, from, to);
        ShortestPathTree tree = new AStar().getShortestPathTree(options);
        options.cleanup();

        GraphPath path = tree.getPath(to, false);

        List<State> states = path.states;

        assertEquals(9, states.size());

        assertEquals("near_shilshole_22nd", states.get(0).getVertex().getLabel());
        assertEquals(V_SHILSHOLE_22ND, states.get(1).getVertex().getLabel());
        assertEquals(V_BALLARD_22ND, states.get(2).getVertex().getLabel());
        assertEquals(V_MARKET_22ND, states.get(3).getVertex().getLabel());
        assertEquals(V_MARKET_LEARY, states.get(4).getVertex().getLabel());
        assertEquals(V_MARKET_RUSSELL, states.get(5).getVertex().getLabel());
        assertEquals(V_MARKET_20TH, states.get(6).getVertex().getLabel());
        assertEquals(V_56TH_20TH, states.get(7).getVertex().getLabel());
        assertEquals("near_56th_20th", states.get(8).getVertex().getLabel());
    }

    @Test
    public void testBackExtraEdges() {

        RoutingRequest options = new RoutingRequest();
        options.walkSpeed = 1.0;
        options.setArriveBy(true);

        TemporaryStreetLocation from = new TemporaryStreetLocation("near_shilshole_22nd",
                new Coordinate(-122.385050, 47.666620), new NonLocalizedString("near_shilshole_22nd"), false);
        new TemporaryConcreteEdge(from, graph.getVertex(V_SHILSHOLE_22ND));

        TemporaryStreetLocation to = new TemporaryStreetLocation("near_56th_20th",
                new Coordinate(-122.382347, 47.669518), new NonLocalizedString("near_56th_20th"), true);
        new TemporaryConcreteEdge(graph.getVertex(V_56TH_20TH), to);

        options.setRoutingContext(graph, from, to);
        ShortestPathTree tree = new AStar().getShortestPathTree(options);
        options.cleanup();

        GraphPath path = tree.getPath(from, false);

        List<State> states = path.states;

        assertEquals(9, states.size());

        assertEquals("near_shilshole_22nd", states.get(0).getVertex().getLabel());
        assertEquals(V_SHILSHOLE_22ND, states.get(1).getVertex().getLabel());
        assertEquals(V_BALLARD_22ND, states.get(2).getVertex().getLabel());
        assertEquals(V_MARKET_22ND, states.get(3).getVertex().getLabel());
        assertEquals(V_MARKET_LEARY, states.get(4).getVertex().getLabel());
        assertEquals(V_MARKET_RUSSELL, states.get(5).getVertex().getLabel());
        assertEquals(V_MARKET_20TH, states.get(6).getVertex().getLabel());
        assertEquals(V_56TH_20TH, states.get(7).getVertex().getLabel());
        assertEquals("near_56th_20th", states.get(8).getVertex().getLabel());
    }

    @Test
    public void testMultipleTargets() {
        RoutingRequest options = new RoutingRequest();
        options.walkSpeed = 1.0;
        options.setRoutingContext(graph, graph.getVertex(V_56TH_24TH), graph.getVertex(V_LEARY_20TH));

        Set<Vertex> targets = new HashSet<Vertex>();
        targets.add(graph.getVertex(V_SHILSHOLE_22ND));
        targets.add(graph.getVertex(V_MARKET_RUSSELL));
        targets.add(graph.getVertex(V_56TH_20TH));
        targets.add(graph.getVertex(V_LEARY_20TH));

        SearchTerminationStrategy strategy = new MultiTargetTerminationStrategy(targets);
        ShortestPathTree tree = new AStar().getShortestPathTree(options, -1, strategy);

        for (Vertex v : targets) {
            GraphPath path = tree.getPath(v, false);
            assertNotNull("No path found for target " + v.getLabel(), path);
        }
    }

    /****
     * Private Methods
     ****/

    private SimpleConcreteVertex vertex(String label, double lat, double lon) {
        SimpleConcreteVertex v = new SimpleConcreteVertex(graph, label, lat, lon);
        return v;
    }

    private void edges(String... vLabels) {
        for (int i = 0; i < vLabels.length - 1; i++) {
            Vertex vA = graph.getVertex(vLabels[i]);
            Vertex vB = graph.getVertex(vLabels[i + 1]);

            new SimpleConcreteEdge(vA, vB);
            new SimpleConcreteEdge(vB, vA);
        }
    }
}