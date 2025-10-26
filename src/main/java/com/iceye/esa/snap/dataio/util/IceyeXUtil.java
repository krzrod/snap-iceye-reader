package com.iceye.esa.snap.dataio.util;

import com.iceye.esa.snap.dataio.model.DecimalCoordinates;
import com.iceye.esa.snap.dataio.model.SarImage.SarGeoReference;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;

import java.util.Map;

import static com.iceye.esa.snap.dataio.model.DecimalCoordinates.fromLatLon;
import static com.iceye.esa.snap.dataio.util.ConversionUtil.convertStringToDoubleArray;
import static com.iceye.esa.snap.dataio.util.IceyeXConstants.ANTENNA_POINTING;

public class IceyeXUtil {

    public static boolean isLookingLeft(final Map<String, String> tiffFields) {
        return ! IceyeXConstants.RIGHT.equalsIgnoreCase(tiffFields.get(ANTENNA_POINTING.toUpperCase()));
    }

    public static SarGeoReference getSarGeoReference(DecimalCoordinates[] coordinateSeq, boolean lookingLeft) {
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

    public static SarGeoReference getSarGeoReference(final Map<String, String> tiffFields) {
        SarGeoReference geoReference = new SarGeoReference();
        geoReference.firstNear = fromLatLon(convertStringToDoubleArray(tiffFields.get(IceyeXConstants.FIRST_NEAR.toUpperCase())));
        geoReference.firstFar = fromLatLon(convertStringToDoubleArray(tiffFields.get(IceyeXConstants.FIRST_FAR.toUpperCase())));
        geoReference.lastNear = fromLatLon(convertStringToDoubleArray(tiffFields.get(IceyeXConstants.LAST_NEAR.toUpperCase())));
        geoReference.lastFar = fromLatLon(convertStringToDoubleArray(tiffFields.get(IceyeXConstants.LAST_FAR.toUpperCase())));

        return geoReference;
    }

}
