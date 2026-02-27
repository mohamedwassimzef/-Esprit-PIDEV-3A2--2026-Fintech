package tn.esprit.services;

import java.io.*;
import java.net.*;
import java.util.*;
import tn.esprit.DTO.ClimateRiskData;

public class WeatherRiskService {

    public ClimateRiskData getClimateAverages(double lat, double lon) throws Exception {
        String url = String.format(
                "https://archive-api.open-meteo.com/v1/archive" +
                        "?latitude=%f&longitude=%f" +
                        "&start_date=2023-01-01&end_date=2023-12-31" +
                        "&daily=temperature_2m_max,windspeed_10m_max,precipitation_sum" +
                        "&timezone=auto",
                lat, lon
        );

        String jsonResponse = sendGetRequest(url);
        return parseAndMax(jsonResponse);
    }

    private String sendGetRequest(String urlString) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/json");

            int status = connection.getResponseCode();
            if (status != 200) {
                throw new RuntimeException("API call failed with status: " + status);
            }

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();

        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private ClimateRiskData parseAndMax(String json) {
        double maxTemp   = extractMax(json, "temperature_2m_max");
        double maxWind   = extractMax(json, "windspeed_10m_max");
        double maxPrecip = extractMax(json, "precipitation_sum");

        return new ClimateRiskData(maxTemp, maxWind, maxPrecip);
    }

    private double extractMax(String json, String fieldName) {
        // Find the array for the given field name
        String key = "\"" + fieldName + "\":[";
        int start = json.indexOf(key);
        if (start == -1) return 0.0;

        start += key.length();
        int end = json.indexOf("]", start);
        if (end == -1) return 0.0;

        String arrayContent = json.substring(start, end);
        String[] values = arrayContent.split(",");

        Double max = null;
        for (String val : values) {
            val = val.trim();
            if (!val.equals("null") && !val.isEmpty()) {
                double d = Double.parseDouble(val);
                if (max == null || d > max) {
                    max = d;
                }
            }
        }

        return max != null ? max : 0.0;

    }


    public static void main(String[] args){

    }
}