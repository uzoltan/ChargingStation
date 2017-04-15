package eu.arrowhead.demo.chargingstation;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

import eu.arrowhead.demo.chargingstation.fragments.NeedInternetDialog;
import eu.arrowhead.demo.chargingstation.messages.OrchestrationResponse;
import eu.arrowhead.demo.chargingstation.messages.models.ArrowheadSystem;
import eu.arrowhead.demo.chargingstation.messages.models.ChargingStation;
import eu.arrowhead.demo.chargingstation.utility.Networking;
import eu.arrowhead.demo.chargingstation.utility.PermissionUtils;
import eu.arrowhead.demo.chargingstation.utility.Utility;

public class ReservationActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        NeedInternetDialog.NeedInternetListener{

    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private Button reserveCharging, readyToCharge;
    private SharedPreferences prefs;
    private static String ARROWHEAD_URL = "http://arrowhead.tmit.bme.hu:8084/orchestrator/orchestration";
    private static String PROVIDER_URL = null;

    private List<ChargingStation> stationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation);

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);

        reserveCharging = (Button) findViewById(R.id.reserve_charging_button);
        readyToCharge = (Button) findViewById(R.id.ready_to_charge_button);

        prefs = this.getSharedPreferences("eu.arrowhead.demo.chargingstation", Context.MODE_PRIVATE);
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

        Log.i("service request form", serviceRequestForm.toString());
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
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("getProviderURL_error", error.toString());
                        Toast.makeText(ReservationActivity.this, "Network error: " + error.getMessage(), Toast.LENGTH_LONG).show();
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
        Log.i("getChargStationsURL", requestURL);
        JsonArrayRequest jsArrayRequest = new JsonArrayRequest(requestURL,
                new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray response) {
                        stationList = Utility.fromJsonArray(response.toString(), ChargingStation.class);
                        Log.i("marker placement", "eljutott a for elé");
                        for(ChargingStation station : stationList){
                            Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(station.getLatitude(), station.getLongitude()))
                                    .title("Name of the charging station")
                                    .snippet("Free charging slots: " + station.getFreeSlots() + "/" + station.getMaxSlots()));

                            if(station.getFreeSlots() == 0){
                                marker.setIcon((BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                            }
                        }
                        Log.i("marker placement", "eljutott a for után");
                    }
                },
                new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.i("gtChargingStationsError", error.toString());
                        Toast.makeText(ReservationActivity.this, "Network error: " + error.getMessage(), Toast.LENGTH_LONG).show();
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
}