import com.katelynslater.CLI;
import com.katelynslater.data.Interchanges;
import com.katelynslater.data.Location;
import com.katelynslater.data.Route;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

class Tests {
    CLI cli = new CLI();
    Interchanges interchanges = cli.interchanges;
    public Tests() {
        // For testing, always load from the resource file
        cli.loadFromResource();
    }

    @Test
    void checkObviousDistance() {
        // Get location 1 and the route to the next location
        Location location = interchanges.getLocationById(1);
        Route route = location.nextRoute;

        // Calculate the distance
        double calculated = interchanges.calculateDistance(1, route.toId);

        // Ensure everything is correct
        Assertions.assertEquals(route.distance, calculated);
    }

    @Test
    void checkMoreComplexDistance() {
        // Get location 1 and the route to the next location
        Location location = interchanges.getLocationById(1);
        Route route = location.nextRoute;

        Location location2 = route.destination;

        int destId = 0;
        double distance = 0;

        // Get the route to the next next location
        Route route2 = location2.nextRoute;

        destId = route2.toId;
        distance = route.distance + route2.distance;

        // Calculate
        double calculated = interchanges.calculateDistance(1, destId);

        // Verify everything is correct
        Assertions.assertEquals(distance, calculated);
    }

    @Test
    void checkEntirety() {
        Location startLocation = null;
        Location lastLocation = null;
        double totalDistance = 0;

        // Iterate through locations
        Iterator<Location> it = interchanges.locationIterator();
        while(it.hasNext()) {
            Location location = it.next();
            if(location == null) continue;

            // Increment the total distance for each route
            Route nextRoute = location.nextRoute;
            if(nextRoute != null) {
                totalDistance += location.nextRoute.distance;
            }

            // Collect the Start and Last locations
            if(startLocation == null) startLocation = location;
            else lastLocation = location;
        }

        // Calculate the distance from start to end
        double calculated = interchanges.calculateDistance(startLocation.id, lastLocation.id);

        // Compare
        Assertions.assertEquals(totalDistance, calculated);
    }

    @Test
    void checkNameLookup() {
        // Get Location ID 1
        Location location = interchanges.getLocationById(1);
        // Get the ID for the name of the Location at ID 1
        int id = interchanges.lookupLocationIdForName(location.name);

        // Compare
        Assertions.assertEquals(location.id, id);
    }

}