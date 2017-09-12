package edu.cmu.pocketsphinx.demo;

import android.content.Context;
import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Config;
import edu.cmu.pocketsphinx.Decoder;
import edu.cmu.pocketsphinx.Segment;

import static junit.framework.Assert.fail;

/**
 * https://stackoverflow.com/questions/29008111/give-a-file-as-input-to-pocketsphinx-on-android
 */

public class FileInputSpeechRecognizer {

    Context context;

    public FileInputSpeechRecognizer(Context context) {
        this.context = context;
    }

    //statically load our library
    static {
        System.loadLibrary("pocketsphinx_jni");
    }

    //convert an inputstream to text
    private void convertToSpeech(final InputStream stream){
        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(context);
                    File assetsDir = assets.syncAssets();
                    Config c = Decoder.defaultConfig();
                    c.setString("-hmm", new File(assetsDir, "en-us-ptm").getPath());
                    c.setString("-dict", new File(assetsDir, "cmudict-en-us.dict").getPath());
                    c.setBoolean("-allphone_ci", true);
                    c.setString("-lm", new File(assetsDir, "en-phone.dmp").getPath());
                    Decoder d = new Decoder(c);

                    d.startUtt();
                    byte[] b = new byte[4096];
                    try {
                        int nbytes;
                        while ((nbytes = stream.read(b)) >= 0) {
                            ByteBuffer bb = ByteBuffer.wrap(b, 0, nbytes);

                            // Not needed on desktop but required on android
                            bb.order(ByteOrder.LITTLE_ENDIAN);

                            short[] s = new short[nbytes/2];
                            bb.asShortBuffer().get(s);
                            d.processRaw(s, nbytes/2, false, false);
                        }
                    } catch (IOException e) {
                        fail("Error when reading inputstream" + e.getMessage());
                    }
                    d.endUtt();
                    System.out.println(d.hyp().getHypstr());
                    for (Segment seg : d.seg()) {
                        //do something with the result here
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
