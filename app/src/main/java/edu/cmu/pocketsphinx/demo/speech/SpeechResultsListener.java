package edu.cmu.pocketsphinx.demo.speech;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

/**
 * Created by tictocproject on 12/09/2017.
 */

public class SpeechResultsListener implements RecognitionListener {

    private static final String TAG = SpeechResultsListener.class.getSimpleName();

    private static final String KEYWORD_SEARCH_NAME = "fillers";
    private static final String ACOUSTIC_MODEL = "en-us-ptm";
    private static final String DICTIONARY = "cmudict-en-us.dict";
    private static final String FILLER_LIST = "keyword.list";

    private SpeechActivity activity;
    private File assetsDir;

    private SpeechRecognizer recognizer;
    private String partialResult;
    private Map<String, Integer> fillerCount;

    public SpeechResultsListener(SpeechActivity activity, File assetsDir) {
        this.activity = activity;
        this.assetsDir = assetsDir;

        partialResult = "";
        fillerCount = new HashMap<>();
    }

    // The recognizer can be configured to perform multiple searches of different kind and switch between them
    // .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
    public void setupRecognizer() throws IOException {
        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, ACOUSTIC_MODEL))
                .setDictionary(new File(assetsDir, DICTIONARY))
                .getRecognizer();
        recognizer.addListener(this);

        File keywordListFile = new File(assetsDir, FILLER_LIST);
        recognizer.addKeywordSearch(KEYWORD_SEARCH_NAME, keywordListFile);
    }

    public void start() {
        if (recognizer != null) {
            recognizer.stop();
            recognizer.startListening(KEYWORD_SEARCH_NAME);
        }
    }

    public void stop() {
        if (recognizer != null) {
            recognizer.stop();
        }
    }

    public void destroy() {
        if (recognizer != null) {
            recognizer.removeListener(this);
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onEndOfSpeech() {

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
        Log.i(this.getClass().getName(), "Partial: " + text);

        if (partialResult.isEmpty()) {
            countWord(text);
        } else if (!partialResult.equalsIgnoreCase(text) && text.contains(partialResult)) {
            //[new word]   [old word]
            //[------text-----------]
            String newPhrase = text.substring(0, text.length() - partialResult.length()).trim();
            countWord(newPhrase);
            /*for (String word : newPhrase.split("\\s+")) {
                countWord(word);
            }*/
        }
        partialResult = text;
    }

    /**
     * Callback when SpeechRecognizer.stop() is called
     */
    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            activity.showResult(tallyResults());
            clearResults();

            String text = hypothesis.getHypstr();
            Log.i(TAG, "--------END RESULT------");
            Log.i(TAG, "Text[" + text + "]");
            Log.i(TAG, "------------------------");
            String[] words = text.split("\\s+");

            for (String word : words) {
                countWord(word);
            }

            tallyResults();
            activity.showResult(tallyResults());
            clearResults();
        }
    }

    @Override
    public void onError(Exception e) {
        activity.showError(e);
    }

    @Override
    public void onTimeout() {
        recognizer.stop();
    }

    private void countWord(String word) {
        word = word.trim();

        if (fillerCount.containsKey(word)) {
            fillerCount.put(word, fillerCount.get(word) + 1);
        } else {
            fillerCount.put(word, 1);
        }
        Log.i(TAG, "Count word: " + word + "=" + fillerCount.get(word));
    }

    private void clearResults() {
        partialResult = "";
        fillerCount.clear();
    }

    private String tallyResults() {
        StringBuilder sb = new StringBuilder();

        for (String fillerWord : fillerCount.keySet()) {
            sb.append(fillerWord);
            sb.append("\t=\t");
            sb.append(fillerCount.get(fillerWord));
            sb.append("\n");
        }

        Log.i(this.getClass().getName(), sb.toString());
        return sb.toString();
    }

    public interface SpeechActivity {
        void showError(Exception e);

        void showResult(String result);
    }

}
