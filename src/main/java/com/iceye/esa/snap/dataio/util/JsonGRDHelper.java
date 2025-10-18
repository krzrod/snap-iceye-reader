package com.iceye.esa.snap.dataio.util;

import com.iceye.esa.snap.dataio.model.JsonMetadataWrapper;
import org.esa.snap.engine_utilities.eo.Constants;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.iceye.esa.snap.dataio.util.IceyeXConstants.*;

public final class JsonGRDHelper {

    private static final String START_DATETIME = "start_datetime";
    private static final String END_DATETIME = "end_datetime";
    private static final String PRODUCT_TYPE = "sar:instrument_mode";
    private static final String PRODUCT_LEVEL = "sar:product_type";
    private static final String AVG_SCENE_HEIGHT = "iceye:average_scene_height";
    private static final String AZIMUTH_LOOKS = "sar:looks_azimuth";
    private static final String AZIMUTH_SPACING = "sar:pixel_spacing_azimuth";
    private static final String CALIBRATION_FACTOR = "iceye:calibration_factor";
    private static final String CARRIER_FREQUENCY = "sar:center_frequency";
    private static final String PULSE_BANDWIDTH = "iceye:pulse_bandwidth";
    private static final String DOPPLER_CENTROID_COEFFICIENTS = "iceye:doppler_centroid_coeffs";
    private static final String DOPPLER_CENTROID_DATETIMES = "iceye:doppler_centroid_datetimes";
    private static final String ORBIT_STATES = "iceye:orbit_states";
    private static final String GRSR_COEFFICIENTS = "iceye:ground_to_slant_coeff";
    private static final String DOPPLER_RATE_COEFFICIENTS = "iceye:doppler_rate_coeffs";
    private static final String INCIDENCE_ANGLE_COEFFICIENTS = "iceye:incidence_angle_coeffs";
    private static final String INCIDENCE_ANGLE_NEAR = "iceye:incidence_angle_near";
    private static final String INCIDENCE_ANGLE_FAR = "iceye:incidence_angle_far";
    private static final String OBSERVATION_DIRECTION = "sar:observation_direction";
    private static final String SHAPE_DIMENSIONS = "proj:shape";
    private static final String CENTROID_COORDINATE = "proj:centroid";
    private static final String ORBIT_DIRECTION = "sat:orbit_state";
    private static final String POLARIZATION = "sar:polarizations";
    private static final String PROCESSING_PRF = "iceye:processing_prf";
    private static final String PROCESSING_TIME = "iceye:processing_end_datetime";
    private static final String PROCESSOR_SOFTWARE = "processing:software";
    private static final String PRODUCT_FILENAME = "iceye:filename";
    private static final String LOOKS_RANGE = "sar:looks_range";
    private static final String RANGE_SAMPLING_RATE = "iceye:acquisition_range_sampling_rate";
    private static final String PIXEL_SPACING_RANGE = "sar:pixel_spacing_range";
    private static final String PLATFORM = "platform";
    private static final String PROCESSED_BANDWIDTH_AZIMUTH = "iceye:processing_bandwidth_azimuth";
    private static final String ZERO_DOPPLER_START_UTC = "iceye:zero_doppler_start_datetime";
    private static final String ZERO_DOPPLER_END_UTC = "iceye:zero_doppler_end_datetime";
    private static final String GEO_REFERENCE_SYSTEM = "iceye:coordinate_frame";
    private static final String ICEYE_RANGE_NEAR = "iceye:range_near";
    private static final String ICEYE_RANGE = "iceye:range";

