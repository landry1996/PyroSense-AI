package com.pyrosense.device.application.port.out;

public interface EnrollmentKeyGeneratorPort {

    record EnrollmentKeyPair(String plainKey, String keyHash) {}

    EnrollmentKeyPair generate();
}
