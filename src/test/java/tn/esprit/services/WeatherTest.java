package tn.esprit.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tn.esprit.utils.WeatherResponse;

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

        WeatherResponse response = Weather.getWeather(lat, lon);

        // Basic checks
        Assertions.assertNotNull(response, "Weather response should not be null");
        Assertions.assertNotNull(response.getCurrent(), "Current weather section should not be null");
        // Temperature should be in a realistic range
        Assertions.assertTrue(response.getCurrent().getTemperature_2m() >= -100.0
                && response.getCurrent().getTemperature_2m() <= 60.0,
                "Temperature should be in a realistic range");
    }
}

