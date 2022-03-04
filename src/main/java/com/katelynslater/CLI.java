package com.katelynslater;

import com.katelynslater.data.Interchanges;
import com.katelynslater.data.Location;
import com.katelynslater.data.Route;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Scanner;

import java.io.*;

public class CLI {
    // CLI friendly exceptions, along with the exit status code to use
    public class CLIException extends RuntimeException {
        ExitStatus status;
        public CLIException(String message, ExitStatus status, Throwable cause) {
            super(message, cause);
            this.status = status;
        }
        public CLIException(String message, ExitStatus status) {
            super(message);
            this.status = status;
        }
    }
    public class UnknownLocationException extends CLIException {
        public UnknownLocationException(String message) {
            super(message, ExitStatus.InvalidLocation);
        }
    }

    // List of Possible Exit Statuses
    enum ExitStatus {
        Okay,

        FailedToReadInterchanges,
        FailedToParseInterchanges,
        InterchangesFormatIssue,

        InvalidLocation,
        UnexpectedException
    }

    // Whether or not to actually print anything when log() is called
    public boolean debug = false;

    // The interchanges helper
    public Interchanges interchanges = new Interchanges();

    // Load the interchanges.json from the file specified
    public void loadFromFile(String filepath) {
        log("Reading \"" + filepath + "\"");
        FileInputStream fileIs;

        try {
            fileIs = new FileInputStream(filepath);
        } catch(FileNotFoundException fnferr) {
            throw new CLIException("The specified file at \"" + filepath + "\" could not be found", ExitStatus.FailedToReadInterchanges, fnferr);
        }

        loadFromInputStream(fileIs);
    }
    // Load the interchanges.json from the JAR package
    public void loadFromResource() {
        log("Reading interchanges.json from Resources");

        // Open interchanges.json from Resource
        InputStream is = CLI.class.getResourceAsStream("/com/katelynslater/interchanges.json");

        // Check if Resource was Found
        if (is == null) {
            throw new CLIException("interchanges.json could not be found in resources, you may provide it in the working directory instead and try again", ExitStatus.FailedToReadInterchanges);
        }

        loadFromInputStream(is);
    }
    public void loadFromInputStream(InputStream is) {
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
            throw new CLIException("UTF-8 Encoding not Supported?", ExitStatus.FailedToReadInterchanges, ueerr);
        }

        // Using a StringBuilder to efficiently store the character data
        StringBuilder interchangesJson = new StringBuilder();
        // Using a char[] buffer of 1024
        char[] buffer = new char[1024];
        // This variable will store the amount of chars read each loop iteration
        int read;

        try {
            // Loop until the entirety of interchanges.json is read
            while ((read = isr.read(buffer)) > 0) {
                // Store the read char[] data in the StringBuilder
                interchangesJson.append(buffer, 0, read);
            }
        } catch (IOException ioerr) {
            throw new CLIException("Error occurred while reading interchanges data", ExitStatus.FailedToReadInterchanges, ioerr);
        }

        // Build the String, and create a JSONObject with it
        JSONObject interchangesJsonData = null;
        try {
            log("Parsing interchanges data");
            interchangesJsonData = new JSONObject(interchangesJson.toString());
        } catch(JSONException jerr) {
            throw new CLIException("The interchanges data provided could not be parsed as JSON", ExitStatus.FailedToParseInterchanges, jerr);
        }

