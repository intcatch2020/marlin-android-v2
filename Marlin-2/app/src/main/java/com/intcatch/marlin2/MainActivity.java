package com.intcatch.marlin2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;

import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.LoginFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.socketio.client.IO;

import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.views.overlay.mylocation.SimpleLocationOverlay;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.emitter.Emitter;

import javax.net.ssl.SSLContext;

public class MainActivity extends AppCompatActivity {

    MapView mapView;
    MapController mapController;
    MyLocationNewOverlay mLocationOverlay;

    Marker boatMarker;
    Marker homeMarker;

    // JSON state decoder
    StateDecoder decoder = new StateDecoder();

    // View Object
    private FloatingActionButton drawButton;
    private FloatingActionButton spiralButton;
    private FloatingActionButton resetButton;
    private FloatingActionButton sendButton;
    private FloatingActionButton speedButton;
    private FloatingActionButton searchButton;
    private FloatingActionButton peristalticButton;
    private FloatingActionButton plusSpiralButton;
    private FloatingActionButton minusSpiralButton;
    private FloatingActionButton goHomeButton; //same button, set-home and go-home
    private FloatingActionButton resetHomeButton;
    private TextView miniLogView;
    private TextView pumpLogView;
    private ArrayList<TextView> sensorsTextViewList;

    // Points List
    private ArrayList<Marker> markerArrayList;
    private ArrayList<Polyline> polyLineArrayList;
    private ArrayList<GeoPoint> geoPointsArrayList;

    // Socket variables
    public static final String SERVER_URL = "http://192.168.2.1:5000";
    private Socket mSocket;

    // My private variables
    private boolean runningPathFlag; //if true, try to resume path
    private boolean goHomePathFlag; //if true, try to resume path
    private boolean homeSetted; //home point setted for go-home
    private int spiralPathSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        runningPathFlag = true;
        goHomePathFlag = true;
        homeSetted = false;
        spiralPathSize = 3;

        setUpConnection();
        setUpPermissions();
        setUpViews();
        setUpTextViews();
        setUpMap();
        setUpList();

        addClickListenerOverlay();
        addButtonsListener();

        setUpEnableButtons();

