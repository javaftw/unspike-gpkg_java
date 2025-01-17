# Unspike gpkg (Java stand-alone version)

Unspike Java is a command-line tool designed to process GeoPackage files by identifying and removing vertices that form sharp angles in polygon geometries. This tool helps in cleaning up geometries by eliminating "spikes" based on a user-defined minimum angle.

## Features

- **Process GeoPackage Files**: Read and write GeoPackage files with ease.
- **Spike Removal**: Remove vertices forming angles smaller than a specified minimum angle.
- **Verbose Mode**: Optionally display detailed processing information.
- **Custom Output Filenames**: Generate unique output filenames based on the input filename and timestamp.

## Requirements

- Java 8 or higher
- Maven
- GeoTools library

## Installation

1. **Clone the Repository**

   ```
   git clone https://github.com/javaftw/unspike-gpkg_java
   cd unspike_java
   ```

## Build and Run

Use Maven to clean and package the project, which will create a shaded JAR file including all dependencies.

```
mvn clean package
java -jar target/unspike_java-0.1-alpha-shaded.jar -i example.gpkg -a 1.5 -v
```

## Usage

Run the shaded JAR file from the command line with the appropriate options.

**Command-Line Options**
- `-i <input_file>`: Specifies the input GeoPackage file (required).
- `-o <output_file>`: Specifies the output GeoPackage file (optional, a unique filename will be generated if not provided).
- `-a <min_angle>`: Specifies the minimum angle in degrees for spike removal (required).
- `-v`: Enables verbose mode (optional).

## Dependencies

The project uses the following dependencies (managed with Maven):

- org.geotools:gt-geopkg
- org.geotools:gt-main
- org.geotools:gt-cql
- org.geotools:gt-referencing
- org.geotools:gt-epsg-hsql
- org.geotools:gt-epsg-extension

## Flow Diagram

```mermaid
graph TD
    A[Start] --> B[Parse Command-Line Arguments]
    B --> C{Are Arguments Valid?}
    C -- No --> D[Show Error Message]
    C -- Yes --> E[Initialize CRS System]
    E --> F[Open GeoPackage File]
    F --> G[Read Features]
    G --> H[Initialize New Feature Collection]
    H --> I[Iterate Over Features]
    I --> J{Is Geometry a Polygon?}
    J -- No --> K[Skip Feature]
    J -- Yes --> L[Remove Spikes from Polygon]
    L --> M[Create New Feature with Modified Polygon]
    M --> N[Add New Feature to Collection]
    N --> O{Are More Features?}
    O -- Yes --> I
    O -- No --> P[Write Modified Features to GeoPackage]
    P --> Q[Close GeoPackage File]
    Q --> R[End]
