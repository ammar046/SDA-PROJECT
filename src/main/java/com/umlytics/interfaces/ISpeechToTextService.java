package com.umlytics.interfaces;

public interface ISpeechToTextService {
    void startRecording();

    String stopRecording();

    boolean isAvailable();
}
