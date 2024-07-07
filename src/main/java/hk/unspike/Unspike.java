package hk.unspike;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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

        try {
            interrogateVertices();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void interrogateVertices() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("dbtype", "geopkg");
        map.put("database", inputFile.getAbsolutePath());

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];
        SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName);
        SimpleFeatureCollection collection = featureSource.getFeatures();

        try (SimpleFeatureIterator iterator = collection.features()) {
            while (iterator.hasNext()) {
                SimpleFeature feature = iterator.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();

                if (geometry instanceof Polygon) {
                    Polygon polygon = (Polygon) geometry;
                    System.out.println("Vertices of Polygon:");
                    for (int i = 0; i < polygon.getCoordinates().length; i++) {
                        System.out.println(polygon.getCoordinates()[i]);
                    }
                }
            }
        }

        dataStore.dispose();
    }
}
