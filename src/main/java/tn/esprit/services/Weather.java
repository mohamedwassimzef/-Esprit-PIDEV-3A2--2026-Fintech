package tn.esprit.services;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.Gson;
import tn.esprit.utils.WeatherResponse;

public class Weather {

    public static WeatherResponse getWeather(double lat, double lon) throws Exception {
        String urlString = "https://api.open-meteo.com/v1/forecast?latitude="
                + lat + "&longitude=" + lon + "&current=temperature_2m,precipitation,wind_speed_10m";

        URL url = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();
        Gson gson = new Gson();
        WeatherResponse response = gson.fromJson(content.toString(), WeatherResponse.class);


        return response;

    }
}