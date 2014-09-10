package com.stfalcon.server;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import java.util.ArrayList;

/**
 * User: Anton Bevza
 * Date: 2/18/14
 * Time: 2:14 PM
 */
public class SoundManager {
    public static final String LOG_TAG = "uaroad_SoundManager";
    private final int duration = 1; // seconds
    private final int sampleRate = 4000;
    private final int numSamples = duration * sampleRate;
    private final double sample[] = new double[numSamples];
    private double freqOfTone = 100; // hz
    private ArrayList<AudioTrack> audioTracks = new ArrayList<AudioTrack>();

    private final byte generatedSnd[] = new byte[2 * numSamples];

    public void genTone(double acc) {
        freqOfTone = (acc / 40) * 1000 + 500;
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / freqOfTone));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        for (double dVal : sample) {
            short val = (short) (dVal * 32767);
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
    }

    public void playSound() {
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                8000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT, numSamples,
                AudioTrack.MODE_STREAM);
        try {
            audioTracks.add(audioTrack);
            audioTrack.write(generatedSnd, 0, numSamples);
            audioTrack.play();
        } catch (IllegalArgumentException e) {
            releaseAllTracks();
            e.printStackTrace();
        } catch (IllegalStateException e) {
            releaseAllTracks();
            e.printStackTrace();
        }
    }

    private void releaseAllTracks() {
        for (AudioTrack audioTrack : audioTracks) {
            audioTrack.release();
        }
    }
}
