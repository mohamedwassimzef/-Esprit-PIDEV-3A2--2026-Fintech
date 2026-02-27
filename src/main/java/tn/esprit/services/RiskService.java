package tn.esprit.services;
import tn.esprit.utils.WeatherResponse;

public class RiskService {

    private GeoLocation GeoLocation;
    private Weather Weather;

    public RiskService(GeoLocation GeoLocation, Weather Weather) {
        this.GeoLocation = GeoLocation;
        this.Weather = Weather;
    }

    /**
     * Calculates risk score for an asset given city + area
     * @param city Example: "Tunis"
     * @param area Example: "La Marsa"
     * @return riskScore (1.0 = base, higher = riskier)
     * @throws Exception
     */
    public double calculateRisk(String city, String area) throws Exception {

        // 1️⃣ Get coordinates
        double[] coords = GeoLocation.getCoordinates(city, area);
        double lat = coords[0];
        double lon = coords[1];

        // 2️⃣ Get current weather
        WeatherResponse weather = Weather.getWeather(lat, lon);
        WeatherResponse.Current current = weather.getCurrent();

        // Print weather details for debugging / visibility
        System.out.println("[RiskService] Weather for " + city + ", " + area + ":");
        System.out.println("  temperature_2m = " + current.getTemperature_2m());
        System.out.println("  precipitation  = " + current.getPrecipitation());
        System.out.println("  wind_speed_10m = " + current.getWind_speed_10m());

        // 3️⃣ Calculate risk
        double risk = 1.0; // base risk



        // 4️⃣ Temperature risk (summer heat)
        // Tunisia summer: 30–45°C, inland hotter than coastal
        if (current.getTemperature_2m() >= 35 && current.getTemperature_2m() < 40) {
            risk += 0.10; // moderate heat
        } else if (current.getTemperature_2m() >= 40) {
            risk += 0.20; // extreme heat
        }

        // 5️⃣ Precipitation risk (winter storms, floods)
        // Northern Tunisia gets 5–20 mm during storms
        if (current.getPrecipitation() >= 5 && current.getPrecipitation() < 15) {
            risk += 0.10; // light storm
        } else if (current.getPrecipitation() >= 15) {
            risk += 0.20; // heavy storm
        }

        // 6️⃣ Wind risk (coastal wind / storms)
        // Coastal cities can reach 20–60 km/h during storms
        if (current.getWind_speed_10m() >= 20 && current.getWind_speed_10m() < 40) {
            risk += 0.05; // moderate wind
        } else if (current.getWind_speed_10m() >= 40) {
            risk += 0.10; // strong wind
        }

        System.out.println("[RiskService] Computed risk = " + risk);
        return risk;
    }

    // Test
    public static void main(String[] args) throws Exception {
        GeoLocation GeoLocation = new GeoLocation();
        Weather Weather = new Weather();

        RiskService riskService = new RiskService(GeoLocation, Weather);
        double riskScore = riskService.calculateRisk("Tunis", "La Marsa");

        System.out.println("Calculated Risk Score: " + riskScore);
    }
}