package com.pyrosense.reporting.domain;

import com.pyrosense.reporting.domain.model.ReportSignature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportSignatureTest {

    @Test
    void shouldComputeSha256Hash() {
        byte[] content = "test content".getBytes();
        ReportSignature sig = ReportSignature.compute(content);

        assertEquals("SHA-256", sig.algorithm());
        assertNotNull(sig.hash());
        assertFalse(sig.hash().isEmpty());
    }

    @Test
    void shouldProduceDeterministicHash() {
        byte[] content = "same content".getBytes();
        ReportSignature sig1 = ReportSignature.compute(content);
        ReportSignature sig2 = ReportSignature.compute(content);

        assertEquals(sig1.hash(), sig2.hash());
    }

    @Test
    void shouldProduceDifferentHashForDifferentContent() {
        ReportSignature sig1 = ReportSignature.compute("content A".getBytes());
        ReportSignature sig2 = ReportSignature.compute("content B".getBytes());

        assertNotEquals(sig1.hash(), sig2.hash());
    }

    @Test
    void shouldProduceHexEncodedHash() {
        ReportSignature sig = ReportSignature.compute("test".getBytes());
        assertTrue(sig.hash().matches("[0-9a-f]+"));
        assertEquals(64, sig.hash().length());
    }
}
