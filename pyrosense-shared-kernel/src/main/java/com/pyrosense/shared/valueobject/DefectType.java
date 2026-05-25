package com.pyrosense.shared.valueobject;

/**
 * Enumeration of detectable electrical defect types.
 */
public enum DefectType {

    INSULATION_DEGRADATION("Degradation of cable insulation resistance"),
    LOOSE_CONNECTION("Loose or corroded electrical connection"),
    MICRO_ARC("Partial discharge / micro-arc event"),
    OVERLOAD("Circuit carrying current above rated capacity"),
    HARMONIC_DISTORTION("Excessive harmonic content in power signal"),
    TEMPERATURE_RISE("Abnormal temperature increase at connection point"),
    UNKNOWN("Unclassified anomaly detected by sensors");

    private final String description;

    DefectType(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
