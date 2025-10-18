package com.iceye.esa.snap.dataio.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceye.esa.snap.dataio.model.JsonMetadataWrapper;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

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
            coordinates.removeLast(); //todo: verify the order of coords if I can remove the last repeated one
            wrapper.setCoordinates(coordinates);

            // Extract "properties"
            JsonNode propsNode = rootNode.path("properties");
            Map<String, Object> properties = mapper.convertValue(
                    propsNode,
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
            );
            wrapper.setIceyeProperties(properties);

            return wrapper;
        } catch (Exception e) {
            return null;
        }
    }
}