        JSONObject locations = null;
        try {
            log("Processing interchange data...");
            locations = interchangesJsonData.getJSONObject("locations");

            int routeCount = 0;
            int interchangeCount = 0;

            // Iterate through the keys
            Iterator kit = locations.keys();
            while(kit.hasNext()) {
                // Every key should be a String
                String key = (String)kit.next();
                try {
                    // Parse the location id
                    int id = Integer.parseInt(key);

                    // Get the Location JSONObject
                    JSONObject locationData = locations.getJSONObject(key);

                    // Get the location's name
                    String name = locationData.getString("name");

                    // Create a Location instance
                    Location location = new Location(id, name);

                    // Add the Location to the Interchanges class to check for duplicates, so we can error as soon as possible
                    interchanges.addLocation(location);

                    // Start parsing the Routes
                    JSONArray routes = locationData.getJSONArray("routes");
                    int routeTotal = routes.length();

                    if(routeTotal > 2) throw new CLIException("Each location can have only 2 routes maximum", ExitStatus.InterchangesFormatIssue);

                    // Fetch the routes ArrayList into a temporary variable
                    for(int i=0; i<routeTotal; i++) {
                        // Get the route JSONObject
                        JSONObject routeData = routes.getJSONObject(i);

                        // Create the Route object
                        Route route = new Route();

                        // Get the toId
                        int toId = routeData.getInt("toId");

                        // Assign Route properties
                        route.toId = toId;
                        route.distance = routeData.getDouble("distance");

                        // Add the Route
                        if(toId > id)
                            location.nextRoute = route;
                        else
                            location.prevRoute = route;
                    }

                    // Increment the counters
                    routeCount += routeTotal;
                    interchangeCount ++;
                } catch(NumberFormatException nferr) {
                    // If the location key cannot be parsed as an integer, report the error
                    throw new CLIException("All location keys must be numeric strings", ExitStatus.InterchangesFormatIssue, nferr);
                } catch(Interchanges.LocationAlreadyExists laerr) {
                    // If multiple locations are found with the same ID, report the error
                    throw new CLIException("You appear to have multiple locations with the same ID in your JSON", ExitStatus.InterchangesFormatIssue, laerr);
                }
            }

            // Report the parsing results
            log("Parsed " + interchangeCount + " interchanges with " + routeCount + " routes total");

            // Remove any routes that reference invalid location IDs, they'll only cost CPU cycles during lookups, so it's better to clean them up rather than ignore them...
            log("Cleaning up routing data...");
            interchanges.connectRoutes();
        } catch(JSONException jerr) {
            throw new CLIException("Interchange data was parsed, but the locations key appears to be missing", ExitStatus.InterchangesFormatIssue, jerr);
        }
    }

    // Debug Log Function
    public void log(String message) {
        if(debug) {
            System.out.println(message);
        }
    }

    // Print program usages
    public static void printUsages() {
        System.out.println("407ETR Interchanges Example by Katelyn Slater");
        System.out.println("Usage:\tjava -jar 407ETRTest.jar [start id or name] [end id or name] [interchanges.json filepath]");
        System.out.println("\t\tjava -jar 407ETRTest.jar [interchanges.json filepath]");
        System.out.println();
        System.out.println("https://github.com/ktaeyln/407ETRTest");
    }

    // The main function
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        // Handle ANY Exception Cleanly
        try {
            String dataFilePath = null;
            String lookupStart = null;
            String lookupEnd = null;

            CLI cli = new CLI();

            switch (args.length) {
                case 0: // 0 arguments means load interchanges.json from resources and request user input
                    cli.debug = true;
                    break;
                case 1: // 1 argument means interchanges.json filepath provided
                    dataFilePath = args[0];
                    cli.debug = true;

                    // Check if the user passed --help or -h and show usages
                    if (dataFilePath.equals("--help") || dataFilePath.equals("-h")) {
                        printUsages();
                        System.exit(0);
                    }
                    break;
                case 2: // 2 arguments means a lookup was provided
                    lookupStart = args[0];
                    lookupEnd = args[1];
                    break;
                case 3: // 3 arguments means a lookup was provided, and an interchanges.json filepath was provided
                    lookupStart = args[0];
                    lookupEnd = args[1];
                    dataFilePath = args[2];
                    break;
                default:
                    printUsages();
                    System.exit(0);
            }

            // Check if a custom interchanges json file was specified, loading from resources if not
            if (dataFilePath == null)
                cli.loadFromResource();
            else
                cli.loadFromFile(dataFilePath);

            // Check if lookup arguments were passed
            if(lookupStart == null) {
                // Report the time it took to startup
                System.out.println("Ready in " + (System.currentTimeMillis() - startTime) + "ms!");
                cli.interactiveLoop();
            } else
                cli.printRouteDistance(lookupStart, lookupEnd);

            // Exit gracefully
            System.exit(0);
        } catch(CLIException clierr) {
            // Print the CLI Exception
            clierr.printStackTrace();
            // Exit with the CLI status code provided
            System.exit(clierr.status.ordinal());
        } catch(Throwable terr) {
            // If an unexpected exception occurs of any type, print it and exit with the correct status code
            System.err.println("An unexpected exception was thrown:");

            // Print the exception
            terr.printStackTrace();

            // Exit with the correct UnexpectedException status code
            System.exit(ExitStatus.UnexpectedException.ordinal());
        }
    }

    // An interactive CLI loop
    public void interactiveLoop() {
        // Create the Input Scanner
        Scanner inScanner = new Scanner(System.in);

        // Initialize variables used in the loop
        String userInput;
        int startId = 0;
        int endId = 0;
        while(true) {
            // Read the Starting ID or Name
            System.out.print("Enter starting location by name or ID: ");
            System.out.flush();
            userInput = inScanner.nextLine().trim();
            if (userInput.equals("")) System.exit(0);

            try {
                // Lookup the ID, and validate if it exists or not
                startId = findLocationIdForString(userInput);
            } catch(UnknownLocationException ulerr) {
                // Let the user know if their input was invalid
                System.err.println(ulerr.getMessage());
                System.err.flush();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {}
                continue;
            }

            // Read the End ID or Name
            System.out.print("Enter end location by name or ID: ");
            System.out.flush();
            userInput = inScanner.nextLine().trim();
            if (userInput.equals("")) System.exit(0);

            try {
                // Lookup the ID, and validate if it exists or not
                endId = findLocationIdForString(userInput);
            } catch(UnknownLocationException ulerr) {
                // Let the user know if their input was invalid
                System.err.println(ulerr.getMessage());
                System.err.flush();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {}
                continue;
            }

            // Print the Route's Distance and Cost using the "Friendly" Interactive style
            printRouteDistance(startId, endId, true);
        }
    }

    // Lookup the ID for a location, and validate that it exists
    public int findLocationIdForString(String input) {
        int foundId = 0;

        try {
            // Attempt to parse the input as an integer, and fetch a location by Id
            foundId = Integer.parseInt(input);
            // Just check if the Location exists or not, 0 exists but is always null
            if (interchanges.getLocationById(foundId) == null) {
                throw new UnknownLocationException("No location with ID " + input + " could be found!");
            }
        } catch(IndexOutOfBoundsException ioberr) {
            throw new UnknownLocationException("No location with ID " + input + " could be found!");
        } catch(NumberFormatException nferr) {
            // If this isn't an integer, it must be a name
            try {
                foundId = interchanges.lookupLocationIdForName(input);
            } catch(NullPointerException nperr) {
                throw new UnknownLocationException("No location with name \"" + input + "\" could be found!");
            }
        }

        return foundId;
    }

    // Print the distance and cost using the requested style, from 2 string inputs
    public void printRouteDistance(String lookupStart, String lookupEnd) {
        int startId = findLocationIdForString(lookupStart);
        int endId = findLocationIdForString(lookupEnd);

        printRouteDistance(startId, endId, false);
    }
    // Print the total distance and cost, using either the interactive or requested style
    public void printRouteDistance(int startId, int endId, boolean useFriendlyStyle) {
        long startTime = System.currentTimeMillis();

        if(useFriendlyStyle) {
            System.out.println("Calculating distance between \"" + interchanges.getLocationById(startId).name + "\" and \"" + interchanges.getLocationById(endId).name + "\"...");
        }

        double distance = 0;
        try {
            distance = interchanges.calculateDistance(startId, endId);
        } catch (Throwable terr) {
            throw new CLIException("An unexpected exception was thrown during distance calculation", ExitStatus.UnexpectedException, terr);
        }

        if(useFriendlyStyle) {
            System.out.println("Calculated a distance of " + (Math.round(distance*1000)/1000.0) + "km costing $" + (Math.ceil(distance*25) / 100.0) + " in " + (System.currentTimeMillis()-startTime) + "ms");
        } else {
            System.out.println("Distance: " + (Math.round(distance*1000)/1000.0) + "km");
            System.out.println("Cost: $" + (Math.ceil(distance*25) / 100.0));
        }
    }
}
