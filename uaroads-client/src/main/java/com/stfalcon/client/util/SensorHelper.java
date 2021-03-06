package com.stfalcon.client.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.stfalcon.client.data.AccelData;

/**
 * Created by alexandr on 03.09.14.
 */
public class SensorHelper {

    public static final int SENSOR_DELAY_36Hz = 27777; // in  microseconds
    public static final int ROTATION_SENSOR_DELAY = 50 * 1000; // 50ms


    private static float[] motion = new float[3];
    private static float[] gravity = new float[3];
    private float gX, gY, gZ;
    private VerticalAccUtil verticalAccUtil;
    private Sensor mRotationSensor;
    private Context context;


    public SensorHelper(Context context) {
        this.context = context;
        verticalAccUtil = new VerticalAccUtil();
    }

    private void getSensorDelay(SensorManager sensorManager) {
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Log.i("Loger", "TYPE_ACCELEROMETER - " + sensor.getName());
        Log.i("Loger", "Version - " + sensor.getVersion());
        Log.i("Loger", "Type - " + sensor.getType());
        Log.i("Loger", "Vendor - " + sensor.getVendor());
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Log.i("Loger", "FifoMaxEventCount - " + sensor.getFifoMaxEventCount());
            Log.i("Loger", "FifoReservedEventCount - " + sensor.getFifoReservedEventCount());
        }
        Log.i("Loger", "MaximumRange - " + sensor.getMaximumRange());
        Log.i("Loger", "MinDelay - " + sensor.getMinDelay());
        Log.i("Loger", "Power - " + sensor.getPower());
        Log.i("Loger", "Resolution - " + sensor.getResolution());

    }


    /**
     * Підписуємось на прослуховування датчиків
     *
     * @param listener
     * @param sensorManager
     * @param sensorType
     */
    public void registrateListener(SensorEventListener listener, SensorManager sensorManager, int sensorType) {
        mRotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (mRotationSensor == null) {
            Toast.makeText(context,
                    "Rotation vector sensor not available; will not provide orientation data",
                    Toast.LENGTH_LONG).show();
        } else {
            sensorManager.registerListener(listener, mRotationSensor, ROTATION_SENSOR_DELAY);
        }
        switch (sensorType) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                Sensor lAcc = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
                sensorManager.registerListener(listener, lAcc, SENSOR_DELAY_36Hz);
                break;

            case Sensor.TYPE_GRAVITY:
                Sensor gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
                sensorManager.registerListener(listener, gravity, SENSOR_DELAY_36Hz);
                break;

            default:
                Sensor acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                sensorManager.registerListener(listener, acc, SENSOR_DELAY_36Hz);
                break;
        }
    }


    /**
     * Аналізує данні що надійшли з сенсорів
     *
     * @param sensorEvent
     * @param time
     * @return Object[] result  - де  long   result[0] - поточний час надходження,
     * String result[1] - оброблені дані сенсора,
     * int    result[2] - тип сенсора
     */
    public AccelData analyzeSensorEvent(SensorEvent sensorEvent, long time) {

        gX = sensorEvent.values[0] / SensorManager.GRAVITY_EARTH;
        gY = sensorEvent.values[1] / SensorManager.GRAVITY_EARTH;
        gZ = sensorEvent.values[2] / SensorManager.GRAVITY_EARTH;

        // gForce will be close to 1 when there is no movement.
        float sqrt = Math.abs((float) (Math.sqrt(gX * gX + gY * gY + gZ * gZ) - 1));
        float verticalAcc =  Math.abs(verticalAccUtil.onSensorChanged(sensorEvent) / SensorManager.GRAVITY_EARTH - 1);

        int sensorType = sensorEvent.sensor.getType();

        return new AccelData(null, sensorType, gX, gY, gZ, sqrt, verticalAcc,
                System.currentTimeMillis(), 0, 0, 0);
    }


}
