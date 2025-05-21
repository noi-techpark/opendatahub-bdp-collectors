// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.a22elaborations;

import java.util.Map;
import java.util.Objects;

public class EUROType {
    private String targa;
    private Map<String, Double> probabilities;

    public EUROType(String targa, Map<String, Double> probabilities) {
        this.targa = targa;
        this.probabilities = probabilities;
    }

    public String getTarga() {
        return targa;
    }

    public Map<String, Double> getProbabilities() {
        return probabilities;
    }

    @Override
    public String toString() {
        return "VehicleData{" +
                "targa='" + targa + '\'' +
                ", probabilities=" + probabilities +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EUROType that = (EUROType) o;
        return Objects.equals(targa, that.targa) &&
                Objects.equals(probabilities, that.probabilities);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targa, probabilities);
    }
}
