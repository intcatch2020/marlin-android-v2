package com.intcatch.marlin2;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.TreeMap;

class StateDecoder {

    // JSON object
    private JSONObject fullJSON;

    // State parameters
    private int compassCalibration;
    private int gpsFix;
    private double latitude;
    private double longitude;
    private double speedGPS;
    private int autonomySpeed;
    private int reachedPoint;
    private ArrayList<GeoPoint> pathPointList;
    private double heading;
    private int drivingMode;

    // Pump parameters
    private boolean pumpOn;
    private int pumpSpeed;
    private int pumpTime;

    // State sensors
    private TreeMap<String, SensorData> sensorsValueMap;

    ////////////////////////
    /////// DECODING ///////
    ////////////////////////

    StateDecoder () {
        sensorsValueMap = new TreeMap<>();
        pathPointList = new ArrayList<>();
    }

    void setFullJSON(JSONObject fullJSON) {
        this.fullJSON = fullJSON;
    }

    ////////////////////////
    //// PUBLIC DECODE /////
    ////////////////////////

    void decodeState() {
        decodeSensors();
        decodeBasic();
        decodeAPS();
        decodeGPS();
        decodeAutonomy();
        decodePump();
    }

    void decodeAutonomyCurrentPath() {
        try {
            JSONObject boatAutonomy = fullJSON.getJSONObject("autonomy");
            decodePointList(boatAutonomy.getJSONArray("waypoints"));
        } catch (JSONException e) { e.printStackTrace(); }
    }

    ////////////////////////
    /// PRIVATE DECODE /////
    ////////////////////////

    private void decodeSensors() {
        try {
            JSONArray sensors = fullJSON.getJSONArray("sensors");
            for (int i=0; i<sensors.length(); i++) {
                JSONObject obj = sensors.getJSONObject(i);
                String name = obj.getString("name");
                String unit = obj.getString("unit");
                double value = obj.getDouble("value");

                SensorData readed = new SensorData();
                readed.name = name;
                readed.unit = unit;
                readed.value = value;

                sensorsValueMap.put(name, readed);
            }
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void decodeAPS () {
        try {
            JSONObject boatAPS = fullJSON.getJSONObject("APS");
            compassCalibration = boatAPS.getInt("mag_cal");
        } catch (JSONException e) { e.printStackTrace(); }

    }

    private void decodeGPS () {
        try {
            JSONObject boatPositionRead = fullJSON.getJSONObject("GPS");
            latitude = boatPositionRead.getDouble("lat");
            longitude = boatPositionRead.getDouble("lng");
            speedGPS = boatPositionRead.getDouble("speed");
            gpsFix = boatPositionRead.getInt("fix");
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void decodeAutonomy () {
        try {
            JSONObject boatAutonomy = fullJSON.getJSONObject("autonomy");
            autonomySpeed = boatAutonomy.getInt("speed");
            reachedPoint = boatAutonomy.getInt("reached_point");
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void decodeBasic() {
        try {
            heading = fullJSON.getDouble("heading");
            drivingMode = fullJSON.getInt("driving_mode");
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void decodePump() {
        try {
            JSONObject boatPositionRead = fullJSON.getJSONObject("pump");
            pumpOn = boatPositionRead.getBoolean("active");
            pumpSpeed = boatPositionRead.getInt("speed");
            pumpTime = boatPositionRead.getInt("time");
        } catch (JSONException e) { e.printStackTrace(); }
    }

    ////////////////////////
    /// SUPPORT METHODS ////
    ////////////////////////

    private void decodePointList(JSONArray jsonArrayPointList) {
        pathPointList.clear();
        for (int i = 0; i < jsonArrayPointList.length(); i++) {
            try {
                JSONObject point = jsonArrayPointList.getJSONObject(i);
                double latitude = point.getDouble("lat");
                double longitude = point.getDouble("lng");
                GeoPoint geoPoint = new GeoPoint(latitude, longitude);
                //Log.d("SocketTest", "b) " + geoPoint.toString());
                pathPointList.add(geoPoint);
            } catch (JSONException e) {  e.printStackTrace(); }
        }
    }

    ////////////////////////
    ///// GET METHODS //////
    ////////////////////////

    int getCompassCalibration() {
        return compassCalibration;
    }

    double getLatitude() {
        return latitude;
    }

    double getLongitude() {
        return longitude;
    }

    double getSpeedGPS() {
        return speedGPS;
    }

    int getAutonomySpeed() {
        return autonomySpeed;
    }

    int getReachedPoint() {
        return reachedPoint;
    }

    int getGpsFix() { return gpsFix; }

    double getHeading() {
        return heading;
    }

    int getDrivingMode() {
        return drivingMode;
    }

    TreeMap<String, SensorData> getSensorsValueMap() {
        return sensorsValueMap;
    }

    boolean getPumpOn() { return pumpOn; }

    int getPumpSpeed() { return pumpSpeed; }

    int getPumpTime() { return pumpTime; }

    ArrayList<GeoPoint> getPathPointList() { return pathPointList; }
}
