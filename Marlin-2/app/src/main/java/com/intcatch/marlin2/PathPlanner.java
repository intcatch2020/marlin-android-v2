package com.intcatch.marlin2;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class PathPlanner {

    private ArrayList<GeoPoint> basicPointList;

    public void setPoints(ArrayList<GeoPoint> basicPointList){
        this.basicPointList = basicPointList;
    }

    public ArrayList<GeoPoint> getSpiralPath(int n){
        ArrayList<GeoPoint> pointList = basicPointList; // NOT A COPY!
        sortByX(pointList);

        ArrayList<GeoPoint> perimeterList = new ArrayList<>();
        perimeterList.add(pointList.get(0));
        double offset = 0;

        // Find the perimeter of the points
        for (int i = 0; i < pointList.size()-1; i++) {
            ArrayList<Double> angles = new ArrayList<>();
            for (GeoPoint point : pointList) {
                double angle = getAngle(perimeterList.get(perimeterList.size()-1), point, offset);
                angles.add(angle);
            }
            offset = Collections.min(angles);
            int minIndex = angles.indexOf(offset);
            if (perimeterList.contains(pointList.get(minIndex))) break;
            perimeterList.add(pointList.get(minIndex));
        }
        perimeterList = sortByIndex(perimeterList);

        // Spiral Generation phase

        // Distance from centroid for every point
        ArrayList<GeoPoint> spiralList = new ArrayList<>();
        GeoPoint centroid = getCentroid(perimeterList);

        ArrayList<Double> stepList = new ArrayList<>();
        ArrayList<Double> distanceList = new ArrayList<>();
        for (int i=0; i < perimeterList.size(); i++){
            double step = (getDistance(centroid, perimeterList.get(i)) / n);
            stepList.add(step);
            distanceList.add(step);
        }

        for (int i = 0; i < n-1; i++){
            for(int k=0; k < perimeterList.size(); k++){
                GeoPoint point = perimeterList.get(k);
                double angle = getAngle(point, centroid, 0);
                double newX = point.getLatitude() + distanceList.get(k) * Math.cos(Math.toRadians(angle));
                double newY = point.getLongitude() + distanceList.get(k) * Math.sin(Math.toRadians(angle));

                GeoPoint newPoint = new GeoPoint(newX, newY);
                newPoint.setLongitude(newY);

                spiralList.add(newPoint);
                distanceList.set(k, distanceList.get(k) + stepList.get(k));
            }
        }

        // Generate the marker list from the point list
        perimeterList.addAll(spiralList);


        return perimeterList;
    }

    private double getAngle(GeoPoint a, GeoPoint b, double offset){
        double angle = Math.toDegrees(Math.atan2(b.getLongitude()-a.getLongitude(), b.getLatitude()-a.getLatitude()));
        angle -= offset;
        if(angle < 0) angle += 360;
        if(angle == 0) angle = 360;
        return angle;
    }

    private double getDistance(GeoPoint a, GeoPoint b){
        return Math.sqrt(Math.pow(b.getLatitude() - a.getLatitude(), 2) + Math.pow(b.getLongitude() - a.getLongitude(),2));
    }

    private double getArea(ArrayList<GeoPoint> pointList){
        double area = 0;

        area += (pointList.get(pointList.size()-1).getLatitude() * pointList.get(0).getLongitude()) - (pointList.get(0).getLatitude() * pointList.get(pointList.size()-1).getLongitude());
        for (int i = 0; i < pointList.size()-1; i++)
            area += (pointList.get(i).getLatitude() * pointList.get(i+1).getLongitude()) - (pointList.get(i+1).getLatitude() * pointList.get(i).getLongitude());

        area *= 0.5;
        return area;
    }

    private GeoPoint getCentroid(ArrayList<GeoPoint> pointList){
        double area = getArea(pointList);

        double x = 0;
        double y = 0;

        x += ((pointList.get(pointList.size()-1).getLatitude() + pointList.get(0).getLatitude()) * (pointList.get(pointList.size()-1).getLatitude()*pointList.get(0).getLongitude()-pointList.get(0).getLatitude()*pointList.get(pointList.size()-1).getLongitude()));
        y += ((pointList.get(pointList.size()-1).getLongitude() + pointList.get(0).getLongitude()) * (pointList.get(pointList.size()-1).getLatitude()*pointList.get(0).getLongitude()-pointList.get(0).getLatitude()*pointList.get(pointList.size()-1).getLongitude()));

        for (int i = 0; i < pointList.size()-1; i++){
            x += ((pointList.get(i).getLatitude() + pointList.get(i+1).getLatitude()) * (pointList.get(i).getLatitude()*pointList.get(i+1).getLongitude()-pointList.get(i+1).getLatitude()*pointList.get(i).getLongitude()));
            y += ((pointList.get(i).getLongitude() + pointList.get(i+1).getLongitude()) * (pointList.get(i).getLatitude()*pointList.get(i+1).getLongitude()-pointList.get(i+1).getLatitude()*pointList.get(i).getLongitude()));
        }

        x *= (1/(6*area));
        y *= (1/(6*area));

        return new GeoPoint(x, y);
    }

    private void sortByX(ArrayList<GeoPoint> pointList){
        Collections.sort(pointList, new Comparator<GeoPoint>() {
            @Override
            public int compare(GeoPoint a, GeoPoint b)
            {
                if (a.getLongitude() > b.getLongitude()) return 1;
                else return -1;
            }
        });
    }

    private ArrayList<GeoPoint> sortByIndex(ArrayList<GeoPoint> perimeterList){
        ArrayList<GeoPoint> originalList = perimeterList;

        int startingIndex = 0;
        for (int i = 0; i < originalList.size(); i++){
            if(perimeterList.contains(originalList.get(i))) {
                startingIndex = perimeterList.indexOf(originalList.get(i));
                break;
            }
        }

        ArrayList<GeoPoint> toReturn = new ArrayList<>();
        for (int i = 0; i < perimeterList.size(); i++ ){
            toReturn.add(perimeterList.get(startingIndex));
            startingIndex ++;
            if(startingIndex > perimeterList.size() - 1)
                startingIndex = 0;
        }

        return  toReturn;
    }

}
