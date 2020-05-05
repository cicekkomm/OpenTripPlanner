package org.opentripplanner.graph_builder.module.osm;

import org.junit.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

import static org.junit.Assert.assertEquals;

/**
 * Test the bike safety ratings for GermanyWayPropertySet.
 *
 * @author hbruch
 */
public class TestGermanyWayPropertySetBikeSafety {
    static WayPropertySet wps = new WayPropertySet();
    static float epsilon = 0.01f;
    static WayPropertySetSource source = new GermanyWayPropertySetSource();

    static {
        source.populateProperties(wps);
    }

    /**
     * Test that bike safety factors are calculated accurately
     */
    @Test
    public void testBikeSafety () {

        OSMWithTags way;

        // way 361961158
        way = new OSMWithTags();
        way.addTag("bicycle", "yes");
        way.addTag("foot", "designated");
        way.addTag("footway", "sidewalk");
        way.addTag("highway", "footway");
        way.addTag("lit", "yes");
        way.addTag("oneway", "no");
        way.addTag("traffic_sign", "DE:239,1022-10");
        assertEquals(1.2, wps.getDataForWay(way).getSafetyFeatures().first, epsilon);

        way = new OSMWithTags();
        way.addTag("cycleway", "opposite");
        way.addTag("highway", "residential");
        way.addTag("lit", "yes");
        way.addTag("maxspeed", "30");
        way.addTag("name", "Freibadstraße");
        way.addTag("oneway", "yes");
        way.addTag("oneway:bicycle", "no");
        way.addTag("parking:lane:left", "parallel");
        way.addTag("parking:lane:right", "no_parking");
        way.addTag("sidewalk", "both");
        way.addTag("source:maxspeed", "DE:zone:30");
        way.addTag("surface", "asphalt");
        way.addTag("width", "6.5");
        way.addTag("zone:traffic", "DE:urban");
        assertEquals(0.9, wps.getDataForWay(way).getSafetyFeatures().first, epsilon);

        // way332589799 (Radschnellweg BW1)
        way = new OSMWithTags();
        way.addTag("bicycle", "designated");
        way.addTag("class:bicycle", "2");
        way.addTag("class:bicycle:roadcycling", "1");
        way.addTag("highway", "track");
        way.addTag("horse", "forestry");
        way.addTag("lcn", "yes");
        way.addTag("lit", "yes");
        way.addTag("maxspeed", "30");
        way.addTag("motor_vehicle", "forestry");
        way.addTag("name", "Römerstraße");
        way.addTag("smoothness", "excellent");
        way.addTag("source:maxspeed", "sign");
        way.addTag("surface", "asphalt");
        way.addTag("tracktype", "grade1");
        assertEquals(0.693, wps.getDataForWay(way).getSafetyFeatures().first, epsilon);

        way = new OSMWithTags();
        way.addTag("highway", "track");
        way.addTag("motor_vehicle", "agricultural");
        way.addTag("surface", "asphalt");
        way.addTag("tracktype", "grade1");
        way.addTag("traffic_sign", "DE:260,1026-36");
        way.addTag("width", "2.5");
        assertEquals(1.0, wps.getDataForWay(way).getSafetyFeatures().first, epsilon);

        // https://www.openstreetmap.org/way/124263424
        way = new OSMWithTags();
        way.addTag("highway", "track");
        way.addTag("tracktype", "grade1");
        assertEquals(wps.getDataForWay(way).getPermission(), StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE);
    }

    @Test
    public void lcnAndRcnShouldNotBeAddedUp() {
        // https://www.openstreetmap.org/way/26443041 is part of both an lcn and rnc but that shouldn't mean that
        // it is to be more heavily favoured than other ways that are part of just one.

        var both = new OSMWithTags();
        both.addTag("highway", "residential");
        both.addTag("rcn", "yes");
        both.addTag("lcn", "yes");

        var justLcn = new OSMWithTags();
        justLcn.addTag("lcn", "yes");
        justLcn.addTag("highway", "residential");

        var residential = new OSMWithTags();
        residential.addTag("highway", "residential");

        assertEquals(
                wps.getDataForWay(both).getSafetyFeatures().first,
                wps.getDataForWay(justLcn).getSafetyFeatures().first,
                epsilon
        );

        assertEquals(
                wps.getDataForWay(both).getSafetyFeatures().first,
                0.6859,
                epsilon
        );

        assertEquals(
                wps.getDataForWay(residential).getSafetyFeatures().first,
                0.98,
                epsilon
        );
    }



}
