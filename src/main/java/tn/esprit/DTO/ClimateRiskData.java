package tn.esprit.DTO;

public class ClimateRiskData {

    private final double avgMaxTemp;
    private final double avgWindSpeed;
    private final double avgPrecip;

    public ClimateRiskData(double avgMaxTemp, double avgWindSpeed, double avgPrecip) {
        this.avgMaxTemp = avgMaxTemp;
        this.avgWindSpeed = avgWindSpeed;
        this.avgPrecip = avgPrecip;
    }

    public double getAvgMaxTemp()   { return avgMaxTemp; }
    public double getAvgWindSpeed() { return avgWindSpeed; }
    public double getAvgPrecip()    { return avgPrecip; }

    @Override
    public String toString() {
        return String.format(
                "ClimateRiskData { avgMaxTemp=%.2f°C, avgWindSpeed=%.2f km/h, avgPrecip=%.2f mm }",
                avgMaxTemp, avgWindSpeed, avgPrecip
        );
    }
}
