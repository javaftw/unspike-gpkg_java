package hk.unspike;

import java.io.File;

public class Unspike {

    private static File inputFile;
    private static File outputFile;
    private static double minAngle;
    private static boolean verbose;

    public Unspike() {

    }

    public static void main(String[] args) {
        // Default values
        inputFile = null;
        outputFile = null;
        minAngle = 0.0;
        verbose = false;

        // Parse arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-i":
                    if (i + 1 < args.length) {
                        try {
                            inputFile = new File(args[++i]);
                            if (!inputFile.exists()) throw new Exception("Input file does not exist");
                        } catch (Exception ex) {
                            System.err.println("The specified file cannot be opened.");
                            ex.printStackTrace();
                        }
                    } else {
                        throw new IllegalArgumentException("Input file not specified.");
                    }
                    break;
                case "-o":
                    if (i + 1 < args.length) {
                        try {
                            outputFile = new File(args[++i]);
                        } catch (Exception ex) {
                            System.err.println("The specified output file cannot be opened.");
                            ex.printStackTrace();
                        }
                    } else {
                        throw new IllegalArgumentException("Output file not specified.");
                    }
                    break;
                case "-a":
                    if (i + 1 < args.length) {
                        minAngle = Math.abs(Double.parseDouble(args[++i]));
                    } else {
                        throw new IllegalArgumentException("Minimum angle must be specified.");
                    }
                    break;
                case "-v":
                    verbose = true;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument: " + args[i]);
            }
        }

        if (inputFile == null) {
            throw new IllegalArgumentException("Input file is required.");
        }

        if (outputFile == null) {
            outputFile = new File(inputFile.getName().replace(".gpkg", "_unspiked.gpkg"));
        }
    }
}
