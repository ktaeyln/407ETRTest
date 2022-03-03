package com.katelynslater.data;

import java.util.*;
import java.util.concurrent.*;

public class Interchanges {
    public class LocationAlreadyExists extends Exception {
        public LocationAlreadyExists(int id) {
            super("A location already exists for ID " + id);
        }
    }

    private static int getThreadCount() {
        String singlethreaded = System.getProperty("singlethreaded");
        if(singlethreaded != null && singlethreaded.equals("true")) return 1;
        return Runtime.getRuntime().availableProcessors();
    }
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(0, getThreadCount(), 15, TimeUnit.SECONDS, new ArrayBlockingQueue(500000));

    private ArrayList<Location> locations = new ArrayList();
    private HashMap<String, Integer> locationIdByName = new HashMap();
    public void addLocation(int id, Location location) throws LocationAlreadyExists{
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
    }

    protected class CalculationLookup implements Future<Double> {
        private ArrayList<Thread> waitingThreads = new ArrayList();
        private boolean cancelled = false;
        private boolean done = false;
        private double result;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            synchronized(this) {
                if(done) return false;
                cancelled = true;
                synchronized (waitingThreads) {
                    Iterator<Thread> it = waitingThreads.iterator();
                    while (it.hasNext()) {
                        it.next().interrupt();
                    }
                }
            }
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        @Override
        public Double get() throws InterruptedException {
            while(!done)
                sleep(Long.MAX_VALUE);
            return result;
        }

        @Override
        public Double get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
            if(!done) sleep(unit.convert(timeout, TimeUnit.MILLISECONDS));
            if(done) return result;
            throw new TimeoutException();
        }

        public void setResult(double result) {
            synchronized(this) {
                if(done) return;

                this.result = result;
                done = true;

                synchronized (waitingThreads) {
                    Iterator<Thread> it = waitingThreads.iterator();
                    while (it.hasNext()) {
                        it.next().interrupt();
                    }
                }
            }
        }

        private class RouteCheck implements Runnable {
            int current;
            int target;
            double distance;
            boolean[] visited;
            public RouteCheck(double distance, int current, int target, boolean[] visited) {
                this.distance = distance;
                this.current = current;
                this.target = target;
                this.visited = visited;
            }

            @Override
            public void run() {
                if(done) return; // Nothing left to do

                synchronized(visited) {
                    visited[current] = true;
                }

                Location currentLocation = locations.get(current);
                Iterator<Route> it = currentLocation.routes.iterator();
                while(it.hasNext()) {
                    Route route = it.next();
                    double routeDistance = distance + route.distance;
                    int toId = route.toId;
                    synchronized(visited) {
                       if(visited[toId]) continue; // Already visited this destination
                    }
                    if(toId == target) {
                        setResult(routeDistance);
                        break; // We're done!!
                    } else threadPoolExecutor.execute(new RouteCheck(routeDistance, toId, target, visited));
                }
            }
        }
        public void calculate(int start, int target) {
            boolean[] visited = new boolean[locations.size()];
            Arrays.fill(visited, false);

            threadPoolExecutor.execute(new RouteCheck(0, start, target, visited));
        }

        private void sleep(long millis) throws InterruptedException {
            Thread currentThread = Thread.currentThread();
            synchronized(this) {
                if (cancelled) throw new CancellationException();
                synchronized (waitingThreads) {
                    waitingThreads.add(currentThread);
                }
            }
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                if(cancelled) throw new CancellationException();
                if(!done) throw e;
            } finally {
                synchronized(waitingThreads) {
                    waitingThreads.remove(currentThread);
                }
            }
        }
    }
    public Future<Double> calculateDistance(int start, int end) {
        CalculationLookup lookup = new CalculationLookup();
        lookup.calculate(start, end);
        return lookup;
    }

    public Location getLocationById(int id) {
        return locations.get(id);
    }
    public int lookupLocationIdForName(String name) {
        return locationIdByName.get(name);
    }

    public void generateNameLookupCache() {
        locationIdByName.clear();

        ListIterator<Location> it = locations.listIterator();
        while(it.hasNext()) {
            int id = it.nextIndex();
            Location location = it.next();
            if(location == null) continue;
            locationIdByName.put(location.name.toLowerCase(), id);
        }
    }
}
