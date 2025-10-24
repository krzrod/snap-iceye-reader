package com.iceye.esa.snap.dataio.model;

import com.iceye.esa.snap.dataio.util.CoordinatesMapper;
import com.iceye.esa.snap.dataio.util.CoordinatesMapper.CoordinatePair;

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
        public CoordinatePair firstNear, firstFar, lastNear, lastFar;
    }
}
