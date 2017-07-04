package eu.arrowhead.demo.chargingstation;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import eu.arrowhead.demo.chargingstation.fragments.ConfirmStopChargingDialog;
import eu.arrowhead.demo.chargingstation.fragments.NeedInternetDialog;
import eu.arrowhead.demo.chargingstation.fragments.ReadyToChargeFragment;
import eu.arrowhead.demo.chargingstation.fragments.TimePickerFragment;
import eu.arrowhead.demo.chargingstation.messages.OrchestrationResponse;
import eu.arrowhead.demo.chargingstation.messages.models.ArrowheadSystem;
import eu.arrowhead.demo.chargingstation.messages.models.ChargingStation;
import eu.arrowhead.demo.chargingstation.utility.Networking;
import eu.arrowhead.demo.chargingstation.utility.PermissionUtils;
import eu.arrowhead.demo.chargingstation.utility.Utility;

import static android.util.Log.i;
import static eu.arrowhead.demo.chargingstation.R.id.map;

public class ReservationActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        NeedInternetDialog.NeedInternetListener,
        TimePickerFragment.TimePickerListener,
        ReadyToChargeFragment.ReadyToChargeListener,
        ConfirmStopChargingDialog.ConfirmStopChargingListener{

    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    Marker selectedStation;
    Marker stationUsed;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private Button reserveCharging, readyToCharge;
    private SharedPreferences prefs;
    private static String ARROWHEAD_URL = "http://arrowhead.tmit.bme.hu:8084/orchestrator/orchestration";
    private static String PROVIDER_URL = null;
    private static Integer APP_STATUS;

    private List<ChargingStation> stationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation);

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(map);
        mapFrag.getMapAsync(this);

        reserveCharging = (Button) findViewById(R.id.reserve_charging_button);
        readyToCharge = (Button) findViewById(R.id.ready_to_charge_button);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.refresh_map_fab);

        prefs = this.getSharedPreferences("eu.arrowhead.demo.chargingstation", Context.MODE_PRIVATE);
        setAppStatus();

        PROVIDER_URL = prefs.getString("provider_url", null);
        if(Utility.isConnected(ReservationActivity.this)){
            if(PROVIDER_URL == null){
                getProviderURL();
            }
            else{
                getChargingStations();
            }
        }
        else{
            DialogFragment newFragment = new NeedInternetDialog();
            newFragment.show(getSupportFragmentManager(), NeedInternetDialog.TAG);
        }

        reserveCharging.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(Utility.isConnected(ReservationActivity.this)){
                    if(selectedStation == null){
                        Toast.makeText(ReservationActivity.this, R.string.select_charging_station, Toast.LENGTH_LONG).show();
                    }
                    else if(!selectedStation.isInfoWindowShown()){
                        Toast.makeText(ReservationActivity.this, R.string.select_charging_station, Toast.LENGTH_LONG).show();
                    }
                    else{
                        reserveChargingStation(selectedStation.getPosition());
                    }
                }
                else{
                    Toast.makeText(ReservationActivity.this, R.string.no_internet_warning, Toast.LENGTH_SHORT).show();
                }
            }
        });

        readyToCharge.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Utility.isConnected(ReservationActivity.this)){
                    if(APP_STATUS == 2){
                        DialogFragment newFragment = new TimePickerFragment();
                        newFragment.show(getSupportFragmentManager(), TimePickerFragment.TAG);
                    }
                    else{
                        Calendar c = Calendar.getInstance();
                        int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
                        int minute = c.get(Calendar.MINUTE);
                        int stopHour = prefs.getInt("stop_time_hour", 0);
                        int stopMinute = prefs.getInt("stop_time_minute", 0);

                        //shoow confirmation fragment if the user wants to finish charging early
                        if(hourOfDay < stopHour || (hourOfDay == stopHour && minute < stopMinute)){
                            DialogFragment newFragment = new ConfirmStopChargingDialog();
                            newFragment.show(getSupportFragmentManager(), ConfirmStopChargingDialog.TAG);
                        }
                        else{
                            stopCharging();
                        }
                    }
                }
                else{
                    Toast.makeText(ReservationActivity.this, R.string.no_internet_warning, Toast.LENGTH_LONG).show();
                }
            }
        });

        readyToCharge.setOnLongClickListener(new Button.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                stopCharging();
                prefs.edit().putInt("app_status", 1).apply();
                setAppStatus();
                return false;
            }
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Utility.isConnected(ReservationActivity.this)){
                    getChargingStations();
                }
                else{
                    Toast.makeText(ReservationActivity.this, R.string.no_internet_warning, Toast.LENGTH_SHORT).show();
                }
            }
        });

        fab.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                getProviderURL();
                return false;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                buildGoogleApiClient();
                mGoogleMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                        Manifest.permission.ACCESS_FINE_LOCATION, true);
            }
        } else {
            buildGoogleApiClient();
            mGoogleMap.setMyLocationEnabled(true);
        }

        mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener()
        {
            @Override
            public boolean onMarkerClick(Marker marker) {
                selectedStation = marker;
                return false;
            }
        });
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        //5mins
        mLocationRequest.setInterval(300000);
        mLocationRequest.setFastestInterval(300000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        //move map camera to current location
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
    }

    public void setAppStatus(){
        APP_STATUS = prefs.getInt("app_status", 1);
        if(APP_STATUS == 1){
            reserveCharging.setEnabled(true);
            readyToCharge.setEnabled(false);
            readyToCharge.setText(R.string.ready_to_charge_with_linebreak);
        }
        else if(APP_STATUS == 2){
            reserveCharging.setEnabled(false);
            readyToCharge.setEnabled(true);
        }
        else{
            reserveCharging.setEnabled(false);
            readyToCharge.setEnabled(true);
            readyToCharge.setText(R.string.finish_charging);
        }
    }

    public JSONObject compileSRF() throws JSONException {
        JSONObject requesterSystem = new JSONObject();
        requesterSystem.put("systemGroup", "charging_station_demo");
        requesterSystem.put("systemName", "consumer");
        requesterSystem.put("address", "192.168.56.20");
        requesterSystem.put("port", "8080");
        requesterSystem.put("authenticationInfo", "info2");

        JSONObject requestedService = new JSONObject();
        requestedService.put("serviceGroup", "chargingStationDemo");
        requestedService.put("serviceDefinition", "lookup");

        JSONArray interfaces = new JSONArray();
        interfaces.put("json");
        requestedService.put("interfaces", interfaces);

        JSONObject serviceRequestForm = new JSONObject();
        serviceRequestForm.put("requesterSystem", requesterSystem);
        serviceRequestForm.put("requestedService", requestedService);

        i("service request form", serviceRequestForm.toString());
        return serviceRequestForm;
    }

    public void getProviderURL(){
        JSONObject serviceRequestForm = null;
        try {
            serviceRequestForm = compileSRF();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST, ARROWHEAD_URL, serviceRequestForm,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("getProviderURL_response", response.toString());
                        OrchestrationResponse orchResponse = Utility.fromJsonObject(response.toString(), OrchestrationResponse.class);
                        ArrowheadSystem provider = orchResponse.getResponse().get(0).getProvider();
                        String base_url = "http://" + provider.getAddress() + ":" + provider.getPort();
                        PROVIDER_URL = base_url.concat(orchResponse.getResponse().get(0).getInstruction());
                        prefs.edit().putString("provider_url", PROVIDER_URL).apply();
                        getChargingStations();
                        Toast.makeText(ReservationActivity.this, R.string.got_backend_url, Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("getProviderURL_error", String.valueOf(error.networkResponse.statusCode));
                        Toast.makeText(ReservationActivity.this, error.networkResponse.toString(), Toast.LENGTH_LONG).show();
                    }
                }
        );
        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy((int) TimeUnit.SECONDS.toMillis(15),
                2,  // maxNumRetries = 0 means no retry
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Networking.getInstance(ReservationActivity.this).addToRequestQueue(jsObjRequest);
    }

    public void getChargingStations(){
        String requestURL = PROVIDER_URL.concat("/chargingstations");
        i("getChargStationsURL", requestURL);
        JsonArrayRequest jsArrayRequest = new JsonArrayRequest(requestURL,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        stationList = Utility.fromJsonArray(response.toString(), ChargingStation.class);

                        //remove all the old markers first
                        mGoogleMap.clear();
                        int i = 1;
                        for(ChargingStation station : stationList){
                            Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(station.getLatitude(), station.getLongitude()))
                                    .title("Name" + i)
                                    .snippet("Free charging slots: " + station.getFreeSlots() + "/" + station.getMaxSlots()));

                            if(station.getFreeSlots() == 0){
                                marker.setIcon((BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            }
                            i++;
                        }
                        Toast.makeText(ReservationActivity.this, R.string.stations_placed_on_map, Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if(error.networkResponse != null){
                            i("gtChargingStationsError", String.valueOf(error.networkResponse.statusCode));
                            Toast.makeText(ReservationActivity.this, error.networkResponse.toString(), Toast.LENGTH_LONG).show();
                        }
                        else{
                            Log.i("gtChargingStationsError", getString(R.string.no_backend_response));
                        }
                    }
                }
        );
        jsArrayRequest.setRetryPolicy(new DefaultRetryPolicy((int) TimeUnit.SECONDS.toMillis(15),
                2,  // maxNumRetries = 0 means no retry
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Networking.getInstance(ReservationActivity.this).addToRequestQueue(jsArrayRequest);
    }

    @Override
    public void onNeedInternetOKclick(DialogFragment dialog) {
        if(!Utility.isConnected(ReservationActivity.this)){
            DialogFragment newFragment = new NeedInternetDialog();
            newFragment.show(getSupportFragmentManager(), NeedInternetDialog.TAG);
        }
        else{
            if(PROVIDER_URL == null){
                getProviderURL();
            }
            else{
                getChargingStations();
            }
        }
    }

    public void reserveChargingStation(LatLng position) {
        stationUsed = selectedStation;
        String requestURL = PROVIDER_URL.concat("/reserve/" + String.valueOf(position.latitude) + "/" + String.valueOf(position.longitude));
        Log.i("reserve_url", requestURL);
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, requestURL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ChargingStation station = Utility.fromJsonObject(response.toString(), ChargingStation.class);
                        stationUsed.setSnippet("Free charging slots: " + station.getFreeSlots() + "/" + station.getMaxSlots());
                        stationUsed.setIcon((BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                        if(stationUsed.isInfoWindowShown()){
                            stationUsed.hideInfoWindow();
                            stationUsed.showInfoWindow();
                        }

                        prefs.edit().putInt("app_status", 2).apply();
                        setAppStatus();

                        Toast.makeText(ReservationActivity.this, R.string.station_reserved, Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        i("reserverStation_error", String.valueOf(error.networkResponse.statusCode));
                        if(error.networkResponse.statusCode == 409){
                            Toast.makeText(ReservationActivity.this, "Please pick a charging station with free slots.", Toast.LENGTH_LONG).show();
                        }
                        else{
                            Toast.makeText(ReservationActivity.this, error.networkResponse.toString(), Toast.LENGTH_LONG).show();
                            getItBackend();
                        }
                    }
                }
        );
        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy((int) TimeUnit.SECONDS.toMillis(15),
                2,  // maxNumRetries = 0 means no retry
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Networking.getInstance(ReservationActivity.this).addToRequestQueue(jsObjRequest);

        prefs.edit().putLong("selected_station_latitude", Double.doubleToRawLongBits(position.latitude)).apply();
        prefs.edit().putLong("selected_station_longitude", Double.doubleToRawLongBits(position.longitude)).apply();
    }

    @Override
    public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();
        int hourOfDayNow = c.get(Calendar.HOUR_OF_DAY);
        int minuteNow = c.get(Calendar.MINUTE);

        //Log.i("debug_timepicker", hourOfDay + ":" + minute + " / " + hourOfDayNow + ":" + minuteNow);

        if(hourOfDay < hourOfDayNow || (hourOfDay == hourOfDayNow && minute < minuteNow)){
            Toast.makeText(ReservationActivity.this, R.string.provide_later_time, Toast.LENGTH_LONG).show();
            DialogFragment newFragment = new TimePickerFragment();
            newFragment.show(getSupportFragmentManager(), TimePickerFragment.TAG);
        }
        else{
            DialogFragment newFragment = new ReadyToChargeFragment();
            newFragment.show(getSupportFragmentManager(), ReadyToChargeFragment.TAG);
            prefs.edit().putInt("stop_time_hour", hourOfDay).apply();
            prefs.edit().putInt("stop_time_minute", minute).apply();
        }
    }

    @Override
    public void onReadyToChargeOKclick(DialogFragment dialog) {
        EditText currentChargeText = (EditText) dialog.getDialog().findViewById(R.id.current_charge_edittext);
        EditText minTargetText = (EditText) dialog.getDialog().findViewById(R.id.min_charge_target_edittext);

        if(currentChargeText.getText().toString().isEmpty() || minTargetText.getText().toString().isEmpty()){
            Toast.makeText(ReservationActivity.this, R.string.empty_imput_field_warning, Toast.LENGTH_LONG).show();
            dialog.dismiss();
            dialog.show(getSupportFragmentManager(), ReadyToChargeFragment.TAG);
        }
        else{
            double currentCharge = Double.parseDouble(currentChargeText.getText().toString());
            double minTarget = Double.parseDouble(minTargetText.getText().toString());
            if(currentCharge > 100.0 || minTarget > 100.0){
                Toast.makeText(ReservationActivity.this, R.string.high_values_warning, Toast.LENGTH_LONG).show();
                dialog.dismiss();
                dialog.show(getSupportFragmentManager(), ReadyToChargeFragment.TAG);
            }
            else if(currentCharge > minTarget){
                Toast.makeText(ReservationActivity.this, R.string.min_charge_target_warning, Toast.LENGTH_LONG).show();
                dialog.dismiss();
                dialog.show(getSupportFragmentManager(), ReadyToChargeFragment.TAG);
            }
            else{
                String requestURL = PROVIDER_URL.concat("/start");
                StringRequest strRequest = new StringRequest(Request.Method.GET, requestURL,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                Toast.makeText(ReservationActivity.this, response, Toast.LENGTH_LONG).show();
                                prefs.edit().putInt("app_status", 3).apply();
                                setAppStatus();
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                i("readytocharge_error", String.valueOf(error.networkResponse.statusCode));
                                Toast.makeText(ReservationActivity.this, error.networkResponse.toString(), Toast.LENGTH_LONG).show();
                                getItBackend();
                            }
                        }
                );
                Networking.getInstance(ReservationActivity.this).addToRequestQueue(strRequest);
            }
        }
    }

    @Override
    public void onStopChargingPositiveClick(DialogFragment dialog) {
        stopCharging();
    }

    public void stopCharging(){
        double latitude = Double.longBitsToDouble(prefs.getLong("selected_station_latitude", 0));
        double longitude = Double.longBitsToDouble(prefs.getLong("selected_station_longitude", 0));
        String requestURL = PROVIDER_URL.concat("/freeup/" + String.valueOf(latitude) + "/" + String.valueOf(longitude));

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, requestURL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ChargingStation station = Utility.fromJsonObject(response.toString(), ChargingStation.class);
                        stationUsed.setSnippet("Free charging slots: " + station.getFreeSlots() + "/" + station.getMaxSlots());
                        if (station.getFreeSlots() != 0) {
                            stationUsed.setIcon((BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                        }
                        else{
                            stationUsed.setIcon((BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                        }
                        if(stationUsed.isInfoWindowShown()){
                            stationUsed.hideInfoWindow();
                            stationUsed.showInfoWindow();
                        }

                        prefs.edit().putInt("app_status", 1).apply();
                        setAppStatus();

                        Toast.makeText(ReservationActivity.this, R.string.finished_charging, Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        i("stopcharging_error", String.valueOf(error.networkResponse.statusCode));
                        Toast.makeText(ReservationActivity.this, error.networkResponse.toString(), Toast.LENGTH_LONG).show();
                        getItBackend();
                    }
                }
        );
        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy((int) TimeUnit.SECONDS.toMillis(15),
                2,  // maxNumRetries = 0 means no retry
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Networking.getInstance(ReservationActivity.this).addToRequestQueue(jsObjRequest);
    }

    public void getItBackend(){
        StringRequest strRequest = new StringRequest(Request.Method.GET, PROVIDER_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //backend should work
                        Log.i("getItBackend", response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        getProviderURL();
                    }
                }
        );
        strRequest.setRetryPolicy(new DefaultRetryPolicy((int) TimeUnit.SECONDS.toMillis(15),
                2,  // maxNumRetries = 0 means no retry
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        Networking.getInstance(ReservationActivity.this).addToRequestQueue(strRequest);
    }
}