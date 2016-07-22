package es.moiseslodeiro.ibri;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

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

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import utils.Crypt;

/**
 * Created by moises on 4/07/16.
 */
public class ibriService extends Service implements RangeNotifier, BeaconConsumer, LocationListener {


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
    final private String TAG = "ibriService";
    private PowerManager.WakeLock mWakeLock;
    private BeaconManager mBeaconManager;
    /**
     * The constant serviceIntent.
     */
    public static Intent serviceIntent;



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
        serviceIntent = new Intent(getApplicationContext(), PhotoTakingService.class);

        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "worker");

        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());


        //mBeaconManager.setForegroundScanPeriod(5000L); //5000
        //mBeaconManager.setForegroundBetweenScanPeriod(5000L); // 25000

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

        mBeaconManager.bind(this);


        //mBeaconManager.setBackgroundMode(true);



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
     * Send result.
     *
     * @param message the message
     */
    public void sendResult(String message) {
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }

    /**
     * Show m sg.
     *
     * @param msg the msg
     */
    public void showMSg(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

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



    private void getData(){

        RequestQueue queue = Volley.newRequestQueue(this);

        String url ="http://"+ibriActivity.serverport+"/getDroneMissionData/"+ibriActivity.droneId+"/";
        Log.d("URL", url);

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {


                        Log.d("DATA", response.toString());
                        String jsonResponse = "";
                        Crypt aesCrypt = Crypt.getInstance(ibriActivity.password);

                        try {
                            jsonResponse = aesCrypt.decrypt_string(response);
                            Log.d("Response", jsonResponse);

                        } catch (Exception e){

                        }


                        try {

                            JSONObject missionData = new JSONObject(jsonResponse);

                            ibriActivity.mission.missionId = missionData.getInt("mid");

                            JSONArray positions =  missionData.getJSONArray("positions");

                            for(int i = 0; i < positions.length(); i++){
                                MissionPosition tmpPos = new MissionPosition();
                                JSONObject position = new JSONObject(positions.getString(i));
                                //Log.d("REC_LAT", String.valueOf(position.getDouble("lat")));
                                //Log.d("REC_LNG", String.valueOf(position.getDouble("lng")));
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
                Log.d("ErrorGettingData","Is the url ok?");

                try {
                    Thread.sleep(1000);
                    getData();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        });

        // Add the request to the RequestQueue.

        queue.add(stringRequest);

    }

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
                        Log.d("Service", "Waiting for base64Photo...");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                data.photo = ibriActivity.base64Photo;
                Gson gson = new Gson();
                String str = gson.toJson(data);
                Log.d("STR CIF", str);
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

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

        Log.d("BEACON IN RANGE", "Triggered!");

        //Log.d("Detected?", String.valueOf(beacons));

        Log.d("BEACON_SIZE", beacons.size()+"" );

        for (Beacon beacon : beacons) {

            if (beacon.getServiceUuid() == 0xfeaa || beacon.getServiceUuid() == 0xfed8) { // Eddystone || URIbeacon

                Log.d("Beacons...", String.valueOf(beacon.getServiceUuid()));

                String url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
                Log.d("DetectedURL", url);

                for (MissionInsearch missionInsearch : ibriActivity.mission.inSearch) {

                    Log.d("URLs", missionInsearch.getPhysicalWeb() + " -- " + url);
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
                        Log.d("BeaconScan", sendString);
                        sendData(url, ibriActivity.latitude, ibriActivity.longitude);

                    }

                }

            }else{
                Log.d("BEACON ERROR", ""+beacon.getServiceUuid());
            }

        }


    }

    @Override
    public void onLocationChanged(Location location) {



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
