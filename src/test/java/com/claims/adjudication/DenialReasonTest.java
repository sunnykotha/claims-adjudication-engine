package com.claims.adjudication;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DenialReasonTest {

    @Test
    void containsAllRequiredDenialReasons() {
        Set<DenialReason> reasons = Set.copyOf(Arrays.asList(DenialReason.values()));

        assertEquals(7, reasons.size());
        assertTrue(reasons.contains(DenialReason.INELIGIBLE_MEMBER));
        assertTrue(reasons.contains(DenialReason.SERVICE_NOT_COVERED));
        assertTrue(reasons.contains(DenialReason.REFERRAL_REQUIRED));
        assertTrue(reasons.contains(DenialReason.PRIOR_AUTHORIZATION_REQUIRED));
        assertTrue(reasons.contains(DenialReason.DUPLICATE_CLAIM));
        assertTrue(reasons.contains(DenialReason.TIMELY_FILING_EXCEEDED));
        assertTrue(reasons.contains(DenialReason.POLICY_TERMINATED));
    }

    @Test
    void denialReasonsHaveStableEnumNames() {
        assertEquals("INELIGIBLE_MEMBER",
                DenialReason.INELIGIBLE_MEMBER.name());
        assertEquals("DUPLICATE_CLAIM",
                DenialReason.DUPLICATE_CLAIM.name());
        assertEquals("POLICY_TERMINATED",
                DenialReason.POLICY_TERMINATED.name());
    }

    @Test
    void denialReasonsCanBeParsedByName() {
        assertEquals(DenialReason.TIMELY_FILING_EXCEEDED,
                DenialReason.valueOf("TIMELY_FILING_EXCEEDED"));
    }

    @Test
    void invalidDenialReasonNameIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> DenialReason.valueOf("UNKNOWN_REASON"));
    }
}
