package edu.cmu.pocketsphinx.demo.audio;

import android.media.MediaRecorder;
import java.io.IOException;

/**
 * Created by Noel on 5/9/2017.
 */

public class AudioRecorder {
    private final static String TAG = AudioRecorder.class.getName();

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;

    public AudioRecorder() {

    }

    public void startRecording(String filename) throws IOException {
        startRecording(filename, 0);
    }

    public void startRecording(String filename, int maxDurationMillis) throws IOException {
        if (isRecording) {
            stopRecording();
        }
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setOutputFile(filename);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        if (maxDurationMillis > 0) {
            mediaRecorder.setMaxDuration(maxDurationMillis);
        }
        mediaRecorder.prepare();
        mediaRecorder.start();
        isRecording = true;
    }

    public void stopRecording() {
        if (!isRecording) {
            return;
        }
        isRecording = false;
        mediaRecorder.stop();
        mediaRecorder.release();
        mediaRecorder = null;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public void setListener(MediaRecorder.OnInfoListener listener) {
        if (mediaRecorder != null) {
            mediaRecorder.setOnInfoListener(listener);
        }
    }
}
