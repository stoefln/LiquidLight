package com.stephanpetzl.liquidanimation;

/**
 * Created by steph on 03/03/17.
 */

public class TrackSettings {

    public static final float MAX_TIMING_OFFSET = 500;
    public static final float MAX_DURATION = 500;
    public static final int[] ARDUINO_PINS = new int[]{11, 10, 9, 8, 7, 6, 5, 4};

    public final int arduinoPin;
    public int trackNumber;
    public String pattern;
    public float timingOffsetMillis;
    public float durationMillis;

    public TrackSettings(int trackNumber, int arduinoPin) {
        this.trackNumber = trackNumber;
        this.arduinoPin = arduinoPin;
        this.timingOffsetMillis = 0;
        this.durationMillis = MAX_DURATION / 2;
    }
}
