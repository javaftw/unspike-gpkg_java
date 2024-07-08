package hk.unspike;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;

public class Unspike {

    private static File inputFile;
    private static File outputFile;
    private static double minAngle;
    private static boolean verbose;

    public Unspike() {
    }

    public static void main(String[] args) {
        // Initialize CRS system
        System.setProperty("org.geotools.referencing.forceXY", "true");
        System.setProperty("org.geotools.referencing.factory.epsg.FactoryUsingWKT", "true");
        CRS.reset("all");

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

        if (minAngle == 0) {
            throw new IllegalArgumentException("The minimum angle is invalid or missing.");
        }

        if (inputFile == null) {
            throw new IllegalArgumentException("Input file is required.");
        }

        if (outputFile == null) {
            outputFile = generateUniqueOutputFilename(inputFile);
        }

        try {
            processGeoPackage();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double calculateAngle(Coordinate p0, Coordinate p1, Coordinate p2) {
        // Calculate vectors from p1 to p0 and p1 to p2
        double[] v1 = new double[]{p0.x - p1.x, p0.y - p1.y};
        double[] v2 = new double[]{p2.x - p1.x, p2.y - p1.y};

        // Calculate dot product and norms
        double dotProduct = v1[0] * v2[0] + v1[1] * v2[1];
        double normV1 = Math.sqrt(v1[0] * v1[0] + v1[1] * v1[1]);
        double normV2 = Math.sqrt(v2[0] * v2[0] + v2[1] * v2[1]);

        // Handle cases where vectors are extremely small to avoid division by zero
        if (normV1 < 1e-10 || normV2 < 1e-10) {
            return 180.0;
        }

        // Calculate and return the angle using the dot product formula
        double cosineAngle = Math.max(-1.0, Math.min(1.0, dotProduct / (normV1 * normV2)));
        return Math.toDegrees(Math.acos(cosineAngle));
    }

    private static File generateUniqueOutputFilename(File inputFile) {
        String inputName = inputFile.getName();
        String baseName = inputName.substring(0, inputName.lastIndexOf('.'));
        String extension = inputName.substring(inputName.lastIndexOf('.'));

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH-mm_dd-MM-yyyy");
        String timestamp = now.format(formatter);

        return new File(inputFile.getParent(), baseName + "_unspiked_" + timestamp + extension);
    }

    private static void processGeoPackage() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("dbtype", "geopkg");
        map.put("database", inputFile.getAbsolutePath());

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
        SimpleFeatureCollection collection = featureSource.getFeatures();

        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();

        try (SimpleFeatureIterator iterator = collection.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();

                if (geometry instanceof Polygon) {
                    Polygon polygon = (Polygon) geometry;
                    Polygon newPolygon = removeSpikes(polygon);
                    SimpleFeature newFeature = SimpleFeatureBuilder.retype(feature, feature.getFeatureType());
                    newFeature.setDefaultGeometry(newPolygon);
                    newCollection.add(newFeature);
                }
            }
        }

        if (verbose) {
            System.out.println("Original Vertices:");
            listVertices(collection);
        }

        writeGeoPackage(newCollection, typeName, dataStore.getSchema(typeName));

        if (verbose) {
            System.out.println("Modified Vertices:");
            listVertices(newCollection);
        }

        dataStore.dispose();
    }

    private static void listVertices(SimpleFeatureCollection collection) {
        try (SimpleFeatureIterator iterator = collection.features()) {
            int featureCount = 0;
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                featureCount++;
                System.out.println("Feature " + featureCount + ":");
                for (Coordinate coord : geometry.getCoordinates()) {
                    System.out.printf("  (%.6f, %.6f)%n", coord.x, coord.y);
                }
            }
        }
    }

    private static Polygon removeSpikes(Polygon polygon) {
        Coordinate[] coordinates = polygon.getCoordinates();
        ArrayList<Coordinate> newCoordinates = new ArrayList<>();
        boolean spikeRemoved = false;

        for (int i = 0; i < coordinates.length - 2; i++) {
            Coordinate p0 = coordinates[i];
            Coordinate p1 = coordinates[(i + 1) % coordinates.length];
            Coordinate p2 = coordinates[(i + 2) % coordinates.length];

            double angle = calculateAngle(p0, p1, p2);
            if (!Double.isNaN(angle) && angle >= minAngle) {
                newCoordinates.add(p1);
            } else {
                spikeRemoved = true;
                if (verbose) {
                    System.out.printf("Removed spike at: (%.6f, %.6f) with angle: %.2f%n", p1.x, p1.y, angle);
                }
            }
        }

        // Ensure the polygon is closed by adding the first point to the end if not already present
        if (!newCoordinates.isEmpty() && !newCoordinates.get(0).equals(newCoordinates.get(newCoordinates.size() - 1))) {
            newCoordinates.add(newCoordinates.get(0));
        }

        if (newCoordinates.isEmpty() || !spikeRemoved) {
            if (verbose) {
                System.out.println("No spikes removed.");
            }
            return polygon; // Return the original polygon if no coordinates are left or no spikes were removed
        }

        GeometryFactory geometryFactory = new GeometryFactory();
        return geometryFactory.createPolygon(newCoordinates.toArray(new Coordinate[0]));
    }

    private static void writeGeoPackage(SimpleFeatureCollection collection, String typeName, SimpleFeatureType schema) throws Exception {
        // Create a new GeoPackage
        GeoPackage geopkg = new GeoPackage(outputFile);
        geopkg.init();

        // Get the CRS from the input schema
        CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
        if (crs == null) {
            // Fallback to EPSG:4326 if no CRS is specified
            crs = CRS.decode("EPSG:4326", true);
        }

        // Create a FeatureEntry
        FeatureEntry entry = new FeatureEntry();
        entry.setDataType(FeatureEntry.DataType.Feature);
        entry.setTableName(typeName);
        entry.setSrid(CRS.lookupEpsgCode(crs, true));

        // Write the features
        geopkg.add(entry, collection);

        if (verbose) {
            System.out.println("Features written to GeoPackage file: " + outputFile.getAbsolutePath());
            System.out.println("Table name: " + typeName + ", SRID: " + entry.getSrid());
        }

        // Close the GeoPackage
        geopkg.close();
    }
}
