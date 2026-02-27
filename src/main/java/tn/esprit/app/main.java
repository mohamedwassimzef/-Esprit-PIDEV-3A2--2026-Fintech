package tn.esprit.app;

import tn.esprit.dao.InsuredAssetDAO;
import tn.esprit.dao.InsuredContractDAO;
import tn.esprit.entities.InsuredContract;
import tn.esprit.enums.ContractStatus;
import tn.esprit.services.RiskService;
import tn.esprit.services.Weather;
import tn.esprit.services.GeoLocation;
import java.time.LocalDate;
import java.time.LocalDateTime;
import tn.esprit.DTO.ClimateRiskData;
import tn.esprit.services.WeatherRiskService;
import java.io.*;
import java.net.*;
import java.util.*;

public class Main {

    public static void main(String[] args) throws Exception{
        System.out.println("hello3");

        // Test Weather API: Tunis coordinates


        GeoLocation GeoLocation = new GeoLocation();
        double[] coords = GeoLocation.getCoordinates("Tunis", "Marsa");
        System.out.println("Latitude: " + coords[0]);
        System.out.println("Longitude: " + coords[1]);


        WeatherRiskService service = new WeatherRiskService();

        // Example: Tunis coordinates
        ClimateRiskData data = service.getClimateAverages(coords[0], coords[1]);
        System.out.println(data);
    }
}
