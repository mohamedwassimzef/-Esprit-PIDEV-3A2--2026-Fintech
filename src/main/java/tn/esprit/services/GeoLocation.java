package tn.esprit.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GeoLocation {

    /**
     * Get coordinates (latitude, longitude) for a given city + area in Tunisia
     * @param city Example: "Tunis"
     * @param area Example: "La Marsa"
     * @return double array [latitude, longitude]
     * @throws Exception
     */
    public double[] getCoordinates(String city, String area) throws Exception {

        // Prepare URL for Nominatim API
        String address = area + "," + city + ",Pakistan";
        String urlString = "https://nominatim.openstreetmap.org/search?q="
                + address.replace(" ", "+")
                + "&format=json&limit=1";

        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Mozilla/5.0 (Java App)"); // Nominatim requires a User-Agent

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();

        // Parse JSON response
        JsonArray jsonArray = JsonParser.parseString(content.toString()).getAsJsonArray();
        if (jsonArray.size() == 0) {
            throw new Exception("Location not found: " + address);
        }

        JsonObject location = jsonArray.get(0).getAsJsonObject();
        double latitude = location.get("lat").getAsDouble();
        double longitude = location.get("lon").getAsDouble();

        return new double[]{latitude, longitude};
    }

    // Test
    public static void main(String[] args) throws Exception {
        GeoLocation GeoLocation = new GeoLocation();
        double[] coords = GeoLocation.getCoordinates("Tunis", "La Marsa");
        System.out.println("Latitude: " + coords[0]);
        System.out.println("Longitude: " + coords[1]);
    }
}