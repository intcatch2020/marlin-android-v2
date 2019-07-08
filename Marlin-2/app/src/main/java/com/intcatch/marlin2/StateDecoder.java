package com.intcatch.marlin2;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
    private double heading;
    private int drivingMode;

    // State sensors
    private TreeMap<String, SensorData> sensorsValueMap;

    ////////////////////////
    /////// DECODING ///////
    ////////////////////////

    StateDecoder () {
        sensorsValueMap = new TreeMap<>();
    }

    void setFullJSON(JSONObject fullJSON) {
        this.fullJSON = fullJSON;
    }

    void decode() {
        decodeSensors();
        decodeBasic();
        decodeAPS();
        decodeGPS();
        decodeAutonomy();
    }

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
            JSONObject boatPositionRead = fullJSON.getJSONObject("autonomy");
            autonomySpeed = boatPositionRead.getInt("speed");
            reachedPoint = boatPositionRead.getInt("reached_point");
        } catch (JSONException e) { e.printStackTrace(); }
    }

    private void decodeBasic() {
        try {
            heading = fullJSON.getDouble("heading");
            drivingMode = fullJSON.getInt("driving_mode");
        } catch (JSONException e) { e.printStackTrace(); }
    }

    ////////////////////////
    ///// GET METHODS //////
    ////////////////////////

    public int getCompassCalibration() {
        return compassCalibration;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getSpeedGPS() {
        return speedGPS;
    }

    public int getAutonomySpeed() {
        return autonomySpeed;
    }

    public int getReachedPoint() {
        return reachedPoint;
    }

    public int getGpsFix() { return gpsFix; }

    public double getHeading() {
        return heading;
    }

    public int getDrivingMode() {
        return drivingMode;
    }

    public TreeMap<String, SensorData> getSensorsValueMap() {
        return sensorsValueMap;
    }
}
