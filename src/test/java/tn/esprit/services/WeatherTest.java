package tn.esprit.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Simple integration-style test for the Weather API wrapper.
 *
 * NOTE: This test performs a real HTTP call to Open-Meteo.
 * It may fail if there is no internet connection or the API is down.
 */
public class WeatherTest {

    @Test
    public void testGetWeather_returnsJsonWithCurrentSection() throws Exception {
        // Coordinates for Tunis, Tunisia (approx)
        double lat = 36.8065;
        double lon = 10.1815;

        String json = Weather.getWeather(lat, lon);

        // Basic checks
        Assertions.assertNotNull(json, "Weather JSON should not be null");
        Assertions.assertFalse(json.isEmpty(), "Weather JSON should not be empty");

        // Open-Meteo current weather responses should contain these keys
        Assertions.assertTrue(json.contains("\"current\""),
                "JSON should contain a 'current' section");
        Assertions.assertTrue(json.contains("temperature"),
                "JSON should contain temperature information");
    }
}

