package org.opentripplanner.model;

import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This class represents a shortened version of trip times, keeping references to TripTimes
 * and exposing getters to access all fields by delegating to the TripTimes instance.
 * This eliminates the need to copy fields and ensures up-to-date data access.
 */
public class TripTimeShort {

    public static final int UNDEFINED = -1;
    private final TripTimes tripTimes;
    private final int stopIndex;
    private final Stop stop;
    private final ServiceDay serviceDay;

    public TripTimeShort(TripTimes tt, int i, Stop stop) {
        this(tt, i, stop, null);
    }

    public TripTimeShort(TripTimes tt, int i, Stop stop, ServiceDay sd) {
        this.tripTimes = tt;
        this.stopIndex = i;
        this.stop = stop;
        this.serviceDay = sd;
    }

    public FeedScopedId getStopId() {
        return stop.getId();
    }

    public int getStopIndex() {
        return stopIndex;
    }

    public int getStopCount() {
        return tripTimes.getNumStops();
    }

    public int getScheduledArrival() {
        return tripTimes.getScheduledArrivalTime(stopIndex);
    }

    public int getScheduledDeparture() {
        return tripTimes.getScheduledDepartureTime(stopIndex);
    }

    public int getRealtimeArrival() {
        return tripTimes.getArrivalTime(stopIndex);
    }

    public int getRealtimeDeparture() {
        return tripTimes.getDepartureTime(stopIndex);
    }

    public int getArrivalDelay() {
        return tripTimes.getArrivalDelay(stopIndex);
    }

    public int getDepartureDelay() {
        return tripTimes.getDepartureDelay(stopIndex);
    }

    public boolean isTimepoint() {
        return tripTimes.isTimepoint(stopIndex);
    }

    public boolean isRealtime() {
        return !tripTimes.isScheduled();
    }

    public RealTimeState getRealtimeState() {
        return tripTimes.getRealTimeState();
    }

    public long getServiceDay() {
        if (serviceDay == null) {
            return -1;
        }
        return serviceDay.time(0);
    }

    public FeedScopedId getTripId() {
        return tripTimes.trip.getId();
    }

    public String getBlockId() {
        return tripTimes.trip.getBlockId();
    }

    public String getHeadsign() {
        return tripTimes.getHeadsign(stopIndex);
    }

    public int getPickupType() {
        return tripTimes.getPickupType(stopIndex);
    }

    public int getDropoffType() {
        return tripTimes.getDropoffType(stopIndex);
    }

    /**
     * must pass in both table and trip, because tripTimes do not have stops.
     */
    public static List<TripTimeShort> fromTripTimes(Timetable table, Trip trip) {
        TripTimes times = table.getTripTimes(table.getTripIndex(trip.getId()));
        List<TripTimeShort> out = new ArrayList<>();
        // one per stop, not one per hop
        for (int i = 0; i < times.getNumStops(); ++i) {
            out.add(new TripTimeShort(times, i, table.pattern.getStop(i)));
        }
        return out;
    }

    /**
     * must pass in both table and trip, because tripTimes do not have stops.
     * @param serviceDay service day to set, if null none is set
     */
    public static List<TripTimeShort> fromTripTimes(Timetable table, Trip trip,
                                                    ServiceDay serviceDay) {
        TripTimes times = table.getTripTimes(table.getTripIndex(trip.getId()));
        List<TripTimeShort> out = new ArrayList<>();
        // one per stop, not one per hop
        for (int i = 0; i < times.getNumStops(); ++i) {
            out.add(new TripTimeShort(times, i, table.pattern.getStop(i), serviceDay));
        }
        return out;
    }

    public static Comparator<TripTimeShort> compareByDeparture() {
        return Comparator.comparing(t -> t.getServiceDay() + t.getRealtimeDeparture());
    }
}