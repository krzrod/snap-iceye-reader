package com.iceye.esa.snap.dataio.model;

import org.geotools.xml.xsi.XSISimpleTypes;

public class DecimalCoordinates {

    double lon, lat;

    public DecimalCoordinates(double lon, double lat) {
        this.lon = lon;
        this.lat = lat;
    }

    public double getLongitude() {
        return this.lon;
    }

    public double getLatitude() {
        return this.lat;
    }

    public static DecimalCoordinates fromLonLat(double[] coords) {
        return new DecimalCoordinates(coords[0], coords[1]);
    }

    public static DecimalCoordinates fromLatLon(double[] coords) {
        return new DecimalCoordinates(coords[1], coords[0]);
    }

    public static double[] longitudes(final DecimalCoordinates[] coords) {
        double[] longitudes = new double[coords.length];
        for(int i = 0 ; i < coords.length; i++)
            longitudes[i] = coords[i].getLongitude();
        return longitudes;
    }

    public static double[] latitudes(final DecimalCoordinates[] coords) {
        double[] latitudes = new double[coords.length];
        for(int i = 0 ; i < coords.length; i++)
            latitudes[i] = coords[i].getLatitude();
        return latitudes;
    }
}
