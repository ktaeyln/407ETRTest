package com.katelynslater;

import com.katelynslater.data.Interchanges;
import com.katelynslater.data.Location;
import com.katelynslater.data.Route;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

public class Main {
    // List of Possible Exit Statuses
    enum ExitStatus {
        Okay,

        FailedToReadInterchanges,
        FailedToParseInterchanges,
        InterchangesFormatIssue,

        UnexpectedException
    }

    public static void main(String[] args) {
        // Handle ANY Exception Cleanly
        try {
            // Open interchanges.json from Resource
            InputStream is = Main.class.getResourceAsStream("/com/katelynslater/interchanges.json");

            // Check if Resource was Found
            if (is == null) {
                // Output Error if Missing
                System.err.println("interchanges.json could not be found in resources, you may provide it in the working directory instead and try again");
                // Exit with Correct Error Code
                System.exit(ExitStatus.FailedToReadInterchanges.ordinal());
            }

            // Create an InputStreamReader to read chars rather than bytes
            InputStreamReader isr = null;
            try {
                // Get the file.encoding property
                String charset = System.getProperty("file.encoding");
                // Default to UTF-8
                if(charset == null) charset = "UTF-8";
                // Use the character set to create the InputStreamReader
                isr = new InputStreamReader(is, charset);
            } catch (UnsupportedEncodingException ueerr) {
                // Output a friendly error
                System.err.println("UTF-8 Encoding not Supported?");
                // Output the actual exception and stack
                ueerr.printStackTrace(System.err);
                // Exit with an appropriate status code
                System.exit(ExitStatus.FailedToReadInterchanges.ordinal());
            }

            // Using a StringBuilder to efficiently store the character data
            StringBuilder interchangesJson = new StringBuilder();
            // Using a char[] buffer of 1024
            char[] buffer = new char[1024];
            // This variable will store the amount of chars read each loop iteration
            int read;

            System.out.println("Reading interchanges.json");
            try {
                // Loop until the entirety of interchanges.json is read
                while ((read = isr.read(buffer)) > 0) {
                    // Store the read char[] data in the StringBuilder
                    interchangesJson.append(buffer, 0, read);
                }
            } catch (IOException ioerr) {
                // If an exception occurs, print the error
                System.err.println("Error occurred while reading interchanges.json from resource:");
                ioerr.printStackTrace(System.err);
                System.exit(ExitStatus.FailedToReadInterchanges.ordinal());
            }

            // Build the String, and create a JSONObject with it
            JSONObject interchangesJsonData = null;
            try {
                System.out.println("Parsing interchanges.json");
                interchangesJsonData = new JSONObject(interchangesJson.toString());
            } catch(JSONException jerr) {
                System.err.println("The interchanges.json file was read, but could not be parsed as JSON:");
                jerr.printStackTrace(System.err);
                System.exit(ExitStatus.FailedToParseInterchanges.ordinal());
            }

            JSONObject locations = null;
            Interchanges interchanges = new Interchanges();
            try {
                System.out.println("Processing interchange data...");
                locations = interchangesJsonData.getJSONObject("locations");

                int routeCount = 0;
                int interchangeCount = 0;
                Iterator<String> kit = locations.keys();
                while(kit.hasNext()) {
                    String key = kit.next();
                    try {
                        JSONObject locationData = locations.getJSONObject(key);

                        Location location = new Location();
                        // Add the Location to the Interchanges class to check for duplicates, before doing any other parsing
                        interchanges.addLocation(Integer.parseInt(key), location);

                        location.name = locationData.getString("name");
                        location.lat = locationData.getDouble("lat");
                        location.lng = locationData.getDouble("lng");

                        JSONArray routes = locationData.getJSONArray("routes");
                        int routeTotal = routes.length();

                        ArrayList<Route> locationRoutes = location.routes;
                        for(int i=0; i<routeTotal; i++) {
                            JSONObject routeData = routes.getJSONObject(i);
                            Route route = new Route();
                            route.toId = routeData.getInt("toId");
                            route.distance = routeData.getDouble("distance");
                            locationRoutes.add(route);
                        }

                        routeCount += routeTotal;
                        interchangeCount ++;
                    } catch(NumberFormatException nferr) {
                        System.err.println("All location keys must be numeric strings:");
                        nferr.printStackTrace(System.err);
                        System.exit(ExitStatus.InterchangesFormatIssue.ordinal());
                    } catch(Interchanges.LocationAlreadyExists laerr) {
                        System.err.println("You appear to have multiple locations with the same ID in your JSON:");
                        laerr.printStackTrace(System.err);
                        System.exit(ExitStatus.InterchangesFormatIssue.ordinal());
                    }
                }

                interchanges.generateNameLookupCache();
                System.out.println("Parsed " + interchangeCount + " interchanges with " + routeCount + " routes total");

                // TODO: Add argument parsing

                Scanner inScanner = new Scanner(System.in);
                while(true) {
                    int startId;
                    int endId;

                    System.out.print("Enter starting location by name or ID: ");
                    System.out.flush();
                    String userInput = inScanner.nextLine().trim();
                    if(userInput.equals("exit")) System.out.println(0);

                    try {
                        // Attempt to parse the input as an integer, and fetch a location by Id
                        startId = Integer.parseInt(userInput);
                        // Just check if the Location exists or not
                        if(interchanges.getLocationById(startId) == null) {
                            System.err.println("No location with ID " + startId + " could be found!");
                            System.err.flush();
                            continue; // Try again
                        }
                    } catch(NumberFormatException nferr) {
                        // If this isn't an integer, it must be a name
                        try {
                            startId = interchanges.lookupLocationIdForName(userInput.toLowerCase());
                        } catch(NullPointerException nperr) {
                            System.err.println("No location with name \"" + userInput + "\" could be found!");
                            System.err.flush();
                            continue; // Try again
                        }
                    }

                    System.out.print("Now enter end location by name or ID: ");
                    System.out.flush();
                    userInput = inScanner.nextLine().trim();
                    if(userInput.equals("exit")) System.out.println(0);

                    try {
                        // Attempt to parse the input as an integer, and fetch a location by Id
                        endId = Integer.parseInt(userInput);
                        // Just check if the Location exists or not
                        if(interchanges.getLocationById(endId) == null) {
                            System.err.println("No location with ID " + startId + " could be found!");
                            System.err.flush();
                            continue; // Try again
                        }
                    } catch(NumberFormatException nferr) {
                        // If this isn't an integer, it must be a name
                        try {
                            endId = interchanges.lookupLocationIdForName(userInput.toLowerCase());
                        } catch(NullPointerException nperr) {
                            System.err.println("No location with name \"" + userInput + "\" could be found!");
                            System.err.flush();
                            continue; // Try again
                        }
                    }

                    System.out.println("Calculating distance between \"" + interchanges.getLocationById(startId).name + "\" and \"" + interchanges.getLocationById(endId).name + "\"...");
                    double distance = interchanges.calculateDistance(startId, endId).get();
                    System.out.println("Calculated a distance of " + (Math.round(distance*1000)/1000.0) + "km costing $" + (Math.ceil(distance*25) / 100.0));
                }
            } catch(JSONException jerr) {
                System.err.println("The interchanges.json file was parsed, but the locations key appears to be missing:");
                jerr.printStackTrace(System.err);
                System.exit(ExitStatus.InterchangesFormatIssue.ordinal());
            }
        } catch(Throwable terr) {
            // If an unexpected exception occurs of any type, print it and exit with the correct status code
            System.err.println("An unexpected exception was thrown:");
            terr.printStackTrace(System.err);
            System.exit(ExitStatus.UnexpectedException.ordinal());
        }
    }
}
