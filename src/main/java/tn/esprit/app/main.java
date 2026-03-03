package tn.esprit.app;

import tn.esprit.dao.InsuredAssetDAO;
import tn.esprit.dao.InsuredContractDAO;
import tn.esprit.entities.InsuredContract;
import tn.esprit.services.Weather;
import tn.esprit.utils.MyDB;
import tn.esprit.utils.WeatherResponse;

public class Main {

    public static void main(String[] args) {
        System.out.println("hello");

        // Test Weather API: Tunis coordinates
        try {
            double lat = 36.8065;
            double lon = 10.1815;
            WeatherResponse weather = Weather.getWeather(lat, lon);
            System.out.println("Weather API response for Tunis (" + lat + ", " + lon + "):");
            if (weather != null && weather.getCurrent() != null) {
                System.out.println("  Temperature : " + weather.getCurrent().getTemperature_2m() + " °C");
                System.out.println("  Precipitation: " + weather.getCurrent().getPrecipitation() + " mm");
                System.out.println("  Wind speed  : " + weather.getCurrent().getWind_speed_10m() + " km/h");
            } else {
                System.out.println("  (no current data returned)");
            }
        } catch (Exception e) {
            System.out.println("Failed to call Weather API: " + e.getMessage());
        }

        // Guard: skip all DB operations if connection is unavailable
        if (MyDB.getInstance().getConx() == null) {
            System.out.println("Database not available – skipping DB operations.");
            return;
        }

        InsuredAssetDAO insuredAssetDAO = new InsuredAssetDAO();
        InsuredContractDAO insuredContractDAO = new InsuredContractDAO();

        System.out.println("\nAll Insured Assets:");
        System.out.println(insuredAssetDAO.readAll());

        InsuredContract insuredContract = new InsuredContract(
                "REF-TEST-" + System.currentTimeMillis(),
                "DOC-" + System.currentTimeMillis(),
                null
        );

        Boolean created = insuredContractDAO.create(insuredContract);
        System.out.println("\nInsured Contract created: " + created);

        System.out.println("\nAll Insured Contracts:");
        System.out.println(insuredContractDAO.readAll());
    }
}