        drawBoat();
        drawHome();
    }

    ////////////////////////
    //// INITIALIZATION ////
    ////////////////////////

    private void setUpConnection() {
        try {
            mSocket = IO.socket(SERVER_URL);
        } catch (URISyntaxException e) { throw new RuntimeException(e); }

        mSocket.on("state", onStateUpdate);
        mSocket.on("disconnect", onDisconnect);
        mSocket.on("connect", onConnect);
        mSocket.connect();
    }

    private void setUpPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @SuppressLint("RestrictedApi")
    private void setUpViews() {
        // Find views
        drawButton = findViewById(R.id.button_draw);
        resetButton = findViewById(R.id.button_reset);
        sendButton = findViewById(R.id.button_send);
        spiralButton = findViewById(R.id.button_spiral);
        speedButton = findViewById(R.id.button_speed);
        searchButton = findViewById(R.id.button_search);
        miniLogView = findViewById(R.id.textView_miniLog);
        pumpLogView = findViewById(R.id.textView_pumpLog);
        peristalticButton = findViewById(R.id.button_peristaltic);
        goHomeButton = findViewById(R.id.button_go_home);
        resetHomeButton = findViewById(R.id.button_reset_home);
        plusSpiralButton = findViewById(R.id.button_plus_spiral);
        minusSpiralButton = findViewById(R.id.button_minus_spiral);
    }

    @SuppressLint("RestrictedApi")
    private void setUpEnableButtons() {
        // Set invisible views
        miniLogView.setVisibility(View.GONE);
        pumpLogView.setVisibility(View.GONE);
        plusSpiralButton.setVisibility(View.GONE);
        minusSpiralButton.setVisibility(View.GONE);

        enableSendButton(false);

        // Disable WIP buttons
        //goHomeButton.setClickable(false);
        //goHomeButton.setActivated(false);
        //goHomeButton.setAlpha(0.3f);
        //resetHomeButton.setClickable(false);
        //resetHomeButton.setActivated(false);
        //resetHomeButton.setAlpha(0.3f);

        // Disable unnecessary buttons
        peristalticButton.setClickable(false);
        peristalticButton.setActivated(false);
        peristalticButton.setAlpha(0.3f);
    }

    private void setUpMap() {
        mapView = findViewById(R.id.map);
        mapView.getTileProvider().getTileCache().getProtectedTileComputers().clear();
        mapView.getTileProvider().getTileCache().setAutoEnsureCapacity(false);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapController = (MapController) mapView.getController();
        mapController.setZoom(18.0);

        // Enable zoom with touch
        mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        mapView.setMultiTouchControls(true);

        // Add my location overlay
        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        mLocationOverlay.enableFollowLocation();
        mLocationOverlay.enableMyLocation();
        mLocationOverlay.setDrawAccuracyEnabled(false);
        Bitmap bInput = BitmapFactory.decodeResource(getResources(), R.drawable.ic_man);
        mLocationOverlay.setPersonIcon(bInput);
        mapView.getOverlays().add(mLocationOverlay);
    }

    private void setUpList() {
        markerArrayList = new ArrayList<>();
        polyLineArrayList = new ArrayList<>();
        geoPointsArrayList = new ArrayList<>();
    }

    @SuppressLint("SetTextI18n")
    private void setUpTextViews() {
        sensorsTextViewList = new ArrayList<>();
        sensorsTextViewList.add((TextView)findViewById(R.id.textView_1));
        sensorsTextViewList.add((TextView)findViewById(R.id.textView_2));
        sensorsTextViewList.add((TextView)findViewById(R.id.textView_3));
        sensorsTextViewList.add((TextView)findViewById(R.id.textView_4));
        sensorsTextViewList.add((TextView)findViewById(R.id.textView_5));
        sensorsTextViewList.add((TextView)findViewById(R.id.textView_6));
        sensorsTextViewList.add((TextView)findViewById(R.id.textView_7));
        sensorsTextViewList.add((TextView)findViewById(R.id.textView_8));
        sensorsTextViewList.add((TextView)findViewById(R.id.textView_9));
        sensorsTextViewList.add((TextView)findViewById(R.id.textView_10));
        sensorsTextViewList.add((TextView)findViewById(R.id.textView_11));
        sensorsTextViewList.add((TextView)findViewById(R.id.textView_12));
        sensorsTextViewList.add((TextView)findViewById(R.id.textView_13));

        for (TextView tV : sensorsTextViewList ) {
            tV.setVisibility(View.GONE);
        }

        sensorsTextViewList.get(0).setVisibility(View.VISIBLE);
        sensorsTextViewList.get(0).setText("Connection \nFailed");
        sensorsTextViewList.get(0).setTextColor(Color.RED);
    }

    private void resumeAutonomyState(boolean resumeGoHome) {
        decoder.decodeAutonomyCurrentPath();

        if (resumeGoHome) goHomePathFlag = false;
        else runningPathFlag = false;

        ArrayList<GeoPoint> geoPoints = decoder.getPathPointList();

        //Log.d("SocketTest", "a) " + geoPoints.size());

        if (resumeGoHome && geoPoints.size() > 0) {
            // Go home called, reset lines and set go home
            for (Marker m : markerArrayList) mapView.getOverlayManager().remove(m);
            markerArrayList.clear();
            for (Polyline p : polyLineArrayList) mapView.getOverlayManager().remove(p);
            polyLineArrayList.clear();

            geoPointsArrayList.clear();

            mapView.invalidate();

            for (GeoPoint p : geoPoints)  addMarker(p);
            drawLinesStandard(Color.RED);
            for (Marker m : markerArrayList) mapView.getOverlayManager().remove(m);
            markerArrayList.clear();
            Log.d("Resume-Path", "Try to resume...");

            return;
        }

        if(geoPoints.size() > 0) {

            enableSendButton(false);

            for (GeoPoint p : geoPoints) {
                addMarker(p);
            }

            drawLinesStandard(Color.BLACK);
        }
    }

    ////////////////////////
    ///// BASIC ACTION /////
    ////////////////////////

    @SuppressLint("RestrictedApi")
    private void sendPath() {
        if (!mSocket.connected()) {
            Toast.makeText(getApplicationContext(), "No boat connection", Toast.LENGTH_LONG).show();
            return;
        }

        AutonomySetting as = new AutonomySetting(30, geoPointsArrayList);
        mSocket.emit("start_autonomy", as.toJSON());
        Toast.makeText(getApplicationContext(), "Sending Path...", Toast.LENGTH_LONG).show();

        enableSendButton(false);
        plusSpiralButton.setVisibility(View.GONE);
        minusSpiralButton.setVisibility(View.GONE);

        drawButton.setClickable(false);
        drawButton.setAlpha(0.3f);
        spiralButton.setClickable(false);
        spiralButton.setAlpha(0.3f);
    }

    private void changeSpeed() {
        if(!mSocket.connected()) {
            Toast.makeText(getApplicationContext(), "No boat connection", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            // TODO: se connesso al server ma non alla barca legge sempre zero come autonomy speed
            DialogSpeed dialogSpeed = new DialogSpeed();
            DialogSpeed.selectedSpeed = decoder.getAutonomySpeed();
            DialogSpeed.mSocket = mSocket;
            dialogSpeed.show(getSupportFragmentManager(), "DialogSpeed");
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Wait for boat state... ", Toast.LENGTH_LONG).show();
            throw new RuntimeException(e);
        }

    }

    private void centerOnBoat() {
        if(!mSocket.connected()) {
            Toast.makeText(getApplicationContext(), "No boat connection! Center on phone", Toast.LENGTH_LONG).show();
            mapController.setZoom(18.0);
            mapController.setCenter(mLocationOverlay.getMyLocation());
        }
        else if (decoder.getGpsFix() == 0) {
            Toast.makeText(getApplicationContext(), "No boat GPS signal! Center on phone", Toast.LENGTH_LONG).show();
            mapController.setZoom(18.0);
            mapController.setCenter(mLocationOverlay.getMyLocation());
        }
        else {
            mapController.setZoom(18.0);
            mapController.setCenter(new GeoPoint(decoder.getLatitude(), decoder.getLongitude()));
        }
    }

    private void peristalticPanel() {
        if(!mSocket.connected()) {
            Toast.makeText(getApplicationContext(), "No boat connection", Toast.LENGTH_LONG).show();
            return;
        }

        DialogPeristaltic dialogPeristaltic = new DialogPeristaltic();
        DialogPeristaltic.mSocket = mSocket;
        dialogPeristaltic.show(getSupportFragmentManager(), "DialogPeristaltic");
    }

    private void addMarker(GeoPoint point) {
        Marker newMarker = new Marker(mapView);
        newMarker.setPosition(point);
        newMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        newMarker.setIcon(getResources().getDrawable(R.drawable.ic_place_black_32dp));
        newMarker.setTitle("Start point");

        mapView.getOverlays().add(newMarker);
        mapView.invalidate();

        markerArrayList.add(newMarker);
    }

    private void drawLinesStandard(int lineColor) {
        if(markerArrayList.size() < 2) {
            Toast.makeText(getApplicationContext(), "2 points needed", Toast.LENGTH_LONG).show();
            return;
        }

        for (int i = 0; i < markerArrayList.size(); i++) {
            GeoPoint point = new GeoPoint(markerArrayList.get(i).getPosition());
            geoPointsArrayList.add(point);
        }
        drawLineFromPoints(lineColor);

        enableSendButton(true);
    }

    @SuppressLint("RestrictedApi")
    private void drawLinesSpiral() {
        if(markerArrayList.size() < 3) {
            Toast.makeText(getApplicationContext(), "3 points needed", Toast.LENGTH_LONG).show();
            return;
        }

        for (int i = 0; i < markerArrayList.size(); i++) {
            GeoPoint point = new GeoPoint(markerArrayList.get(i).getPosition());
            geoPointsArrayList.add(point);
        }
        PathPlanner planner = new PathPlanner();
        planner.setPoints(geoPointsArrayList);
        geoPointsArrayList = planner.getSpiralPath(spiralPathSize);
        drawLineFromPoints(Color.BLACK);

        enableSendButton(true);
        plusSpiralButton.setVisibility(View.VISIBLE);
        minusSpiralButton.setVisibility(View.VISIBLE);
    }

    private void drawLineFromPoints(int lineColor) {
        for (int i = 0; i < geoPointsArrayList.size() - 1; i++) {
            List<GeoPoint> geoPoints = new ArrayList<>();
            GeoPoint pointA = geoPointsArrayList.get(i);
            GeoPoint pointB = geoPointsArrayList.get(i+1);
            geoPoints.add(pointA);
            geoPoints.add(pointB);

            Polyline line = new Polyline();
            line.setPoints(geoPoints);
            line.setColor(lineColor);

            polyLineArrayList.add(line);
            mapView.getOverlayManager().add(line);
        }

        mapView.invalidate();
    }

    @SuppressLint("RestrictedApi")
    private void resetMarkerLines() {
        mSocket.emit("stop_autonomy");

        if(polyLineArrayList.size() > 0) {
            for (Polyline p : polyLineArrayList) mapView.getOverlayManager().remove(p);
            polyLineArrayList.clear();

            plusSpiralButton.setVisibility(View.GONE);
            minusSpiralButton.setVisibility(View.GONE);

            geoPointsArrayList.clear();
        }
        else {
            for (Marker m : markerArrayList) mapView.getOverlayManager().remove(m);
            markerArrayList.clear();
            geoPointsArrayList.clear();
        }

        mapView.invalidate();
        enableSendButton(false);
    }

    private void setReachedPoints() {
        int n = decoder.getReachedPoint();
        if(n >= polyLineArrayList.size()) return; // safe return, hopefully unreachable
        if(polyLineArrayList.get(0).getColor() == Color.RED) return; // not used in go home
        for (int i = 1; i < n; i++) polyLineArrayList.get(i-1).setColor(R.color.alphaBlack);
        mapView.invalidate();
    }

    private void enableSendButton(boolean flag) {
        if(flag) {
            sendButton.setClickable(true);
            sendButton.setAlpha(1.0f);
            drawButton.setClickable(false);
            drawButton.setAlpha(0.3f);
            spiralButton.setClickable(false);
            spiralButton.setAlpha(0.3f);
        } else {
            sendButton.setClickable(false);
            sendButton.setAlpha(0.3f);
            drawButton.setClickable(true);
            drawButton.setAlpha(1.0f);
            spiralButton.setClickable(true);
            spiralButton.setAlpha(1.0f);
        }
    }

    private void plusSpiral() {
        spiralPathSize += 1;

        for (Polyline p : polyLineArrayList) mapView.getOverlayManager().remove(p);
        polyLineArrayList.clear();
        geoPointsArrayList.clear();
        mapView.invalidate();

        for (int i = 0; i < markerArrayList.size(); i++) {
            GeoPoint point = new GeoPoint(markerArrayList.get(i).getPosition());
            geoPointsArrayList.add(point);
        }
        PathPlanner planner = new PathPlanner();
        planner.setPoints(geoPointsArrayList);
        geoPointsArrayList = planner.getSpiralPath(spiralPathSize);
        drawLineFromPoints(Color.BLACK);
    }

    private void minusSpiral() {
        if(spiralPathSize > 1) spiralPathSize -= 1;

        for (Polyline p : polyLineArrayList) mapView.getOverlayManager().remove(p);
        polyLineArrayList.clear();
        geoPointsArrayList.clear();
        mapView.invalidate();

        for (int i = 0; i < markerArrayList.size(); i++) {
            GeoPoint point = new GeoPoint(markerArrayList.get(i).getPosition());
            geoPointsArrayList.add(point);
        }
        PathPlanner planner = new PathPlanner();
        planner.setPoints(geoPointsArrayList);
        geoPointsArrayList = planner.getSpiralPath(spiralPathSize);
        drawLineFromPoints(Color.BLACK);
    }

    private void callGoHome() {
        if(!mSocket.connected()) {
            Toast.makeText(getApplicationContext(), "No boat connection", Toast.LENGTH_LONG).show();
            return;
        }

        if(!homeSetted) {
            if (decoder.getGpsFix() == 0) {
                Toast.makeText(getApplicationContext(), "No boat GPS signal! Can't set Home", Toast.LENGTH_LONG).show();
            } else {
                GeoPoint homePosition = new GeoPoint(decoder.getLatitude(), decoder.getLongitude());
                moveHome(false);

                JSONObject homeJSON = new JSONObject();
                try {
                    homeJSON.put("lat", homePosition.getLatitude());
                    homeJSON.put("lng", homePosition.getLongitude());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mSocket.emit("set_home", homeJSON.toString());

                homeSetted = true;

                goHomeButton.setImageResource(R.drawable.ic_arrow_revert);
            }
        } else {
            mSocket.emit("go_home");

            // Draw go-home path
            goHomePathFlag = true; //try to resume path
        }
    }

    private void resetHome() {
        if(!mSocket.connected()) {
            Toast.makeText(getApplicationContext(), "No boat connection", Toast.LENGTH_LONG).show();
            return;
        }

        // RESET go-home stuff
        moveHome(true);
        goHomeButton.setImageResource(R.drawable.ic_go_home_set);
        homeSetted = false;
    }

    ////////////////////////
    //// GRAPHIC UPDATE ////
    ////////////////////////

    @SuppressLint("DefaultLocale")
    private void updateSensorViews() {
        if (!mSocket.connected()) {
            for (TextView tV : sensorsTextViewList ) {
                tV.setVisibility(View.GONE);
            }
            sensorsTextViewList.get(0).setVisibility(View.VISIBLE);
            sensorsTextViewList.get(0).setText("Connection \nFailed");
            sensorsTextViewList.get(0).setTextColor(Color.RED);
        } else {
            int i = 1;
            for (Map.Entry<String, SensorData> entry : decoder.getSensorsValueMap().entrySet()) {
                String key = entry.getKey();
                double value = entry.getValue().value;
                String unit = entry.getValue().unit;

                sensorsTextViewList.get(i).setText(String.format("%s\n%.2f\n%s", key, value, unit));
                sensorsTextViewList.get(i).setVisibility(View.VISIBLE);
                i++;
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private void updateMiniLog() {
        if (mSocket.connected()) {
            double speed = decoder.getSpeedGPS();
            int calibration = decoder.getCompassCalibration();
            int mode = decoder.getDrivingMode();

            String modeStr;
            switch (mode) {
                case 0:
                    modeStr = "RC";
                    break;
                case 1:
                    modeStr = "Autonomy";
                    break;
                case 2:
                    modeStr = "Go Home";
                    break;
                default:
                    modeStr = "IDLE";
            }

            String miniLog = "";
            miniLog += String.format("IP: %s \n", SERVER_URL);
            miniLog += String.format("Speed: %s \n", speed);
            miniLog += String.format("Compass Calibration: %d/3 \n", calibration);
            miniLog += String.format("Driving Mode: %s", modeStr);

            miniLogView.setText(miniLog);
            miniLogView.setTextColor(Color.rgb(0, 0, 0));

            miniLogView.setVisibility(View.VISIBLE);
        } else {
            miniLogView.setVisibility(View.GONE);
        }
    }

    @SuppressLint("DefaultLocale")
    private void updatePumpLog() {
        if (mSocket.connected() && decoder.getPumpOn()) {

            int pumpSpeed = decoder.getPumpSpeed();
            int pumpTime = decoder.getPumpTime();

            String pumpLog = "";
            pumpLog += ("Pump ON\n");
            pumpLog += String.format("Pump Speed: %d \n", pumpSpeed);

            int pumpTimeMinutes = (pumpTime % 3600) / 60;
            int pumpTimeSeconds = pumpTime % 60;

            pumpLog += String.format("Remaining time: %02dm%02ds", pumpTimeMinutes, pumpTimeSeconds);

            pumpLogView.setText(pumpLog);
            pumpLogView.setTextColor(Color.rgb(0, 0, 0));

            pumpLogView.setVisibility(View.VISIBLE);
        } else {
            pumpLogView.setVisibility(View.GONE);
        }
    }

    private void drawHome() {
        GeoPoint homePosition = new GeoPoint(0.0, 0.0);

        homeMarker = new Marker(mapView);
        homeMarker.setPosition(homePosition);
        homeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        homeMarker.setIcon(getResources().getDrawable(R.drawable.ic_home));
        homeMarker.setTitle("Home");

        mapView.getOverlays().add(homeMarker);
        mapView.invalidate();
    }

    private void moveHome(boolean reset){
        double boatLat = decoder.getLatitude();
        double boatLng = decoder.getLongitude();

        GeoPoint homePosition = new GeoPoint(0.0, 0.0);
        if (!reset) homePosition = new GeoPoint(boatLat, boatLng);

        Bitmap bInput = BitmapFactory.decodeResource(getResources(), R.drawable.ic_home);
        Matrix matrix = new Matrix();
        Bitmap bOutput = Bitmap.createBitmap(bInput, 0, 0, bInput.getWidth(), bInput.getHeight(), matrix, true);
        Drawable oIcon = new BitmapDrawable(getResources(), bOutput);

        homeMarker.setPosition(homePosition);
        homeMarker.setIcon(oIcon);
        mapView.invalidate();
    }

    private void drawBoat() {
        GeoPoint boatMarkerPos = new GeoPoint(0.0, 0.0);

        boatMarker = new Marker(mapView);
        boatMarker.setPosition(boatMarkerPos);
        boatMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        boatMarker.setIcon(getResources().getDrawable(R.drawable.arrow_white));
        boatMarker.setTitle("Boat");

        mapView.getOverlays().add(boatMarker);
        mapView.invalidate();
    }

    private void moveBoat() {
        double boatLat = decoder.getLatitude();
        double boatLng = decoder.getLongitude();
        double boatHeading = decoder.getHeading();

        GeoPoint boatMarkerPos = new GeoPoint(boatLat, boatLng);

        Bitmap bInput = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_white);
        Matrix matrix = new Matrix();
        matrix.setRotate((float) boatHeading);
        Bitmap bOutput = Bitmap.createBitmap(bInput, 0, 0, bInput.getWidth(), bInput.getHeight(), matrix, true);
        Drawable rotatedIcon = new BitmapDrawable(getResources(), bOutput);

        boatMarker.setPosition(boatMarkerPos);
        boatMarker.setIcon(rotatedIcon);
        mapView.invalidate();
    }

    ////////////////////////
    /////// LISTENER ///////
    ////////////////////////

    private void addClickListenerOverlay() {
        MapEventsReceiver mReceive = new MapEventsReceiver() {

            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                GeoPoint newPoint = new GeoPoint(p.getLatitude(), p.getLongitude());
                addMarker(newPoint);
                return false;
            }
        };

        mapView.getOverlays().add(new MapEventsOverlay(mReceive));
    }

    private void addButtonsListener() {
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPath();
            }
        });
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetMarkerLines ();
            }
        });
        drawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawLinesStandard(Color.BLACK);
            }
        });
        spiralButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawLinesSpiral();
            }
        });
        speedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSpeed();
            }
        });
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                centerOnBoat();
            }
        });
        peristalticButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                peristalticPanel();
            }
        });
        plusSpiralButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                plusSpiral();
            }
        });
        minusSpiralButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                minusSpiral();
            }
        });
        goHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callGoHome();
            }
        });
        resetHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetHome();
            }
        });
    }

    ////////////////////////
    /// ANDROID STANDARD ///
    ////////////////////////

    public void onResume(){
        super.onResume();
        mapView.onResume();
    }

    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override public void onDestroy () {
        super.onDestroy();

        mSocket.disconnect();
        mSocket.off("state", onStateUpdate);
    }

    ////////////////////////
    /// SOCKET LISTENER ////
    ////////////////////////

    private Emitter.Listener onStateUpdate = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    decoder.setFullJSON((JSONObject) args[0]);
                    decoder.decodeState();
                    updateSensorViews();
                    updateMiniLog();
                    updatePumpLog();
                    moveBoat();
                    if(goHomePathFlag && decoder.getDrivingMode() == 2) { resumeAutonomyState(true); }
                    if(runningPathFlag) { resumeAutonomyState(false); }
                    setReachedPoints();

                }
            });
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    sensorsTextViewList.get(0).setText("Connection \nFailed");
                    sensorsTextViewList.get(0).setTextColor(Color.RED);
                    updateSensorViews();
                    updateMiniLog();
                    updatePumpLog();
                }
            });
        }
    };

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    sensorsTextViewList.get(0).setText("Connection \nOk");
                    sensorsTextViewList.get(0).setTextColor(Color.GREEN);
                }
            });
        }
    };

}
