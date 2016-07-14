package es.moiseslodeiro.ibri;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;

import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.utils.UrlBeaconUrlCompressor;
import org.w3c.dom.Text;

import java.util.Collection;
import java.util.List;


public class ibriActivity extends AppCompatActivity implements LocationListener {

    private Intent serverIntent;
    TextView log;
    BroadcastReceiver receiver;
    Intent serviceIntent;
    LocationManager locationManager;
    String provider;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;


    static String serverport = "";
    static String password = "";
    static String droneId = "";

    static double latitude = 0.0;
    static double longitude = 0.0;
    private SurfaceTexture mPreviewSurfaceTexture;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ibri_layout);


        TextView tv = (TextView)findViewById(R.id.serverport);
        serverport = (String) String.valueOf(tv.getText());

        TextView di = (TextView)findViewById(R.id.droneID);
        droneId = (String) String.valueOf(di.getText());

        TextView pass = (TextView)findViewById(R.id.sharedPass);
        password = (String) String.valueOf(pass.getText());



        log = (TextView)findViewById(R.id.logView);
        log.setMovementMethod(new ScrollingMovementMethod());
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                //String time = intent.getStringExtra("time");
                String msg = intent.getStringExtra("message");
                String tmpTxt = ""+log.getText();
                log.setText(msg+"\n"+tmpTxt);

                //log.append(counter+"\n");

            }
        };

        // Getting LocationManager object
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Creating an empty criteria object
        Criteria criteria = new Criteria();

        //requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_ASK_PERMISSIONS);

        // Getting the name of the provider that meets the criteria
        provider = locationManager.getBestProvider(criteria, false);

        if (provider != null && !provider.equals("")) {

            // Get the location from the given provider
            Location location = locationManager.getLastKnownLocation(provider);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {



                Log.d("ERROR:","Error retreiving gps perms");
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            //locationManager.requestLocationUpdates(provider, 20000, 1, this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);

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

        /*serviceIntent = new Intent(getApplicationContext(),
                ibriService.class);
        startService(serviceIntent);*/

        //registerReceiver(receiver, new IntentFilter(
        //        ibriService.BROADCAST_ACTION));

    }


    @Override
    public void onPause() {
        super.onPause();

        //stopService(serviceIntent);
        //unregisterReceiver(receiver);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    Log.d("HOLA", "SE HAN ACEPTADO LOS PERMISOS DE GPS");
                } else {
                    // Permission Denied
                    Toast.makeText(ibriActivity.this, "ACCESS_FINE_LOCATION Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    public void startService(View view) {
        serverIntent = new Intent(this, ibriService.class);
        startService(this.serverIntent);
        registerReceiver(receiver, new IntentFilter(ibriService.BROADCAST_ACTION));

        Button stopButton = (Button)findViewById(R.id.endService);
        Button startButton = (Button)findViewById(R.id.startService);
        stopButton.setEnabled(true);
        startButton.setEnabled(false);

    }

    public void stopService(View view) {
        stopService(this.serverIntent);
        unregisterReceiver(receiver);
        Button stopButton = (Button)findViewById(R.id.endService);
        Button startButton = (Button)findViewById(R.id.startService);
        stopButton.setEnabled(false);
        startButton.setEnabled(true);
    }

    @Override
    public void onLocationChanged(Location location) {

        // 28.4815393,-16.321767

        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();


        /*float[] results = new float[1];
        Location.distanceBetween(28.4815393,-16.321767, location.getLatitude(), location.getLongitude(), results);
        float distanceInMeters = results[0];
        Log.d("DISTANCIA", distanceInMeters+"");
        boolean isInRange = distanceInMeters <= 10;
        Log.d("DISTANCIA", isInRange+"");
        */

//--------------------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------------------



        //EJEMPLO OK
        /*Crypt aesCrypt = Crypt.getInstance();


        DataDron data = new DataDron();
        data.latitude = 20.50;
        data.longitude = 10.20;
        data.beacon = 1;

        Gson gson = new Gson();
        String str = gson.toJson(data);

        //String str = "";
        Log.i("myTag", "step1: " + str);
        String a = null;
        try {
            a = aesCrypt.encrypt_string(str);
        } catch (Exception e){

        }
        Log.i("myTag", "step2: " + a);
        String b = null;
        try {
            b = aesCrypt.decrypt_string(a);
        } catch (Exception e){

        }
        Log.i("myTag", "step3: " + b);
        */

//--------------------------------------------------------------------------------------------------
//--------------------------------------------------------------------------------------------------





        //Log.d("DEBUG_GPS", "LNG: "+location.getLongitude());
        //Log.d("DEBUG_GPS:", "LAT: "+location.getLatitude());

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