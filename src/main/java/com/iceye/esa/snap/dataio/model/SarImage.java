package com.iceye.esa.snap.dataio.model;

/**
 * Contains classes modelling a SAR image.
 */
public class SarImage {

    /**
     * Geo reference of SAR image, mapping of extreme near/far and first/last line pixels to corresponding
     * geo locations.
     */
    public static class SarGeoReference
    {
        public DecimalCoordinates firstNear, firstFar, lastNear, lastFar;

        public SarGeoReference flipNearFar() {
            SarGeoReference flipped = new SarGeoReference();
            flipped.firstNear = this.firstFar;
            flipped.firstFar = this.firstNear;
            flipped.lastNear = this.lastFar;
            flipped.lastFar = this.lastNear;
            return flipped;
        }
    }

}
