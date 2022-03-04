package com.katelynslater.data;

import java.util.*;
import java.util.concurrent.*;

// The Interchanges main class
public class Interchanges {
    // Exception used when a Location ID already exists
    public class LocationAlreadyExists extends Exception {
        public LocationAlreadyExists(int id) {
            super("A location already exists for ID " + id);
        }
    }

    // The list of locations by ID
    private ArrayList<Location> locations = new ArrayList<Location>();
    // Map of IDs for each Location ID
    private HashMap<String, Integer> locationIdByName = new HashMap<String, Integer>();
    public void addLocation(Location location) throws LocationAlreadyExists{
        int id = location.id;
        try {
            Location locationForId = locations.get(id);
            if(locationForId != null) throw new LocationAlreadyExists(id);
            locations.set(id, location);
        } catch(IndexOutOfBoundsException err) {
            // If the index is out of bounds, add this location instead
            // Keys may be out of order, so this ensures we can continue to use an ArrayList
            // while also providing support for out of order entries
            // It is much more efficient to use an ArrayList over a HashMap

            // Determine how many nulls to add
            int left = (id - locations.size());
            while(left-- > 0) locations.add(null);

            // Finally, add the location
            locations.add(location);
        }

        // Add a reference to the ID by the location's name
        locationIdByName.put(location.name.toLowerCase(), id);
    }

    // Calculate the Distance between a Start and End ID
    public double calculateDistance(int start, int end) {
        // Always scan from lowest to highest, if end is lower than start, reverse them
        if(start > end) {
            int temp = end;
            end = start;
            start = temp;
        }

        // Store the currently calculated distance
        double distance = 0;

        // Get the starting location
        Location current = locations.get(start);
        do {
            // Get the next ID (higher than current)
            Route next = current.nextRoute;
            // Add to the distance
            distance += next.distance;
            // Get the ID
            int id = next.toId;
            // Compare the ID against the End ID, break if it matches
            if(id == end) break;
            // Change the current Location to the Route's Destination
            current = next.destination;
        } while(true);

        // Return the Calculated Distance
        return distance;
    }

    // Connect the Destination reference to each Route
    public void connectRoutes() {
        // Iterate through Locations
        Iterator<Location> it = locations.iterator();
        while(it.hasNext()) {
            Location location = it.next();

            // Skip any IDs that have no location
            if(location == null) continue;

            // Get the next ID (higher than current)
            Route next = location.nextRoute;
            if(next != null) {
                try {
                    // Set the Destination to it's instance
                    next.destination = locations.get(next.toId);
                } catch(IndexOutOfBoundsException t) {
                    // If the ID is invalid, then clear the route
                    location.nextRoute = null;
                }
            }

            // Get the previous ID (lower than current)
            Route prev = location.prevRoute;
            if(prev != null) {
                try {
                    // Set the Destination to it's instance
                    prev.destination = locations.get(prev.toId);
                } catch(IndexOutOfBoundsException t) {
                    // If the ID is invalid, then clear the route
                    location.prevRoute = null;
                }
            }
        }
    }

    // Return a Location instance for a given ID
    public Location getLocationById(int id) {
        return locations.get(id);
    }

    // Returns an Iterator for Locations
    public Iterator<Location> locationIterator() {
        return locations.iterator();
    }

    // Return a Location ID for a given Name
    public int lookupLocationIdForName(String name) {
        return locationIdByName.get(name.toLowerCase());
    }
}
