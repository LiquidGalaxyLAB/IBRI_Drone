package es.moiseslodeiro.ibri;

/**
 * Native Android Libraries
 */

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Third party Android libraries
 * - Volley: Used to make POST request to the server
 * - GSON: Used to implement Google JSON
 */

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

/**
 * Java Utils
 * - HashMap
 * - Map
 */

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;

/**
 * Own Utils
 * - Crypt: Used to crypt strings
 */

import utils.Crypt;


/**
 * The ibriActivity is the first executed class in the application. It loads the XML template
 * and applies the main configuration to work with our HTTP server.
 *
 * @author Moisés Lodeiro Santiago
 * @version 1
 * @see AppCompatActivity
 * @see LocationListener
 */
public class ibriActivity extends AppCompatActivity implements LocationListener {

    /**
     * TAG_D Used to debug messages
     */

    final String TAG_D = "ibriActivity::";

    /**
     * Gps Meters triggers the locationListener event when the distance changes
     * @see LocationListener
     * @see LocationManager
     */
    static int gpsMeters = 10; // 10 metes
    /**
     * GPS Acuracity is the acuracity that is used to determinate if the drone is near a point using
     * that acuracity.
     */
    static int gpsAcuracity = 15; // 15 meters
    /**
     * Gps Time triggers the locationListener event when the time pass
     * @see LocationListener
     */
    static int gpsTime = 10000; // 10 seconds (10.000 milliseconds)
    /**
     * Serverport is represented with an URL:PORT (without the http:// protocol)
     * Example: ibri.moiseslodeiro.es:80
     */
    static String serverport = "";
    /**
     * The Password variable is used to store the TextView password field
     */
    static String password = "";
    /**
     * The Base64photo is used to save the representation of a base64 photo that is taken by the
     * smartphone
     */
    static String base64Photo = "";
    /**
     * The Drone id to communicate with the server
     */
    static int droneId = 0;
    /**
     * The Latitude of the drone
     */
    static double latitude = 0.0;
    /**
     * The Longitude of the drone
     */
    static double longitude = 0.0;
    /**
     * The Mission representation
     * @see Mission
     */
    static Mission mission = null;
    /**
     * Request code ask permissions is neccesary to request code permissions
     */
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    /**
     * Server Intent manages the background service that is used to communicate to the server
     */
    private Intent serverIntent;
    /**
     * The Log is used to display the log on the main screen
     */
    TextView log;
    /**
     * The Receiver
     */
    BroadcastReceiver receiver;
    /**
     * The Location manager.
     */
    LocationManager locationManager;
    /**
     * The Provider.
     */
    String provider;

    //----------------------------------------------------------------------------------------------

