package org.opentripplanner.analyst.batch;

import com.csvreader.CsvReader;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

public class CSVPopulation extends BasicPopulation {

    private static final Logger LOG = LoggerFactory.getLogger(CSVPopulation.class);

    public int yCol = 0;
    
    public void setLatCol(int latCol) {
    	yCol = latCol;
    }

    public int xCol = 1;

    public void setLonCol(int lonCol) {
    	xCol = lonCol;
    }
    
    public int labelCol = 2;

    public int inputCol = 3;
    
    public String crs = null;

    public boolean skipHeaders = true;

    @Override
    public void createIndividuals() {
        try {
            CsvReader reader = new CsvReader(sourceFilename, ',', Charset.forName("UTF8"));
            if (skipHeaders) {
                reader.readHeaders();
            }

            // deal with non-WGS84 data

            MathTransform mathTransform = null;
            boolean transform = false;
            CoordinateReferenceSystem destCrs = CRS.decode("EPSG:4326");
            Boolean latLon = null;
            if (crs != null) {
                CoordinateReferenceSystem sourceCrs = CRS.decode(crs);

                // make sure coordinates come out in the right order
                // lat,lon: geotools default
                if (CRS.getAxisOrder(destCrs) == CRS.AxisOrder.NORTH_EAST)
                    latLon = true;
                else if (CRS.getAxisOrder(destCrs) == CRS.AxisOrder.EAST_NORTH)
                    latLon = false;
                else
                    throw new UnsupportedOperationException("Coordinate axis order for WGS 84 unknown.");


                if (!destCrs.equals(sourceCrs)) {
                    transform = true;

                    // find the transformation, being strict about datums etc.
                    mathTransform = CRS.findMathTransform(sourceCrs, destCrs, false);
                }
            }

            while (reader.readRecord()) {
                double y = Double.parseDouble(reader.get(yCol));
                double x = Double.parseDouble(reader.get(xCol));

                double lon, lat;
                if (transform) {
                    DirectPosition2D orig = new DirectPosition2D(x, y);
                    DirectPosition2D transformed = new DirectPosition2D();
                    mathTransform.transform(orig, transformed);

                    // x: lat, y: lon. This seems backwards but is the way Geotools does it. 
                    if (latLon) {
                        lon = transformed.getY();
                        lat = transformed.getX();	
                    } 
                    // x: lon, y: lat
                    else {
                        lon = transformed.getX();
                        lat = transformed.getY();
                    }
                }                	
                else {
                    lon = x;
                    lat = y;
                }

                String label = reader.get(labelCol);
                Double input = Double.parseDouble(reader.get(inputCol));
                // at this point x and y are expressed in WGS84
                Individual individual = new Individual(label, lon, lat, input);
                this.addIndividual(individual);
            }
            reader.close();
        } catch (Exception e) {
            LOG.error("exception while loading individuals from CSV file", e);
        }
    }

}
