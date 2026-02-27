package tn.esprit.utils;

public class WeatherResponse {
    private Current current;

    public Current getCurrent() { return current; }

    public static class Current {
        private double temperature_2m;
        private double precipitation;
        private double wind_speed_10m;

        public double getTemperature_2m() { return temperature_2m; }
        public double getPrecipitation() { return precipitation; }
        public double getWind_speed_10m() { return wind_speed_10m; }
    }
}