    public static void mapJsonMetadataToTiffFields(JsonMetadataWrapper metadataWrapper, Map<String, String> tiffFields) {
        Map<String, Object> iceyeProperties = metadataWrapper.getIceyeProperties();

        String startTime = (String) iceyeProperties.get(START_DATETIME);
        String endTime = (String) iceyeProperties.get(END_DATETIME);

        tiffFields.put(PASS.toUpperCase(), (String) iceyeProperties.get(ORBIT_DIRECTION));
        tiffFields.put(ANTENNA_POINTING.toUpperCase(), (String) iceyeProperties.get(OBSERVATION_DIRECTION));
        tiffFields.put(PULSE_REPETITION_FREQUENCY.toUpperCase(), String.valueOf(iceyeProperties.get(PROCESSING_PRF)));
        tiffFields.put(PROC_TIME_UTC.toUpperCase(), normalizeDateTimeToSnapFormat((String) iceyeProperties.get(PROCESSING_TIME)));
        tiffFields.put(IceyeXConstants.PRODUCT_TYPE.toUpperCase(), (String) iceyeProperties.get(PRODUCT_TYPE));
        tiffFields.put(RANGE_LOOKS.toUpperCase(), String.valueOf(iceyeProperties.get(LOOKS_RANGE)));
        tiffFields.put(IceyeXConstants.RANGE_SAMPLING_RATE.toUpperCase(), String.valueOf(iceyeProperties.get(RANGE_SAMPLING_RATE)));
        tiffFields.put(RANGE_SPACING.toUpperCase(), String.valueOf(iceyeProperties.get(PIXEL_SPACING_RANGE)));
        tiffFields.put(MISSION.toUpperCase(), (String) iceyeProperties.get(PLATFORM));
        tiffFields.put(AZIMUTH_BANDWIDTH.toUpperCase(), String.valueOf(iceyeProperties.get(PROCESSED_BANDWIDTH_AZIMUTH)));
        tiffFields.put(FIRST_LINE_TIME.toUpperCase(), normalizeDateTimeToSnapFormat((String) (iceyeProperties.get(ZERO_DOPPLER_START_UTC))));
        tiffFields.put(LAST_LINE_TIME.toUpperCase(), normalizeDateTimeToSnapFormat(String.valueOf(iceyeProperties.get(ZERO_DOPPLER_END_UTC))));
        tiffFields.put(IceyeXConstants.AVG_SCENE_HEIGHT.toUpperCase(), String.valueOf(iceyeProperties.get(AVG_SCENE_HEIGHT)));
        tiffFields.put(IceyeXConstants.AZIMUTH_LOOKS.toUpperCase(), String.valueOf(iceyeProperties.get(AZIMUTH_LOOKS)));
        tiffFields.put(IceyeXConstants.AZIMUTH_SPACING.toUpperCase(), String.valueOf(iceyeProperties.get(AZIMUTH_SPACING)));
        tiffFields.put(IceyeXConstants.CALIBRATION_FACTOR.toUpperCase(), String.valueOf(iceyeProperties.get(CALIBRATION_FACTOR)));
        tiffFields.put(RADAR_FREQUENCY.toUpperCase(), String.valueOf(iceyeProperties.get(CARRIER_FREQUENCY)));
        tiffFields.put(RANGE_BANDWIDTH.toUpperCase(), String.valueOf(iceyeProperties.get(PULSE_BANDWIDTH)));
        tiffFields.put(INCIDENCE_NEAR.toUpperCase(), String.valueOf(iceyeProperties.get(INCIDENCE_ANGLE_NEAR)));
        tiffFields.put(INCIDENCE_FAR.toUpperCase(), String.valueOf(iceyeProperties.get(INCIDENCE_ANGLE_FAR)));
        tiffFields.put(IceyeXConstants.GEO_REFERENCE_SYSTEM.toUpperCase(),
                (String) iceyeProperties.get(GEO_REFERENCE_SYSTEM));
        tiffFields.put(GRSR_ZERO_DOPPLER_TIME.toUpperCase(), mapGRSRZeroDopplerTime(startTime, endTime));
        //todo: taken from iceye:range_near for now. To be further confirmed by Julio
        tiffFields.put(SLANT_RANGE_TO_FIRST_PIXEL.toUpperCase(), String.valueOf(iceyeProperties.get(ICEYE_RANGE_NEAR)));
        mapFirstPixelTime(Double.valueOf(tiffFields.get(SLANT_RANGE_TO_FIRST_PIXEL.toUpperCase())), tiffFields);
        //todo, for now removed from json, discussed with Iceye and using default value
        tiffFields.put(ANT_ELEV_CORR_FLAG.toUpperCase(), String.valueOf(ANT_ELEV_CORR_FLAG_DEFAULT_VALUE));
        //todo, for now removed from json, discussed with Iceye and using default value
        tiffFields.put(RANGE_SPREAD_COMP_FLAG.toUpperCase(),
                String.valueOf(RANGE_SPREAD_COMP_FLAG_DEFAULT_VALUE));
        /*Unverified but discussed with Iceye */
        //todo: probably not needed for reader to work but for now using default provided by iceye
        tiffFields.put(ABS_ORBIT.toUpperCase(), "33333");
        //todo: probably not needed for reader to work but for now using default provided by iceye
        tiffFields.put(REL_ORBIT.toUpperCase(), "33333");
        //todo: probably not needed for reader to work but for now using default provided by iceye
        tiffFields.put(CYCLE.toUpperCase(), "99999");

        //todo: unknown, temporarily hardcoded to 0.0
        tiffFields.put(GRSR_GROUND_RANGE_ORIGIN.toUpperCase(), "0.0");

        mapAcquisitionTimes(startTime, endTime, tiffFields);
        mapProductLevel((String) iceyeProperties.get(PRODUCT_LEVEL), tiffFields);
        mapOrbitStates((ArrayList<LinkedHashMap<String, Object>>) iceyeProperties.get(ORBIT_STATES), tiffFields);
        mapDopplerCentroidCoefficients((List<List<Double>>) iceyeProperties.get(DOPPLER_CENTROID_COEFFICIENTS), tiffFields);
        mapDopplerCentroidDateTimes((List<String>) iceyeProperties.get(DOPPLER_CENTROID_DATETIMES), tiffFields);
        mapGrsrCoefficients((List<Double>) iceyeProperties.get(GRSR_COEFFICIENTS), tiffFields);
        mapDopplerRateCoefficients((List<Double>) iceyeProperties.get(DOPPLER_RATE_COEFFICIENTS), tiffFields);
        mapIncidenceAngleCoefficients((List<Double>) iceyeProperties.get(INCIDENCE_ANGLE_COEFFICIENTS), tiffFields);
        mapShape((List<Double>) iceyeProperties.get(SHAPE_DIMENSIONS), tiffFields);
        mapPolarization((List<String>) iceyeProperties.get(POLARIZATION), tiffFields);
        mapProcessor((Map<String, String>) iceyeProperties.get(PROCESSOR_SOFTWARE), tiffFields);
        mapProductName((String) iceyeProperties.get(PRODUCT_FILENAME), tiffFields);
        mapAzimuthTimeInterval(startTime, endTime, Integer.parseInt(tiffFields.get(NUM_OUTPUT_LINES.toUpperCase())), tiffFields);
        mapDCReferencePixelTime((Double) iceyeProperties.get(ICEYE_RANGE), tiffFields);
        mapCornerCoordinates(
                metadataWrapper.getCoordinates(),
                tiffFields.get(PASS.toUpperCase()),
                tiffFields.get(ANTENNA_POINTING.toUpperCase()),
                tiffFields);
        mapCenterCoord
                ((Map<String, Double>) iceyeProperties.get(CENTROID_COORDINATE),
                        tiffFields.get(NUM_OUTPUT_LINES.toUpperCase()),
                        tiffFields.get(NUM_SAMPLES_PER_LINE.toUpperCase()), tiffFields);
    }

