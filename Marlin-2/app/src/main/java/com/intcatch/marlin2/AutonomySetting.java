package com.intcatch.marlin2;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;

class AutonomySetting {

    private int speed;
    private ArrayList<GeoPoint> points;

    AutonomySetting(int speed, ArrayList<GeoPoint> points) {
        this.speed = speed;
        this.points = points;
    }

    String toJSON() {
        JSONObject mainObject= new JSONObject();

        try {
            JSONArray listObject = new JSONArray();
            for (GeoPoint p : points) {
                JSONObject coordinate = new JSONObject();
                coordinate.put("lat", p.getLatitude());
                coordinate.put("lng", p.getLongitude());
                listObject.put(coordinate);
            }

            mainObject.put("speed", speed);
            mainObject.put("path", listObject);

            return  mainObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "";
        }
    }

}
