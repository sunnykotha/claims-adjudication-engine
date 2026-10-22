package com.claims.adjudication;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProviderTest {

    @Test
    void createsProviderWithValidDetails() {
        Provider provider = new Provider("PR001", "City Hospital");

        assertEquals("PR001", provider.providerId());
        assertEquals("City Hospital", provider.name());
    }

    @Test
    void trimsProviderDetails() {
        Provider provider = new Provider(" PR001 ", " City Hospital ");

        assertEquals("PR001", provider.providerId());
        assertEquals("City Hospital", provider.name());
    }

    @Test
    void rejectsBlankProviderId() {
        assertThrows(IllegalArgumentException.class,
                () -> new Provider(" ", "City Hospital"));
    }

    @Test
    void rejectsBlankProviderName() {
        assertThrows(IllegalArgumentException.class,
                () -> new Provider("PR001", " "));
    }

    @Test
    void rejectsNullProviderId() {
        assertThrows(NullPointerException.class,
                () -> new Provider(null, "City Hospital"));
    }

    @Test
    void rejectsNullProviderName() {
        assertThrows(NullPointerException.class,
                () -> new Provider("PR001", null));
    }
}