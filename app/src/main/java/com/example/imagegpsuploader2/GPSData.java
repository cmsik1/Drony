package com.example.imagegpsuploader2;

public class GPSData {

    private double latitude;
    private double longitude;
    private double altitude;
    private float heading;

    public GPSData(double latitude, double longitude, double altitude, float heading) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.heading = heading;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public float getHeading() {
        return heading;
    }

    @Override
    public String toString() {
        return "GPSData{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", altitude=" + altitude +
                ", heading=" + heading +
                '}';
    }
}


