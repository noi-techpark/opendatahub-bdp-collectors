// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.noi.ondemandmerano.model;

import java.util.HashMap;

public class OnDemandMeranoVehicle {

    private String licensePlateNumber;
    private HashMap<String, Object> type;
    private OnDemandMeranoOperator operator;
    private HashMap<String, Integer> capacityMax;
    private HashMap<String, Integer> capacityUsed;
    private String recordTime;
    private OnDemandServicePositionPoint position;

    public String getLicensePlateNumber() {
        return licensePlateNumber;
    }

    public void setLicensePlateNumber(String licensePlateNumber) {
        this.licensePlateNumber = licensePlateNumber;
    }

    public HashMap<String, Object> getType() {
        return type;
    }

    public void setType(HashMap<String, Object> type) {
        this.type = type;
    }

    public OnDemandMeranoOperator getOperator() {
        return operator;
    }

    public void setOperator(OnDemandMeranoOperator operator) {
        this.operator = operator;
    }

    public HashMap<String, Integer> getCapacityMax() {
        return capacityMax;
    }

    public void setCapacityMax(HashMap<String, Integer> capacityMax) {
        this.capacityMax = capacityMax;
    }

    public HashMap<String, Integer> getCapacityUsed() {
        return capacityUsed;
    }

    public void setCapacityUsed(HashMap<String, Integer> capacityUsed) {
        this.capacityUsed = capacityUsed;
    }

    public String getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(String recordTime) {
        this.recordTime = recordTime;
    }

    public OnDemandServicePositionPoint getPosition() {
        return position;
    }

    public void setPosition(OnDemandServicePositionPoint position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "OnDemandMeranoVehicle{" +
                "licensePlateNumber='" + licensePlateNumber + '\'' +
                ", type='" + type + '\'' +
                ", operator=" + operator +
                ", capacityMax=" + capacityMax +
                ", capacityUsed=" + capacityUsed +
                ", recordTime='" + recordTime + '\'' +
                ", position=" + position +
                '}';
    }
}