    /**
     * onCreate is executed when the instance is created. Sets the serverport, droneid and password
     * variables and ask to the permission request to the user.
     * @author Moisés Lodeiro Santiago
     * @param savedInstanceState
     * @see Bundle
     * @see TextView
     * @see BroadcastReceiver
     * @see LocationManager
     * @see Criteria
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.ibri_layout);

        TextView tv = (TextView)findViewById(R.id.serverport);
        serverport = String.valueOf(tv.getText());
        TextView di = (TextView)findViewById(R.id.droneID);
        droneId = Integer.parseInt(di.getText().toString());
        TextView pass = (TextView)findViewById(R.id.sharedPass);
        password = String.valueOf(pass.getText());
        log = (TextView)findViewById(R.id.logView);
        log.setMovementMethod(new ScrollingMovementMethod());

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String msg = intent.getStringExtra("message");

                if(msg.startsWith("id ")){
                    TextView mID = (TextView)findViewById(R.id.missionId);
                    mID.setText("Current Mission: "+msg);
                }else {
                    String tmpTxt = ""+log.getText();
                    log.setText(msg+"\n"+tmpTxt);
                }

            }
        };

        // Getting LocationManager object
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Creating an empty criteria object
        Criteria criteria = new Criteria();

        // Getting the name of the provider that meets the criteria
        provider = locationManager.getBestProvider(criteria, false);

        if (provider != null && !provider.equals("")) {

            // Get the location from the given provider
            Location location = locationManager.getLastKnownLocation(provider);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG_D, "Error retreiving gps perms");
                return;
            }

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, ibriActivity.gpsTime, ibriActivity.gpsMeters, this);
            if(location!=null)
                onLocationChanged(location);
            else
                Toast.makeText(getBaseContext(), "Location can't be retrieved", Toast.LENGTH_SHORT).show();

        }

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    /**
     * onRequestPermissionsResults
     * Is triggered when the GPS permission is required
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {

            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG_D+"Perms", "GPS permission has been granted :-)");
                } else {
                    Toast.makeText(ibriActivity.this, "ACCESS_FINE_LOCATION Denied", Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * Start service.
     * This function starts the service that is used to connect to the HTTP server
     * @param view the view
     */
    public void startService(View view) {

        TextView tv = (TextView)findViewById(R.id.serverport);
        serverport = String.valueOf(tv.getText());
        TextView pass = (TextView)findViewById(R.id.sharedPass);
        password = String.valueOf(pass.getText());
        TextView di = (TextView)findViewById(R.id.droneID);
        droneId = Integer.parseInt(di.getText().toString());

        log = (TextView)findViewById(R.id.logView);
        log.setText("");

        serverIntent = new Intent(this, ibriService.class);
        startService(this.serverIntent);
        registerReceiver(receiver, new IntentFilter(ibriService.BROADCAST_ACTION));

        Button stopButton = (Button)findViewById(R.id.endService);
        Button startButton = (Button)findViewById(R.id.startService);

        stopButton.setVisibility(View.VISIBLE);
        startButton.setVisibility(View.GONE);

    }

    /**
     * Stop service.
     * This method is used to stop the service
     * @param view the view
     */
    public void stopService(View view) {
        stopService(this.serverIntent);
        unregisterReceiver(receiver);
        Button stopButton = (Button)findViewById(R.id.endService);
        Button startButton = (Button)findViewById(R.id.startService);
        stopButton.setVisibility(View.GONE);
        startButton.setVisibility(View.VISIBLE);

    }

    /**
     * The onloncationchanged is triggered when gps detects a change of coordinates
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        setTrack(latitude, longitude);
    }

    /**
     * Set the new track to the HTTP server
     * @param latitude
     * @param longitude
     */
    private void setTrack(double latitude, double longitude) {

        RequestQueue queue = Volley.newRequestQueue(this); // this = context
        String url ="http://"+ serverport+"/setDroneTracking/";

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG_D+"Response", response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d(TAG_D+"Response", error.toString());
                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();

                Crypt aesCrypt = Crypt.getInstance(ibriActivity.password);
                DataDron data = new DataDron();

                data.missionId = mission.missionId;
                data.latitude = ibriActivity.latitude;
                data.longitude = ibriActivity.longitude;
                data.droneId = ibriActivity.droneId;
                data.beacon = "";
                data.photo = "";


                int tmpCounter = 0;
                float tmpDistance = Float.MAX_VALUE;

                for(MissionPosition mp: ibriActivity.mission.positions){


                    Log.d(TAG_D+"GPSPoint", mp.getLat()+","+mp.getLng()+" --- "+data.latitude+","+data.longitude);

                    float[] results = new float[1];
                    Location.distanceBetween(mp.getLat(), mp.getLng(), data.latitude, data.longitude, results);

                    Log.d(TAG_D+"Dif", ""+results[0]);

                    if(results[0] < tmpDistance){
                        tmpDistance = results[0];

                        if(results[0] < gpsAcuracity) {
                            data.nearpoint = tmpCounter;
                            data.nearLat = mp.getLat();
                            data.nearLng = mp.getLng();
                        }
                    }

                    tmpCounter++;

                }

                Gson gson = new Gson();
                String str = gson.toJson(data);
                Log.d(TAG_D+"STR CIF", str);
                String c = null;

                try {
                    c = aesCrypt.encrypt_string(str);
                } catch (Exception e){

                }

                params.put("info", c);

                return params;

            }
        };
        queue.add(postRequest);

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}