package es.moiseslodeiro.ibri;

/**
 * Native Android Libraries
 */
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

/**
 * Java Utils
 */
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * The ibriPhotoService class is a service that runs in background. It allows to open the smartphone
 * camera and take a photo when a insearch beacon is detected. Then, the photo is stored in the
 * smartphone and also it is converted to a base64 and saved in the ibriActivity.
 * @author Moisés Lodeiro Santiago
 * @see Camera
 * @see Base64
 */

public class ibriPhotoService extends Service {

    /**
     * The Foto in byte array format
     */
    static byte[] foto;


    /* Photo Size Table (width height)
        D/SIZE:: 2592w  1944h
        D/SIZE:: 2048w  1536h
        D/SIZE:: 1920w  1080h
        D/SIZE:: 1600w  1200h
        D/SIZE:: 1280w  768h
        D/SIZE:: 1280w  720h
        D/SIZE:: 1024w  768h
        D/SIZE:: 800w  600h
        D/SIZE:: 800w  480h
        D/SIZE:: 720w  480h
        D/SIZE:: 640w  480h
        D/SIZE:: 352w  288h
        D/SIZE:: 320w  240h
        D/SIZE:: 176w  144h
    */

    /**
     * Photo Width
     * See the column row of the table above
     */
    final int photoWidth = 640;

    /**
     * Photo Height
     * see the second column of the table above
     */
    final int photoHeight = 480;

    /**
     * When the service is created, a photo is taken
     */
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

    /**
     * The take photo method takes the photo from the front camera and stores it in the smartphone
     * @param context
     */
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

                    for(Camera.Size s : sizes){

                        if(s.width <= photoWidth && s.height <= photoHeight){
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
                0, //Don't know if this is a safe default
                PixelFormat.UNKNOWN);

        //Don't set the preview visibility to GONE or INVISIBLE
        wm.addView(preview, params);
    }

    private static void showMessage(String message) {
        Log.i("Camera", message);
    }

    /**
     * The SavePhotoTask is used to save the photo in a background process
     */
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

                Log.d("PhotoService", "OnPostExcexute #"+String.valueOf(numPhoto));

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


        }


    }

}