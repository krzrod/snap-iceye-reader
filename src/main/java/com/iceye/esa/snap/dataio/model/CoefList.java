package com.iceye.esa.snap.dataio.model;

import java.util.ArrayList;
import java.util.List;

public class CoefList {

    final List<Double> coefficients = new ArrayList<>();
    double utcSeconds = 0.0;
    double grOrigin = 0.0;

    public List<Double> getCoefficients() {
        return coefficients;
    }

    public double getUtcSeconds() {
        return utcSeconds;
    }

    public void setUtcSeconds(double utcSeconds) {
        this.utcSeconds = utcSeconds;
    }

    public double getGrOrigin() {
        return grOrigin;
    }

    public void setGrOrigin(double grOrigin) {
        this.grOrigin = grOrigin;
    }

    public void addCoefficient(Double coefficient){
        this.coefficients.add(coefficient);
    }
}
