package com.iceye.esa.snap.dataio.util;

import java.util.*;
import java.util.stream.Collectors;

public final class CoordinatesMapper {

    public static class CoordinatePair {
        public double lon;
        public double lat;

        public CoordinatePair(double lon, double lat) {
            this.lon = lon;
            this.lat = lat;
        }
    }

    public static class Coordinates {
        public List<CoordinatePair> coordinates;

        public Coordinates(List<List<Double>> rawCoordinates) {
            this.coordinates = rawCoordinates.stream()
                    .map(coords -> new CoordinatePair(coords.get(0), coords.get(1)))
                    .collect(Collectors.toList());
        }
    }

    public static Map<String, CoordinatePair> assignCorners(Coordinates coords,
                                                            String orbitState,
                                                            String obsDir) {
        Map<String, CoordinatePair> out = new HashMap<>();

        boolean ascending = orbitState.equalsIgnoreCase("ascending");
        boolean lookingRight = obsDir.equalsIgnoreCase("right");
        boolean lookEast = ascending ? lookingRight : !lookingRight;

        List<CoordinatePair> coordinates = coords.coordinates;

        // Sort by latitude
        coordinates.sort(Comparator.comparingDouble(c -> c.lat));

        List<CoordinatePair> firstPair, lastPair;
        if (ascending) {
            firstPair = coordinates.subList(0, 2);
            lastPair = coordinates.subList(2, 4);
        } else {
            firstPair = coordinates.subList(2, 4);
            lastPair = coordinates.subList(0, 2);
        }

        out.put("coord_first_near", pickNear(firstPair, lookEast));
        out.put("coord_first_far", pickFar(firstPair, lookEast));
        out.put("coord_last_near", pickNear(lastPair, lookEast));
        out.put("coord_last_far", pickFar(lastPair, lookEast));

        return out;
    }

    private static CoordinatePair pickNear(List<CoordinatePair> pair, boolean lookEast) {
        return lookEast ? Collections.min(pair, Comparator.comparingDouble(c -> c.lon))
                : Collections.max(pair, Comparator.comparingDouble(c -> c.lon));
    }

    private static CoordinatePair pickFar(List<CoordinatePair> pair, boolean lookEast) {
        return lookEast ? Collections.max(pair, Comparator.comparingDouble(c -> c.lon))
                : Collections.min(pair, Comparator.comparingDouble(c -> c.lon));
    }
}

