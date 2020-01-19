package net.simplifiedcoding.firebasephoneauth;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    MapView mapview;
    TextView tv_current_location;

    private double longitude;
    private double latitude;

    GoogleMap mGoogleMap;

    FloatingActionButton fab_main;

    private GoogleApiClient googleApiClient;

    JSONObject jsonObject = new JSONObject();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        findViewById(R.id.buttonLogout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();

                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent);
            }
        });

        mapview = findViewById(R.id.mapview);
        tv_current_location = findViewById(R.id.tv_current_location);
        mapview.onCreate(savedInstanceState);
        mapview.getMapAsync(this);

        fab_main = findViewById(R.id.fab_main);

        create_jsonLatLong();


        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        fab_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                if (mLastLocation != null) {
                    latitude = mLastLocation.getLatitude();
                    longitude = mLastLocation.getLongitude();


                    LatLng latLng = new LatLng(latitude, longitude);
                    CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(15).build();
                    mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

                    try {
                        new GetPickUpLocationAsyncFromMap(latitude, longitude).execute();
                    } catch (Exception e) {
                    }
                }
            }
        });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (location != null) {

            latitude = location.getLatitude();
            longitude = location.getLongitude();


            new GetPickUpLocationAsyncFromMap(latitude,longitude).execute();

            //moving the map to location
            moveMap();
        }
    }

    private void moveMap() {
        LatLng latLng = new LatLng(latitude, longitude);
        //Moving the camera
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        //Animating the camera
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        MapsInitializer.initialize(this);
        switch (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)) {
            case ConnectionResult.SUCCESS:
                mGoogleMap.setMyLocationEnabled(true);
                mGoogleMap.setBuildingsEnabled(true);
                mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                mGoogleMap.setTrafficEnabled(true);
                mGoogleMap.setIndoorEnabled(true);

                mGoogleMap.getUiSettings().setZoomControlsEnabled(false);
                mGoogleMap.getUiSettings().setZoomGesturesEnabled(true);
                mGoogleMap.getUiSettings().setRotateGesturesEnabled(true);

                mGoogleMap.getUiSettings().setCompassEnabled(true);
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
                mGoogleMap.getUiSettings().setMapToolbarEnabled(false);


                break;
            case ConnectionResult.SERVICE_MISSING:
                Toast.makeText(this, "SERVICE MISSING", Toast.LENGTH_SHORT).show();
                break;
            case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
                Toast.makeText(this, "UPDATE REQUIRED", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(this, GooglePlayServicesUtil.isGooglePlayServicesAvailable(this), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapview.onPause();
    }

    @Override
    protected void onStart() {
        googleApiClient.connect();

        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapview.onLowMemory();
    }

    private class GetPickUpLocationAsyncFromMap extends AsyncTask<String, Void, String> {

        double x, y;
        StringBuilder stringBuilder;
        Geocoder geocoder;
        List<Address> addresses;

        public GetPickUpLocationAsyncFromMap(double latitude, double longitude) {
            x = latitude;
            y = longitude;
        }

        @Override
        protected void onPreExecute() {

            tv_current_location.setText("Getting Location...");

        }

        @Override
        protected String doInBackground(String... params) {
            stringBuilder = new StringBuilder();
            try {
                geocoder = new Geocoder(ProfileActivity.this, Locale.ENGLISH);
                addresses = geocoder.getFromLocation(x, y, 1);
                if (geocoder.isPresent()) {
                    Address returnAddress = addresses.get(0);
                    String addressLine = returnAddress.getAddressLine(0);
                    String addressLine1 = returnAddress.getAddressLine(1);
                    String addressLine2 = returnAddress.getAddressLine(2);
                    stringBuilder
                            .append(addressLine + ", " + addressLine1 + ", " + addressLine2);
                } else {
                }
            } catch (Exception e) {
            }
            return stringBuilder.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                tv_current_location.setText(result);
            } catch (Exception e) {
            }
        }
    }

    void create_jsonLatLong(){
        try {
            JSONArray jsonArray = new JSONArray();

            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("latitude", "28.4591");
            jsonObject1.put("longitude", "77.0726");

            JSONObject jsonObject2 = new JSONObject();
            jsonObject2.put("latitude", "28.4135");
            jsonObject2.put("longitude", "77.0415");

            JSONObject jsonObject3 = new JSONObject();
            jsonObject3.put("latitude", "28.4426");
            jsonObject3.put("longitude", "77.0571");

            jsonArray.put(jsonObject1);
            jsonArray.put(jsonObject2);
            jsonArray.put(jsonObject3);

            jsonObject.put("data",jsonArray);
        }
        catch (Exception e){

        }
    }
}