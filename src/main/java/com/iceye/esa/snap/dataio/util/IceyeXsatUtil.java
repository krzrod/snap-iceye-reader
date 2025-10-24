package com.iceye.esa.snap.dataio.util;

import com.iceye.esa.snap.dataio.model.SarImage.SarGeoReference;
import com.iceye.esa.snap.dataio.util.CoordinatesMapper.CoordinatePair;

import java.util.List;

public class IceyeXsatUtil {

    public static SarGeoReference getSarGeoReference(CoordinatePair[] coordinateSeq, boolean lookingLeft) {
        final SarGeoReference geoReference = new SarGeoReference();
        if (lookingLeft) {
            geoReference.lastFar = coordinateSeq[0];
            geoReference.firstFar = coordinateSeq[1];
            geoReference.firstNear = coordinateSeq[2];
            geoReference.lastNear = coordinateSeq[3];
        }
        else {
            geoReference.firstFar = coordinateSeq[0];
            geoReference.lastFar = coordinateSeq[1];
            geoReference.lastNear = coordinateSeq[2];
            geoReference.firstNear = coordinateSeq[3];
        }

        return geoReference;
    }
}
