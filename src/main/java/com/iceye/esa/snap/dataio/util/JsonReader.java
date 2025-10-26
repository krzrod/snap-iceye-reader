package com.iceye.esa.snap.dataio.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceye.esa.snap.dataio.model.JsonMetadataWrapper;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public class JsonReader {

    private final static ObjectMapper mapper = new ObjectMapper();

    public static JsonMetadataWrapper readMetadata(Path jsonFilePath) {

        // Read the JSON into a tree
        try {
            JsonNode rootNode = mapper.readTree(new File(jsonFilePath.toString()));

            JsonMetadataWrapper wrapper = new JsonMetadataWrapper();

            // Extract "geometry.coordinates"
            JsonNode coordsNode = rootNode.path("geometry").path("coordinates").get(0);
            List<List<Double>> coordinates = mapper.convertValue(
                    coordsNode,
                    mapper.getTypeFactory().constructCollectionType(List.class,
                            mapper.getTypeFactory().constructCollectionType(List.class, Double.class))
            );
            wrapper.setCoordinates(coordinates);

            // Extract "properties"
            JsonNode propsNode = rootNode.path("properties");
            Map<String, Object> properties = mapper.convertValue(
                    propsNode,
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
            );

            // correction - ObjMapper automatically converts '0.0' to Double but '0' to Integer,
            // but we expect them to be Doubles always,  like in doppler centroid coeffs
            convertNestedLists(properties.get("proj:transform"), JsonReader::intToDouble);
            convertNestedLists(properties.get("iceye:doppler_centroid_coeffs"), JsonReader::intToDouble);
            convertNestedLists(properties.get("iceye:doppler_rate_coeffs"), JsonReader::intToDouble);
            convertNestedLists(properties.get("iceye:incidence_angle_coeffs"), JsonReader::intToDouble);
            convertNestedLists(properties.get("line_num_coeff"), JsonReader::intToDouble);
            convertNestedLists(properties.get("line_den_coeff"), JsonReader::intToDouble);
            convertNestedLists(properties.get("samp_num_coeff"), JsonReader::intToDouble);
            convertNestedLists(properties.get("samp_den_coeff"), JsonReader::intToDouble);
            convertNestedLists(properties.get("iceye:ground_to_slant_coeff"), JsonReader::intToDouble);
            convertNestedLists(properties.get("iceye:focal_plane_normal"), JsonReader::intToDouble);
            convertNestedLists(properties.get("iceye:image_plane_normal"), JsonReader::intToDouble);

            wrapper.setIceyeProperties(properties);

            return wrapper;
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T,U> void convertNestedLists(Object item, Function<Object,U> valueConverter) {
        // precautions for nulls and non matching types
        if (! (item instanceof List)) {
            return;
        }

        List aList = (List) item;

        for (int i = 0  ; i < aList.size(); i++) {
            Object elem = aList.get(i);
            if (elem instanceof List) {
                convertNestedLists((List)elem, valueConverter);
            }
            else {
                U converted = valueConverter.apply(elem);
                if (null != converted) {
                    aList.set(i, converted);
                }
            }
        }
    }

    private static Double intToDouble(Object value) {
        if (value instanceof Integer)
            return ((Integer)value) * 1.0;
        return null;
    }


}
