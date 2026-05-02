package com.umlytics.domain;

import java.util.UUID;

public class VoiceInput {
    private UUID voiceId;
    private int durationSeconds;
    private byte[] audioData;
    private String transcribedText;

    public VoiceInput() {
    }

    public UUID getVoiceId() {
        return voiceId;
    }

    public void setVoiceId(UUID voiceId) {
        this.voiceId = voiceId;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public byte[] getAudioData() {
        return audioData;
    }

    public void setAudioData(byte[] audioData) {
        this.audioData = audioData;
    }

    public String getTranscribedText() {
        return transcribedText;
    }

    public void setTranscribedText(String transcribedText) {
        this.transcribedText = transcribedText;
    }
}
