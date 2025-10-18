package com.iceye.esa.snap.dataio.model;

import java.util.List;
import java.util.Map;


public class JsonMetadataWrapper {

    private List<List<Double>> coordinates;
    private Map<String, Object> iceyeProperties;

    public List<List<Double>> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<List<Double>> coordinates) {
        this.coordinates = coordinates;
    }

    public Map<String, Object> getIceyeProperties() {
        return iceyeProperties;
    }

    public void setIceyeProperties(Map<String, Object> iceyeProperties) {
        this.iceyeProperties = iceyeProperties;
    }
}
