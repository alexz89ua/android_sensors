package com.stfalcon.client.util;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

/**
 * Created by alex on 01.07.16.
 */
public class VerticalAccUtil {
    private float[] rotationVector = {0,0,0};
    private float[] accelerometerValues = {0,0,0};
    private float verticalAcceleration = 0;
    private final float lowPassAlpha = 0.25f;


    public float onSensorChanged(SensorEvent event) {
        if( event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            rotationVector = lowPass(lowPassAlpha, event.values, rotationVector);
        }
        else if( event.sensor.getType() == Sensor.TYPE_ACCELEROMETER ) {
            accelerometerValues = lowPass(lowPassAlpha, event.values, accelerometerValues);
            verticalAcceleration = getVerticalAcceleration(); // Account for rotations
        }
        return verticalAcceleration;
    }


    private float getVerticalAcceleration(){
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);

        float[] accelerationInWorldFrame = matrixMult3x9(
                accelerometerValues,
                rotationMatrixTranspose(rotationMatrix)
        );

        return accelerationInWorldFrame[2];
    }



    private float[] matrixMult3x9(float[] a, float[] b){
        float[] result = new float[3];
        result[0] = a[0]*b[0] + a[1]*b[3] + a[2]*b[6];
        result[1] = a[0]*b[1] + a[1]*b[4] + a[2]*b[7];
        result[2] = a[0]*b[2] + a[1]*b[5] + a[2]*b[8];
        return result;
    }


    private float[] rotationMatrixTranspose(float[] rotationMatrix){
        float[] result = new float[9];
        result[0] = rotationMatrix[0];
        result[1] = rotationMatrix[3];
        result[2] = rotationMatrix[6];
        result[3] = rotationMatrix[1];
        result[4] = rotationMatrix[4];
        result[5] = rotationMatrix[7];
        result[6] = rotationMatrix[2];
        result[7] = rotationMatrix[5];
        result[8] = rotationMatrix[8];
        return result;
    }


    private float[] lowPass(float alpha, float[] input, float[] output ) {
        for ( int i=0; i<3; i++ ) {
            output[i] = alpha*output[i] + (1-alpha)*input[i];
        }
        return output;
    }
}
