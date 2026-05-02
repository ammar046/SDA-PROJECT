package com.umlytics.services;

public class AudioCapture {
    private volatile boolean recording;

    public void start() {
        recording = true;
    }

    public void stop() {
        recording = false;
    }

    public boolean isRecording() {
        return recording;
    }
}
