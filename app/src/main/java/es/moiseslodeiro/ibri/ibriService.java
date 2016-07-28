package es.moiseslodeiro.ibri;

/**
 * Native Android Libraries
 */

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

/**
 * Third party Android libraries
 * - Volley: Used to make POST request to the server
 * - GSON: Used to implement Google JSON
 */

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

/**
 * Third party libraries
 * - Altbeacon to detect beacons
 * - JSON to work with json format
 */

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Java Utils
 * - Decimal Format
 * - Hashmap
 * - Collections
 * - Map
 */

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Own Utils
 * - Crypt: Used to crypt strings
 */

import utils.Crypt;

/**
 * ibriService
 * Is a service that runs on background. The service is the responsible to connect to the HTTP
 * server and crypt and decrypt the messages.
 * @see Service
 * @see RangeNotifier
 * @see BeaconConsumer
 * @see LocationListener
 */
public class ibriService extends Service implements RangeNotifier, BeaconConsumer {


    /**
     * The Broadcaster.
     */
    BroadcastReceiver broadcaster;
    /**
     * The Intent.
     */
    Intent intent;
    /**
     * The Broadcast action.
     */
    static final public String BROADCAST_ACTION = "es.moiseslodeiro.ibri";
    /**
     * The Debug Tag
     */
    final private String TAG = "ibriService::";
    /**
     * The Beacon Manager. Used to listen to beacon events
     */
    private BeaconManager mBeaconManager;
    /**
     * The constant serviceIntent.
     */
    public static Intent serviceIntent;
    /**
     * Number Connection Times
     */
    final int maxConnections = 5;
    static int tmpMaxConnections = 0;



    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {

        super.onCreate();

        intent = new Intent(BROADCAST_ACTION);
        ibriActivity.mission = new Mission();


        Log.d(TAG, "Service has been created...");
        serviceIntent = new Intent(getApplicationContext(), ibriPhotoService.class);

        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "worker");

        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());
        mBeaconManager.setBackgroundScanPeriod(5000L); //5000
        mBeaconManager.setBackgroundBetweenScanPeriod(5000L); // 25000

        try {
            mBeaconManager.updateScanPeriods();
        } catch (RemoteException e) {
            e.printStackTrace();
        }


        mBeaconManager.bind(ibriService.this);
        // Altbeacon
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"));
        // EDDYSTONE TLM
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("x,s:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15"));
        // EDDYSTONE UID
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));
        // EDDYSTONE URL
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v"));
        // IBEACON AND ¿URI?
        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("s:0-1=fed8,m:2-2=00,p:3-3:-41,i:4-21v"));
        //
        mBeaconManager.bind(this);

    }

    @Override
    public synchronized int onStartCommand(Intent intent, int flags, int startId) {
        getData();
        Log.d(TAG, "Service started");
        showMSg("Service Started");
        return START_STICKY;
    }

    @Override
    public synchronized void onDestroy() {
        sendResult("Service has been destroyed");
        mBeaconManager.unbind(this);
        stopService(serviceIntent);
        Log.d(TAG, "Service destroyed...");
    }

    /**
     * Send result to main activity
     *
     * @param message Sends the broadcast message to main activity
     */
    public void sendResult(String message) {
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }

    /**
     * Show msg
     * Displays the msg in a Toast
     * @see Toast
     * @param msg the msg
     */
    public void showMSg(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * onBeaconServiceConnect
     * Is triggered when beacon service is connected using "all-beacons-region"
     */
    @Override
    public void onBeaconServiceConnect() {
        Region region = new Region("all-beacons-region", null, null, null);
        try {
            mBeaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mBeaconManager.setRangeNotifier(this);
    }


    /**
     * Get Data
     * Make a GET request to the server to obtain the mission data and stores the data in
     * @author Moisés Lodeiro Santiago
     * @see Volley
     */
    private void getData(){

        RequestQueue queue = Volley.newRequestQueue(this);


        String url ="http://"+ibriActivity.serverport+"/getDroneMissionData/"+ibriActivity.droneId+"/";
        Log.d("URL", url);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {


                        Log.d(TAG+"DATA", response.toString());
                        String jsonResponse = "";
                        Crypt aesCrypt = Crypt.getInstance(ibriActivity.password);

                        try {
                            jsonResponse = aesCrypt.decrypt_string(response);
                            Log.d(TAG+"Response", jsonResponse);

                        } catch (Exception e){
                            Log.e(TAG+"ERROR", e.toString());
                        }

                        try {

                            JSONObject missionData = new JSONObject(jsonResponse);

                            ibriActivity.mission.missionId = missionData.getInt("mid");

                            JSONArray positions =  missionData.getJSONArray("positions");

                            for(int i = 0; i < positions.length(); i++){
                                MissionPosition tmpPos = new MissionPosition();
                                JSONObject position = new JSONObject(positions.getString(i));
                                tmpPos.setLat(position.getDouble("lat"));
                                tmpPos.setLng(position.getDouble("lng"));
                                ibriActivity.mission.positions.add(tmpPos);

                            }

                            sendResult("Connected!");

                            JSONArray insearch =  missionData.getJSONArray("insearch");

                            for(int i = 0; i < insearch.length(); i++){
                                MissionInsearch tmpIn = new MissionInsearch();
                                JSONObject physicalCode = new JSONObject(insearch.getString(i));
                                Log.d("PhysicalWeb Insearch: ", physicalCode.getString("physicalCode"));
                                tmpIn.setPhysicalWeb(physicalCode.getString("physicalCode"));
                                ibriActivity.mission.inSearch.add(tmpIn);
                            }


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {

                sendResult("Error connecting.. trying again!");
                Log.d(TAG+"DataError","Is the url ok?");

                if(tmpMaxConnections < maxConnections) {

                    try {
                        Thread.sleep(1000);
                        tmpMaxConnections++;
                        getData();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }

            }

        });

        stringRequest.setRetryPolicy(new DefaultRetryPolicy(20 * 1000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // Add the request to the RequestQueue.
        queue.add(stringRequest);

    }

    /**
     * The send Data function sends the data to the HTTP server using POST method
     * @param beaconUrl Detecter Beacon URL
     * @param lat Latitude
     * @param lng Longitude
     */
    private void sendData(final String beaconUrl, final double lat, final double lng){

        RequestQueue queue = Volley.newRequestQueue(this); // this = context
        String url ="http://"+ibriActivity.serverport+"/setDroneTracking/";

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        Log.d("Server Response: ", response);
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

                data.missionId = ibriActivity.mission.missionId;
                data.latitude = lat;
                data.longitude = lng;
                data.droneId = ibriActivity.droneId;
                data.beacon = beaconUrl;

                if(lat != 0 && lng != 0){
                    int tmpCounter = 0;
                    float tmpDistance = Float.MAX_VALUE;
                    for(MissionPosition mp: ibriActivity.mission.positions){

                        float[] results = new float[1];
                        Location.distanceBetween(mp.getLat(), mp.getLng(), lat, lng, results);

                        if(results[0] < tmpDistance){
                            tmpDistance = results[0];
                            data.nearpoint = tmpCounter;
                            data.nearLat = mp.getLat();
                            data.nearLng = mp.getLng();
                        }

                        tmpCounter++;
                    }

                }

                while(ibriActivity.base64Photo.equals("")){
                    try {
                        Thread.sleep(500);
                        Log.d(TAG+"Service", "Waiting for base64Photo...");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                data.photo = ibriActivity.base64Photo;
                Gson gson = new Gson();
                String str = gson.toJson(data);
                Log.d(TAG+"STR CIF", str);
                String c = null;

                try {
                    c = aesCrypt.encrypt_string(str);
                } catch (Exception e){

                }

                stopService(serviceIntent);

                params.put("info", c);

                return params;

            }
        };
        queue.add(postRequest);
    }

    /**
     * Did Range Beacons In Region
     * Detects if the system detects any beacon in region
     * @param beacons
     * @param region
     */
    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

        Log.d(TAG+"IN RANGE", "Triggered!");
        Log.d(TAG+"B_SIZE", beacons.size()+"" );

        for (Beacon beacon : beacons) {

            if (beacon.getServiceUuid() == 0xfeaa || beacon.getServiceUuid() == 0xfed8) { // Eddystone || URIbeacon

                Log.d(TAG+"Beacons...", String.valueOf(beacon.getServiceUuid()));

                String url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
                Log.d(TAG+"DetectURL", url);

                for (MissionInsearch missionInsearch : ibriActivity.mission.inSearch) {

                    Log.d(TAG+"URLs", missionInsearch.getPhysicalWeb() + " -- " + url);
                    startService(serviceIntent);

                    if (missionInsearch.getPhysicalWeb().equals(url)) {
                        startService(serviceIntent);

                        DecimalFormat df = new DecimalFormat("##.###");
                        String sendString = "Beacon! " + url + "\n";
                        sendString += beacon.getBluetoothAddress() + "\n";
                        sendString += df.format(beacon.getDistance()) + "m away\n";
                        sendString += "near " + ibriActivity.latitude + ", " + ibriActivity.longitude + "\n";
                        sendString += "---------------------------------------------------------------------------------------";
                        sendResult(sendString);
                        Log.d(TAG+"BeaconScan", sendString);
                        sendData(url, ibriActivity.latitude, ibriActivity.longitude);

                    }

                }

            }else{
                Log.d(TAG+"B_ERROR", ""+beacon.getServiceUuid());
            }

        }


    }


}
