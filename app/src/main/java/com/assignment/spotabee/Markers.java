package com.assignment.spotabee;

import android.arch.persistence.room.Room;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import com.assignment.spotabee.database.AppDatabase;
import com.assignment.spotabee.database.Description;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Markers extends AppCompatActivity implements OnMapReadyCallback{
    private static final String TAG = "markers_debug";
    private AppDatabase db;
    private List<Double> latitudes;
    private List<Double> longitudes;
    private List<Description> descriptions;
    private HashMap<String, LatLng> coOrdinates;
    private boolean mapIsReady;
    private GoogleMap googleMap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        this.coOrdinates = new HashMap<>();
        mapIsReady = false;

        // Database build

        // fallBackToDestructiveMigration() will DROP all tables and recreate them
        // when the version number of the database is updated.
        // Necessary since migration has not been put in place.

        db = Room.databaseBuilder(
                getApplicationContext(),
                AppDatabase.class,
                "App Database"
        ).fallbackToDestructiveMigration().build();

        Log.d(TAG, "initialise starting");
        initialise();
        Log.d(TAG, "initialise finished");

    }

    // Retrieve latitude, longitudes and location titles from the database
    // to make a HashMap of titled co-ordinates from which to place on the map
public void initialise(){

    AsyncTask.execute(new Runnable() {
        @Override
        public void run() {
            descriptions = db.descriptionDao()
                    .getAllDescriptions();

            for (int i = 0; i < descriptions.size(); i++){
                Description currentDescription = descriptions.get(i);
                coOrdinates.put(currentDescription.getLocation(),
                        new LatLng(currentDescription.getLatitude(),
                                currentDescription.getLongitude()));
            }
        }
    });
    for(String key : coOrdinates.keySet()){
        Log.d(TAG, "IN CO-ORDINATES:" + coOrdinates.get(key).toString());
    }
}

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        mapIsReady = true;
        initialise();

        // Ensure that the layout size is ready for LatLng bounds to be applied
        readyMapLayout();
    }


public void setMarkers(int width, int height) {


    if(coOrdinates.isEmpty()){
            Log.d(TAG, "setMarkers: co-ordinates HashMap is empty.");
            return;
        }

        final ArrayList<LatLng> arrayListLatLang = new ArrayList<>();
        for (String title : coOrdinates.keySet()){
            googleMap.addMarker(new MarkerOptions()
                    .position(coOrdinates.get(title))
                    .title(title));

            arrayListLatLang.add(coOrdinates.get(title));
        }

        // Builder calculates the area of the map it needs to show on screen
    // to include all markers
        LatLngBounds.Builder bld = new LatLngBounds.Builder();
        for (int i = 0; i < arrayListLatLang.size(); i++) {
            LatLng ll = arrayListLatLang.get(i);
            bld.include(ll);
        }
        LatLngBounds bounds = bld.build();
    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, 70));
    }


    // Adapted from: https://stackoverflow.com/questions/7733813/how-can-you-tell-when-a-layout-has-been-drawn?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa

    // Global layout listener retrieves the map's width and height and passes that to setMarkers()
    // so that LatLng bounds can be created with map dimensions given.
    // Otherwise the activity crashes

    public void readyMapLayout(){
        final View mapFragment = findViewById(R.id.map);
        ViewTreeObserver vto = mapFragment.getViewTreeObserver();
        vto.addOnGlobalLayoutListener (new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mapFragment.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width  = mapFragment.getMeasuredWidth();
                int height = mapFragment.getMeasuredHeight();
                setMarkers(width, height);

            }
        });
    }
}