    public static void mapOrbitStates(ArrayList<LinkedHashMap<String, Object>> orbitVectorStates, Map<String, String> tiffFields) {
        final StringBuilder orbitStateTimes = new StringBuilder("[");
        final StringBuilder positionX = new StringBuilder("[");
        final StringBuilder positionY = new StringBuilder("[");
        final StringBuilder positionZ = new StringBuilder("[");
        final StringBuilder velocityX = new StringBuilder("[");
        final StringBuilder velocityY = new StringBuilder("[");
        final StringBuilder velocityZ = new StringBuilder("[");
        orbitVectorStates.forEach(orbitState -> {
            String time = (String) orbitState.get("time");
            orbitStateTimes.append(normalizeDateTimeToSnapFormat(time));
            orbitStateTimes.append(",");
            ArrayList<Double> positionList = (ArrayList<Double>) orbitState.get("position");
            positionX.append(positionList.get(0)).append(" ");
            positionY.append(positionList.get(1)).append(" ");
            positionZ.append(positionList.get(2)).append(" ");
            ArrayList<Double> velocityList = (ArrayList<Double>) orbitState.get("velocity");
            velocityX.append(velocityList.get(0)).append(" ");
            velocityY.append(velocityList.get(1)).append(" ");
            velocityZ.append(velocityList.get(2)).append(" ");
        });
        orbitStateTimes.append("]");
        positionX.append("]");
        positionY.append("]");
        positionZ.append("]");
        velocityX.append("]");
        velocityY.append("]");
        velocityZ.append("]");

        tiffFields.put(STATE_VECTOR_TIME.toUpperCase(), orbitStateTimes.toString());
        tiffFields.put(ORBIT_VECTOR_N_X_POS.toUpperCase(), positionX.toString());
        tiffFields.put(ORBIT_VECTOR_N_Y_POS.toUpperCase(), positionY.toString());
        tiffFields.put(ORBIT_VECTOR_N_Z_POS.toUpperCase(), positionZ.toString());
        tiffFields.put(ORBIT_VECTOR_N_X_VEL.toUpperCase(), velocityX.toString());
        tiffFields.put(ORBIT_VECTOR_N_Y_VEL.toUpperCase(), velocityY.toString());
        tiffFields.put(ORBIT_VECTOR_N_Z_VEL.toUpperCase(), velocityZ.toString());
        tiffFields.put(NUMBER_OF_STATE_VECTORS.toUpperCase(), String.valueOf(orbitVectorStates.size()));
    }

