/* ====================================================================
 * Copyright (c) 2014 Alpha Cephei Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY ALPHA CEPHEI INC. ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 */

package edu.cmu.pocketsphinx.demo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.widget.Toast.makeText;

public class SpeechActivity extends Activity implements RecognitionListener {

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "fillers";

    /* Used to handle permission request */
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private SpeechRecognizer recognizer;
    private String partialResult;
    private Map<String, Integer> fillerCount;
    private Button startRecordingBtn;
    private boolean isRecording;
    private TextView resultText, captionText;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        partialResult = "";
        fillerCount = new HashMap<>();
        setContentView(R.layout.main);
        captionText = (TextView) findViewById(R.id.caption_text);
        resultText = (TextView) findViewById(R.id.result_text);
        startRecordingBtn = (Button) findViewById(R.id.start);

        captionText.setText("Preparing the recognizer");

        startRecordingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    stopRecording();
                } else {
                    startRecording();
                }
            }
        });

        // Check if user has given permission to record audio
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        runRecognizerSetup();
    }

    private void stopRecording() {
        isRecording = false;
        recognizer.stop();
        startRecordingBtn.setText("Start");
    }

    private void startRecording() {
        startRecordingBtn.setText("Stop");
        recognizer.stop();

        isRecording = true;
        recognizer.startListening(KWS_SEARCH);

        captionText.setText(R.string.kws_caption);
    }

    private void runRecognizerSetup() {
        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(SpeechActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    ((TextView) findViewById(R.id.caption_text))
                            .setText("Failed to init recognizer " + result);
                } else {
                    startRecording();
                }
            }
        }.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runRecognizerSetup();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.removeListener(this);
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    /**
     * In partial result we get quick updates about current hypothesis. In
     * keyword spotting mode we can react here, in other modes we need to wait
     * for final result in onResult.
     */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        Toast.makeText(this, "Partial: " + text, Toast.LENGTH_SHORT).show();
        resultText.setText(text + "\n------------\n" + partialResult);
        Log.i(this.getClass().getName(), "Partial: " + text);

        if (partialResult.isEmpty()) {
            countWord(text);
        } else if (!partialResult.equalsIgnoreCase(text) && text.contains(partialResult)) {
            //[new word]   [old word]
            //[------text-----------]
            String newPhrase = text.substring(0, text.length() - partialResult.length()).trim();
            for (String word : newPhrase.split("\\s+")) {
                countWord(word);
            }
        }
        partialResult = text;
    }

    private void countWord(String word) {
        word = word.trim();

        if (fillerCount.containsKey(word)) {
            fillerCount.put(word, fillerCount.get(word) + 1);
        } else {
            fillerCount.put(word, 1);
        }
        makeText(this, "Count word: " + word + "=" + fillerCount.get(word), Toast.LENGTH_LONG).show();
    }

    private void displayFillers() {
        StringBuilder sb = new StringBuilder();

        for (String fillerWord : fillerCount.keySet()) {
            sb.append(fillerWord);
            sb.append("\t=\t");
            sb.append(fillerCount.get(fillerWord));
            sb.append("\n");
        }

        resultText.setText(sb.toString());
        Log.i(this.getClass().getName(), sb.toString());
        fillerCount.clear();
    }

    /**
     * Callback when SpeechRecognizer.stop() is called
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        resultText.setText("Ended");

        if (hypothesis != null) {
            displayFillers();

            String text = hypothesis.getHypstr();
            Log.i(this.getClass().getName(), "--------END RESULT------");
            Log.i(this.getClass().getName(), "Text[" + text + "]");
            Log.i(this.getClass().getName(), "------------------------");
            String[] words = text.split("\\s+");

            for (String word : words) {
                countWord(word);
            }

            displayFillers();
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        Toast.makeText(this, "Started", Toast.LENGTH_SHORT).show();
    }

    /**
     * We stop recognizer here to get a final result
     */
    @Override
    public void onEndOfSpeech() {
        ((TextView) findViewById(R.id.caption_text)).setText("Speech ended. Click on stop if you want to stop keyword search.");
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .getRecognizer();
        recognizer.addListener(this);

        /** In your application you might not need to add all those searches.
         * They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        File keywordListFile = new File(assetsDir, "keyword.list");
        recognizer.addKeywordSearch(KWS_SEARCH, keywordListFile);
    }

    @Override
    public void onError(Exception error) {
        ((TextView) findViewById(R.id.caption_text)).setText(error.getMessage());
    }

    @Override
    public void onTimeout() {
        stopRecording();
    }
}
