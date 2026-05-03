package com.umlytics.services;

import com.umlytics.exceptions.HardwareException;
import com.umlytics.exceptions.SpeechServiceException;
import com.umlytics.interfaces.ISpeechToTextService;

import javax.sound.sampled.AudioSystem;

// GRASP: Pure Fabrication, Low Coupling
public class SpeechToTextServiceImpl implements ISpeechToTextService {
    private final AudioCapture audioCapture;
    private final STTAPIClient sttAPI;

    public SpeechToTextServiceImpl() {
        this.audioCapture = new AudioCapture();
        this.sttAPI = new STTAPIClient();
    }

    @Override
    public void startRecording() {
        if (!isAvailable()) {
            throw new HardwareException("No microphone detected on this system.");
        }
        audioCapture.start();
    }

    @Override
    public String stopRecording() {
        try {
            audioCapture.stop();
            return sttAPI.transcribe(new byte[0]);
        } catch (Exception e) {
            throw new SpeechServiceException("Failed to transcribe recorded audio.", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return AudioSystem.getMixerInfo().length > 0;
    }
}