    public static void mapDopplerCentroidCoefficients(List<List<Double>> dcCentroidCoefficients, Map<String, String> tiffFields) {
        final StringBuilder coefficientBuilder = new StringBuilder("[");
        int polyOrder = -1;
        for (List<Double> coeffList : dcCentroidCoefficients) {
            coefficientBuilder.append("[");
            coeffList.forEach(coeff -> {
                coefficientBuilder.append(coeff);
                coefficientBuilder.append(" ");
            });
            coefficientBuilder.append("]\n");
            if (polyOrder == -1) {
                polyOrder = coeffList.size() - 1;
            }
        }
        tiffFields.put(DC_ESTIMATE_COEFFS.toUpperCase(), coefficientBuilder.toString());
        tiffFields.put(DC_ESTIMATE_POLY_ORDER.toUpperCase(), String.valueOf(polyOrder));
    }

    private static void mapDopplerCentroidDateTimes(List<String> dcCentroidDateTimes, Map<String, String> tiffFields) {
        String joined = dcCentroidDateTimes.stream()
                .map(dateTime -> String.format("\'%s\'", dateTime))
                .map(JsonGRDHelper::normalizeDateTimeToSnapFormat)
                .collect(Collectors.joining(","));
        tiffFields.put(DC_ESTIMATE_TIME_UTC.toUpperCase(), String.format("[%s]", joined));
    }

    private static void mapGrsrCoefficients(List<Double> grsrCoeffs, Map<String, String> tiffFields) {
        String coeffsAsString = grsrCoeffs.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
        tiffFields.put(IceyeXConstants.GRSR_COEFFICIENTS.toUpperCase(), "[" + coeffsAsString + "]");
    }

    private static void mapDopplerRateCoefficients(List<Double> drCoeffs, Map<String, String> tiffFields) {
        String coeffsAsString = drCoeffs.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
        tiffFields.put(DR_COEFFS.toUpperCase(), "[" + coeffsAsString + "]");
    }

