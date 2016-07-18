package es.moiseslodeiro.ibri;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import android.hardware.Camera.PictureCallback;
import android.view.WindowManager;

/** Takes a single photo on service start. */
public class PhotoTakingService extends Service {

    static byte[] foto;

    @Override
    public void onCreate() {
        super.onCreate();
        takePhoto(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressWarnings("deprecation")
    private static void takePhoto(final Context context) {
        final SurfaceView preview = new SurfaceView(context);
        SurfaceHolder holder = preview.getHolder();
        // deprecated setting, but required on Android versions prior to 3.0
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            //The preview must happen at or after this point or takePicture fails
            public void surfaceCreated(SurfaceHolder holder) {
                showMessage("Surface created");

                Camera camera = null;

                try {
                    camera = Camera.open();
                    showMessage("Opened camera");

                    Camera.Parameters params = camera.getParameters();
                    List<Camera.Size> sizes = params.getSupportedPictureSizes();

                    Camera.Size mSize = null;

                    /*
                    D/SIZE:: 2592w  1944
                    D/SIZE:: 2048w  1536
                    D/SIZE:: 1920w  1080
                    D/SIZE:: 1600w  1200
                    D/SIZE:: 1280w  768
                    D/SIZE:: 1280w  720
                    D/SIZE:: 1024w  768
                    D/SIZE:: 800w  600
                    D/SIZE:: 800w  480
                    D/SIZE:: 720w  480
                    D/SIZE:: 640w  480
                    D/SIZE:: 352w  288
                    D/SIZE:: 320w  240
                    D/SIZE:: 176w  144
                     */

                    for(Camera.Size s : sizes){

                        if(s.width <= 640 && s.height <= 480){
                            mSize = s;
                            break;
                        }
                    }

                    params.setPictureSize(mSize.width, mSize.height);
                    camera.setParameters(params);

                    try {
                        camera.setPreviewDisplay(holder);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    camera.startPreview();
                    showMessage("Started preview");



                    camera.takePicture(null, null, new PictureCallback() {

                        @Override
                        public void onPictureTaken(byte[] data, Camera camera) {
                            showMessage("Took picture");
                            new SavePhotoTask().execute(data);

                            camera.release();
                        }
                    });
                } catch (Exception e) {
                    if (camera != null)
                        camera.release();
                    throw new RuntimeException(e);
                }
            }

            @Override public void surfaceDestroyed(SurfaceHolder holder) {}
            @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
        });

        WindowManager wm = (WindowManager)context
                .getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                1, 1, //Must be at least 1x1
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                0,
                //Don't know if this is a safe default
                PixelFormat.UNKNOWN);

        //Don't set the preview visibility to GONE or INVISIBLE
        wm.addView(preview, params);
    }

    private static void showMessage(String message) {
        Log.i("Camera", message);
    }

    static class SavePhotoTask extends AsyncTask<byte[], String, String> {

        private int numPhoto = 0;

        @Override
        protected String doInBackground(byte[]... jpeg) {
            File photo = new File(Environment.getExternalStorageDirectory(), "/ibriphoto"+String.valueOf(numPhoto)+".jpg");

            if (photo.exists()) {
                photo.delete();
            }

            try {

                FileOutputStream fos=new FileOutputStream(photo.getPath());

                fos.write(jpeg[0]);
                fos.close();

            }
            catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }

            return(null);
        }


        protected void onPostExecute(String result) {

                String filepath = Environment.getExternalStorageDirectory()+"/ibriphoto"+String.valueOf(numPhoto)+".jpg";
                File imagefile = new File(filepath);
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(imagefile);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                Bitmap bm = BitmapFactory.decodeStream(fis);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.WEBP, 100 , baos);
                byte[] b = baos.toByteArray();
                ibriActivity.base64Photo = Base64.encodeToString(b, Base64.NO_WRAP);

                numPhoto++;

                //BufferedWriter out = new BufferedWriter(new FileWriter(Environment.getExternalStorageDirectory()+"/texto.txt"));
                //out.write(ibriActivity.base64Photo);
                //out.close();






        }


    }

}