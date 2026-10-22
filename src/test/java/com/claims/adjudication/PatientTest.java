package com.claims.adjudication;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PatientTest {

    @Test
    void createsPatientWithValidDetails() {
        Patient patient = new Patient("P001", "John Smith");

        assertEquals("P001", patient.patientId());
        assertEquals("John Smith", patient.name());
    }

    @Test
    void trimsLeadingAndTrailingWhitespace() {
        Patient patient = new Patient(" P001 ", " John Smith ");

        assertEquals("P001", patient.patientId());
        assertEquals("John Smith", patient.name());
    }

    @Test
    void rejectsBlankPatientId() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Patient("   ", "John Smith")
        );
    }

    @Test
    void rejectsBlankPatientName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new Patient("P001", "   ")
        );
    }

    @Test
    void rejectsNullPatientId() {
        assertThrows(
            NullPointerException.class,
            () -> new Patient(null, "John Smith")
        );
    }

    @Test
    void rejectsNullPatientName() {
        assertThrows(
            NullPointerException.class,
            () -> new Patient("P001", null)
        );
    }
    @Test
    void acceptsAndTrimsFamilyId() {
        Patient patient = new Patient("P001", "John Smith", " FAMILY-1 ");

        assertEquals("FAMILY-1", patient.familyId());
    }

    @Test
    void rejectsBlankFamilyIdWhenProvided() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Patient("P001", "John Smith", "   "));
    }

    @Test
    void existingConstructorLeavesFamilyIdUnspecified() {
        Patient patient = new Patient("P001", "John Smith");

        assertNull(patient.familyId());
    }
}