package es.moiseslodeiro.ibri;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
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
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import utils.Crypt;
import utils.DataDron;

/**
 * Created by moises on 4/07/16.
 */

public class ibriService extends Service implements RangeNotifier, BeaconConsumer {

    BroadcastReceiver broadcaster;
    Intent intent;
    static final public String BROADCAST_ACTION = "es.moiseslodeiro.ibri";
    final private String TAG = "ibriService";
    private PowerManager.WakeLock mWakeLock;
    private BeaconManager mBeaconManager;
    public static Intent serviceIntent;


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {

        super.onCreate();

        intent = new Intent(BROADCAST_ACTION);




        Log.d(TAG, "Servicio creado...");

        PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "worker");

        mBeaconManager = BeaconManager.getInstanceForApplication(this.getApplicationContext());


        mBeaconManager.setForegroundScanPeriod(5000L);
        mBeaconManager.setForegroundBetweenScanPeriod(25000L);

        mBeaconManager.setBackgroundScanPeriod(5000L);
        mBeaconManager.setBackgroundBetweenScanPeriod(25000L);

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

    public void sendResult(String message) {
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }

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
                        String respuesta = "";
                        Crypt aesCrypt = Crypt.getInstance();

                        try {
                            respuesta = aesCrypt.decrypt_string(response);
                            Log.d("Respuesta", respuesta);
                        } catch (Exception e){

                        }


                        try {

                            JSONObject jo = new JSONObject(respuesta);




                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        //Map jsonJavaRootObject = new Gson().fromJson(respuesta, Map.class);



                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("ERROR GETTING DATA","That didn't work!");
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

                Crypt aesCrypt = Crypt.getInstance();
                DataDron data = new DataDron();
                data.latitude = lat;
                data.longitude = lng;
                data.beacon = beaconUrl;
                Gson gson = new Gson();
                String str = gson.toJson(data);
                Log.d("STR CIF", str);
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
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

        for (Beacon beacon : beacons) {

            if (beacon.getServiceUuid() == 0xfeaa || beacon.getServiceUuid() == 0xfed8) { // Eddystone || URIbeacon

                String url = UrlBeaconUrlCompressor.uncompress(beacon.getId1().toByteArray());
                DecimalFormat df = new DecimalFormat("##.###");
                String sendString = "Beacon! "+url+"\n";
                sendString += beacon.getBluetoothAddress()+"\n";
                sendString += df.format(beacon.getDistance())+"m away\n";
                sendString += "near "+ibriActivity.latitude+", "+ibriActivity.longitude+"\n";
                sendString += "---------------------------------------------------------------------------------------";
                sendResult(sendString);

                Log.d("BeaconScan", sendString);


                sendData(url, ibriActivity.latitude, ibriActivity.longitude);


            }

        }

        serviceIntent = new Intent(getApplicationContext(), PhotoTakingService.class);
        startService(serviceIntent);

    }

}
