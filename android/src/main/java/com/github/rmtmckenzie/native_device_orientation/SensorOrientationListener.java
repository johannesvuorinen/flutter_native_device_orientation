package com.github.rmtmckenzie.native_device_orientation;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.view.OrientationEventListener;
import android.hardware.SensorEventListener;
import android.util.Log;

public class SensorOrientationListener implements IOrientationListener {

    private final OrientationReader reader;
    private final Context context;
    private final OrientationCallback callback;
    private SensorEventListener orientationSensorEventListener;
    private Sensor orientationSensor;
    private SensorManager sensorManager;
    private OrientationReader.Orientation lastOrientation = null;
    private float[] rMat;
    private float[] orientation;

    public SensorOrientationListener(OrientationReader orientationReader, Context context, OrientationCallback callback) {
        this.reader = orientationReader;
        this.context = context;
        this.callback = callback;
        orientation = new float[3];
        rMat = new float[9];

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (orientationSensor == null)
        {
            orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        }
    }

    @Override
    public void startOrientationListener() {
        if (orientationSensorEventListener != null) return;

        orientationSensorEventListener = new SensorEventListener() {
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            public void onSensorChanged(SensorEvent event) {
                SensorManager.getRotationMatrixFromVector(rMat, event.values);
                SensorManager.getOrientation(rMat, orientation);
                //Log.d("", "Orientation " + orientation[0] + "  " + orientation[1] + "  " + orientation[2]);

                OrientationReader.Orientation newOrientation = reader.getDeviceDefaultOrientation();
                if (((lastOrientation == OrientationReader.Orientation.PortraitDown || lastOrientation == OrientationReader.Orientation.PortraitUp) && Math.abs(orientation[2]) > Math.abs(orientation[1]) + 0.5) ||
                        ((lastOrientation == OrientationReader.Orientation.LandscapeLeft || lastOrientation == OrientationReader.Orientation.LandscapeRight) && Math.abs(orientation[2]) + 0.5 > Math.abs(orientation[1]))) {
                    //Log.d("", "LANDSCAPE!");
                    if(orientation[0]>=0){
                        newOrientation = OrientationReader.Orientation.LandscapeRight;
                    } else {
                        newOrientation = OrientationReader.Orientation.LandscapeLeft;
                    }
                } else {
                    if(orientation[1]>=0){
                        //Log.d("", "Portrait Down!");
                        newOrientation = OrientationReader.Orientation.PortraitDown;
                    } else {
                        //Log.d("", "Portrait Up!");
                        newOrientation = OrientationReader.Orientation.PortraitUp;
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
}
