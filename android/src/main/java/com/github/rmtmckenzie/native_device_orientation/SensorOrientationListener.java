package com.github.rmtmckenzie.native_device_orientation;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.view.OrientationEventListener;
import android.hardware.SensorEventListener;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;


public class SensorOrientationListener implements IOrientationListener {
    enum Rate {
        normal(SensorManager.SENSOR_DELAY_NORMAL),
        ui(SensorManager.SENSOR_DELAY_UI),
        game(SensorManager.SENSOR_DELAY_GAME),
        fastest(SensorManager.SENSOR_DELAY_FASTEST);

        int nativeValue;
        Rate(int nativeValue) {
            this.nativeValue = nativeValue;
        }
    }

    private final Context context;
    private final OrientationCallback callback;
    private final Rate rate;

    private SensorEventListener orientationSensorEventListener;
    private Sensor orientationSensor;
    private SensorManager sensorManager;
    private NativeOrientation lastOrientation = null;
    private float[] rMat;
    private float[] orientation;

    public SensorOrientationListener(Context context, OrientationCallback callback, Rate rate) {
        this.context = context;
        this.callback = callback;
        this.rate = rate;

        orientation = new float[3];
        rMat = new float[9];

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (orientationSensor == null)
        {
            orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        }
    }

    public SensorOrientationListener(Context context, OrientationCallback callback) {
        this(context, callback, Rate.ui);
    }


    @Override
    public void startOrientationListener() {
        if (orientationSensorEventListener != null) {
            callback.receive(lastOrientation);
            return;
        }

        orientationSensorEventListener = new SensorEventListener() {
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            public void onSensorChanged(SensorEvent event) {
                SensorManager.getRotationMatrixFromVector(rMat, event.values);
                SensorManager.getOrientation(rMat, orientation);
                //Log.d("", "Orientation " + orientation[0] + "  " + orientation[1] + "  " + orientation[2]);

                int defaultOrientation = getDeviceDefaultOrientation();
                if (defaultOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    // add offset to landscape
                    //angle += 90;
                }

                NativeOrientation newOrientation = NativeOrientation.PortraitDown;
                if (((lastOrientation == NativeOrientation.PortraitDown || lastOrientation == NativeOrientation.PortraitUp) && Math.abs(orientation[2]) > Math.abs(orientation[1]) + 0.5) ||
                        ((lastOrientation == NativeOrientation.LandscapeLeft || lastOrientation == NativeOrientation.LandscapeRight) && Math.abs(orientation[2]) + 0.5 > Math.abs(orientation[1]))) {
                    //Log.d("", "LANDSCAPE!");
                    if(orientation[0]>=0){
                        newOrientation = NativeOrientation.LandscapeRight;
                    } else {
                        newOrientation = NativeOrientation.LandscapeLeft;
                    }
                } else {
                    if(orientation[1]>=0){
                        //Log.d("", "Portrait Down!");
                        newOrientation = NativeOrientation.PortraitDown;
                    } else {
                        //Log.d("", "Portrait Up!");
                        newOrientation = NativeOrientation.PortraitUp;
                    }
                }

                if (!newOrientation.equals(lastOrientation)) {
                    lastOrientation = newOrientation;
                    callback.receive(newOrientation);
                }
            }
        };

        sensorManager.registerListener(orientationSensorEventListener, orientationSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void stopOrientationListener() {
        if (sensorManager != null && orientationSensorEventListener != null)
        {
            sensorManager.unregisterListener(orientationSensorEventListener);
        }
        orientationSensorEventListener = null;
    }

    public int getDeviceDefaultOrientation() {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        Configuration config = context.getResources().getConfiguration();

        int rotation = windowManager.getDefaultDisplay().getRotation();

        if (((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
                config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
                config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
            return Configuration.ORIENTATION_LANDSCAPE;
        } else {
            return Configuration.ORIENTATION_PORTRAIT;
        }
    }
}
