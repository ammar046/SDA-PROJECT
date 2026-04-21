package com.umlytics.service.speech;

import com.umlytics.interfaces.ISpeechToTextService;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

/**
 * Real microphone capture with graceful fallback for STT API.
 * GRASP: Pure Fabrication
 */
public class SpeechToTextServiceImpl implements ISpeechToTextService {

    private static final AudioFormat FORMAT   = new AudioFormat(16000.0f, 16, 1, true, false);
    private static final DataLine.Info LINE_INFO = new DataLine.Info(TargetDataLine.class, FORMAT);

    private TargetDataLine        targetLine;
    private volatile boolean      isRecording = false;
    private ByteArrayOutputStream capturedAudio;
    private Thread                recordingThread;

    /** True only if the system has a supported audio input device. */
    @Override
    public boolean isAvailable() {
        try {
            return AudioSystem.isLineSupported(LINE_INFO);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void startRecording() {
        if (!isAvailable()) {
            System.err.println("[SpeechToTextServiceImpl] Microphone not available.");
            return;
        }
        try {
            targetLine    = (TargetDataLine) AudioSystem.getLine(LINE_INFO);
            targetLine.open(FORMAT);
            targetLine.start();
            isRecording   = true;
            capturedAudio = new ByteArrayOutputStream();

            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[1024];
                while (isRecording) {
                    int read = targetLine.read(buffer, 0, buffer.length);
                    if (read > 0) capturedAudio.write(buffer, 0, read);
                }
            }, "audio-capture");
            recordingThread.setDaemon(true);
            recordingThread.start();
            System.out.println("[SpeechToTextServiceImpl] Recording started.");
        } catch (LineUnavailableException e) {
            System.err.println("[SpeechToTextServiceImpl] Mic open failed: " + e.getMessage());
        }
    }

    @Override
    public String stopRecording() {
        isRecording = false;
        if (targetLine != null) {
            targetLine.stop();
            targetLine.close();
        }
        if (recordingThread != null) {
            try { recordingThread.join(1000); } catch (InterruptedException ignored) {}
        }
        System.out.println("[SpeechToTextServiceImpl] Recording stopped.");

        if (capturedAudio == null || capturedAudio.size() == 0) return "";

        try {
            return callSpeechAPI(capturedAudio.toByteArray());
        } catch (Exception e) {
            System.err.println("[SpeechToTextServiceImpl] STT API call failed: " + e.getMessage());
            return "";
        }
    }

    /**
     * Sends audio bytes to a speech-to-text endpoint.
     * Currently a stub — replace with Whisper REST or Google STT v2 call.
     * Audio format: 16kHz mono 16-bit PCM.
     */
    private String callSpeechAPI(byte[] audioBytes) {
        System.out.println("[SpeechToTextServiceImpl] STT stub — "
                + audioBytes.length + " bytes captured.");
        return "";
    }
}