    private static void mapIncidenceAngleCoefficients(List<Double> incidenceAngleCoeffs, Map<String, String> tiffFields) {
        // todo verify if the IceyeX constant is correct
        String coeffsAsString = incidenceAngleCoeffs.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(" "));
        tiffFields.put(INCIDENCE_ANGLES.toUpperCase(), "[" + coeffsAsString + "]");
    }

    private static void mapShape(List<Double> shapeDimensions, Map<String, String> tiffFields) {
        tiffFields.put(NUM_SAMPLES_PER_LINE.toUpperCase(), String.valueOf(shapeDimensions.get(0))); //width
        tiffFields.put(NUM_OUTPUT_LINES.toUpperCase(), String.valueOf(shapeDimensions.get(1))); //height
    }

    private static void mapPolarization(List<String> polarizations, Map<String, String> tiffFields) {
        tiffFields.put(MDS1_TX_RX_POLAR.toUpperCase(), polarizations.stream().findFirst().orElse("VV"));
    }

    private static void mapProcessor(Map<String, String> procSoftware, Map<String, String> tiffFields) {
        tiffFields.put(PROCESSING_SYSTEM_IDENTIFIER.toUpperCase(), procSoftware.get("processor"));
    }

    private static void mapProductName(String fileName, Map<String, String> tiffFields) {
        final String productName = fileName.substring(0, fileName.lastIndexOf("."));
        tiffFields.put(PRODUCT.toUpperCase(), productName);
    }

    private static void mapProductLevel(String originalProduct, Map<String, String> tiffFields) {
        String productLevel;
        if (originalProduct.contains("GRD")) {
            productLevel = "GRD";
        } else if (originalProduct.contains("SLC")) {
            productLevel = "SLC";
        } else {
            productLevel = originalProduct;
        }
        tiffFields.put(SPH_DESCRIPTOR.toUpperCase(), productLevel);
    }

    private static void mapAcquisitionTimes(String startTime, String endTime, Map<String, String> tiffFields) {
        tiffFields.put(ACQUISITION_START_UTC.toUpperCase(), normalizeDateTimeToSnapFormat(startTime));
        tiffFields.put(ACQUISITION_END_UTC.toUpperCase(), normalizeDateTimeToSnapFormat(endTime));
    }

    private static String normalizeDateTimeToSnapFormat(String dateTime) {
        String formattedDateTime = dateTime.replace("Z", "");
        int dotIndex = formattedDateTime.indexOf('.');
        String integerPart = formattedDateTime.substring(0, dotIndex);
        String fractionPart = formattedDateTime.substring(dotIndex + 1);
        // normalize to exactly 6 digits
        if (fractionPart.length() < 6) {
            fractionPart = String.format("%-6s", fractionPart).replace(' ', '0');
        } else if (fractionPart.length() > 6) {
            fractionPart = fractionPart.substring(0, 6);
        }

        return integerPart + "." + fractionPart;
    }

    private static void mapAzimuthTimeInterval(String startTime, String endTime, int height, Map<String, String> tiffFields) {
        Instant endInstant = Instant.parse(endTime);
        Instant startInstant = Instant.parse(startTime);

        double timeInterval = (Duration.between(startInstant, endInstant).toMillis() / 1000.0) / height;

        tiffFields.put(LINE_TIME_INTERVAL.toUpperCase(), String.valueOf(timeInterval));
    }

    private static void mapDCReferencePixelTime(Double range, Map<String, String> tiffFields) {
        double pixelTime = range / Constants.lightSpeed * 2;
        tiffFields.put(DC_REFERENCE_PIXEL_TIME.toUpperCase(), String.valueOf(pixelTime));
    }

    private static String mapGRSRZeroDopplerTime(String startTime, String endTime) {
        long startTimeMilis = Instant.parse(startTime).toEpochMilli();
        long endTimeMilis = Instant.parse(endTime).toEpochMilli();
        long datesDiffAvg = startTimeMilis + (endTimeMilis - startTimeMilis) / 2;
        Instant grsrZeroDateTime = Instant.ofEpochMilli(datesDiffAvg);
        return normalizeDateTimeToSnapFormat(grsrZeroDateTime.toString());
    }

    public static void mapCornerCoordinates(List<List<Double>> rawCoordinates, String orbitState, String lookDirection, Map<String, String> tiffFields) {
        CoordinatesMapper.Coordinates coordinates = new CoordinatesMapper.Coordinates(rawCoordinates);
        Map<String, CoordinatesMapper.CoordinatePair> stringCoordinatePairMap = CoordinatesMapper.assignCorners(coordinates, orbitState, lookDirection);
        CoordinatesMapper.CoordinatePair coordFirstNear = stringCoordinatePairMap.get("coord_first_near");
        CoordinatesMapper.CoordinatePair coordFirstFar = stringCoordinatePairMap.get("coord_first_far");
        CoordinatesMapper.CoordinatePair coordLastNear = stringCoordinatePairMap.get("coord_last_near");
        CoordinatesMapper.CoordinatePair coordLastFar = stringCoordinatePairMap.get("coord_last_far");

        tiffFields.put(FIRST_NEAR.toUpperCase(), "[" + coordFirstNear.lat + ", " + coordFirstNear.lon + "]");
        tiffFields.put(FIRST_FAR.toUpperCase(), "[" + coordFirstFar.lat + ", " + coordFirstFar.lon + "]");
        tiffFields.put(LAST_NEAR.toUpperCase(), "[" + coordLastNear.lat + ", " + coordLastNear.lon + "]");
        tiffFields.put(LAST_FAR.toUpperCase(), "[" + coordLastFar.lat + ", " + coordLastFar.lon + "]");
    }

    public static void mapCenterCoord(Map<String, Double> centerCoord, String width, String height, Map<String, String> tiffFields) {
        final String coordCenter = "[" + height + ", " + width + ", " + centerCoord.get("lat") + ", " + centerCoord.get("lon") + "]";
        tiffFields.put(COORD_CENTER.toUpperCase(), coordCenter);
    }

    public static void mapFirstPixelTime(Double slantRangeNear, Map<String, String> tiffFields) {
        double firstPixelTime = (slantRangeNear * 2) / Constants.lightSpeed * 2;
        tiffFields.put(FIRST_PIXEL_TIME.toUpperCase(), String.valueOf(firstPixelTime));
    }
}
