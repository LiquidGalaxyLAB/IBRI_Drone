package es.moiseslodeiro.ibri;

/*
 *
 */


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
     * Static Variables
     */
    static String serverport = "";
    /**
     * The Password.
     */
    static String password = "";
    /**
     * The Base 64 photo.
     */
    static String base64Photo = "";
    /**
     * The Drone id.
     */
    static int droneId = 0;
    /**
     * The Latitude.
     */
    static double latitude = 0.0;
    /**
     * The Longitude.
     */
    static double longitude = 0.0;
    /**
     * The Mission.
     */
    static Mission mission = null;

    /**
     * Final Variables
     */

    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;

    /**
     * Local and private vars
     */

    private Intent serverIntent;
    /**
     * The Log.
     */
    TextView log;
    /**
     * The Receiver.
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
     * @author Moisés Lodeiro Santiago
     *
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
                String tmpTxt = ""+log.getText();
                log.setText(msg+"\n"+tmpTxt);

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
                Log.d("ERROR:","Error retreiving gps perms");
                return;
            }
            //locationManager.requestLocationUpdates(provider, 20000, 1, this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, this);

            if(location!=null)
                onLocationChanged(location);
            else
                Toast.makeText(getBaseContext(), "Location can't be retrieved", Toast.LENGTH_SHORT).show();

        }else{
            Toast.makeText(getBaseContext(), "No Provider Found", Toast.LENGTH_SHORT).show();
        }

    }




    @Override
    public void onResume() {
        super.onResume();

        /*erviceIntent = new Intent(getApplicationContext(),
                ibriService.class);
        startService(erviceIntent);*/

        //registerReceiver(receiver, new IntentFilter(
        //        ibriService.BROADCAST_ACTION));

    }


    @Override
    public void onPause() {
        super.onPause();

        //unregisterReceiver(receiver);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    Log.d("GPS_PERMS", "GPS PERMISSION HAS BEEN GRANTED");
                } else {
                    // Permission Denied
                    Toast.makeText(ibriActivity.this, "ACCESS_FINE_LOCATION Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    /**
     * Start service.
     *
     * @param view the view
     */
    public void startService(View view) {

        TextView tv = (TextView)findViewById(R.id.serverport);
        serverport = String.valueOf(tv.getText());
        TextView pass = (TextView)findViewById(R.id.sharedPass);
        password = String.valueOf(pass.getText());
        TextView di = (TextView)findViewById(R.id.droneID);
        droneId = Integer.parseInt(di.getText().toString());

        serverIntent = new Intent(this, ibriService.class);
        startService(this.serverIntent);
        registerReceiver(receiver, new IntentFilter(ibriService.BROADCAST_ACTION));

        Button stopButton = (Button)findViewById(R.id.endService);
        Button startButton = (Button)findViewById(R.id.startService);

        stopButton.setVisibility(View.VISIBLE);
        startButton.setVisibility(View.GONE);
        //stopButton.setEnabled(true);
        //startButton.setEnabled(false);

    }

    /**
     * Stop service.
     *
     * @param view the view
     */
    public void stopService(View view) {
        stopService(this.serverIntent);
        unregisterReceiver(receiver);
        Button stopButton = (Button)findViewById(R.id.endService);
        Button startButton = (Button)findViewById(R.id.startService);


        stopButton.setVisibility(View.GONE);
        startButton.setVisibility(View.VISIBLE);
        //stopButton.setEnabled(false);
        //startButton.setEnabled(true);
    }



    @Override
    public void onLocationChanged(Location location) {

        latitude = location.getLatitude();
        longitude = location.getLongitude();

        //Log.d("LOCATION", this.latitude+" -- "+this.longitude);
        setTrack(latitude, longitude);


    }

    private void setTrack(double latitude, double longitude) {

        RequestQueue queue = Volley.newRequestQueue(this); // this = context
        String url ="http://"+ serverport+"/setDroneTracking/";

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Voley Response", response);
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error.Response", error.toString());
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

                    //Log.d("GPSPoint", mp.getLat()+","+mp.getLng()+" --- "+data.latitude+","+data.longitude);

                    float[] results = new float[1];
                    Location.distanceBetween(mp.getLat(), mp.getLng(), data.latitude, data.longitude, results);

                    //Log.d("Difference:", ""+results[0]);

                    if(results[0] < tmpDistance){
                        tmpDistance = results[0];

                        if(results[0] < 15) { // 10m and acuracy of 5m...
                            data.nearpoint = tmpCounter;
                            data.nearLat = mp.getLat();
                            data.nearLng = mp.getLng();
                        }

                    }

                    tmpCounter++;

                }

                Gson gson = new Gson();
                String str = gson.toJson(data);
                //Log.d("STR CIF", str);
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