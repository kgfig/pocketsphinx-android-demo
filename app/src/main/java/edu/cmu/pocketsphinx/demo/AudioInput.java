package edu.cmu.pocketsphinx.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import edu.cmu.pocketsphinx.Config;
import edu.cmu.pocketsphinx.Decoder;
import edu.cmu.pocketsphinx.Segment;

import static junit.framework.Assert.fail;

/**
 * Created by tictocproject on 07/09/2017.
 */

public class AudioInput {

    public AudioInput() {
        Config c = Decoder.defaultConfig();
        c.setString("-hmm", "../../model/en-us/en-us");
        c.setString("-lm", "../../model/en-us/en-us.lm.dmp");
        c.setString("-dict", "../../model/en-us/cmudict-en-us.dict");
        Decoder d = new Decoder(c);

        URL testwav;
        FileInputStream stream;

        try {
            testwav = new URL("file:../../test/data/goforward.wav");
            stream = new FileInputStream(new File(testwav.getPath()));
            d.startUtt();
            byte[] b = new byte[4096];
            int nbytes;
            while ((nbytes = stream.read(b)) >= 0) {
                ByteBuffer bb = ByteBuffer.wrap(b, 0, nbytes);

                // Not needed on desktop but required on android
                bb.order(ByteOrder.LITTLE_ENDIAN);

                short[] s = new short[nbytes / 2];
                bb.asShortBuffer().get(s);
                d.processRaw(s, nbytes / 2, false, false);
            }
        } catch (IOException e) {
            fail("Error when reading goforward.wav" + e.getMessage());
        }
        d.endUtt();
        System.out.println(d.hyp().getHypstr());
        for (Segment seg : d.seg()) {
            System.out.println(seg.getWord());
        }
    }

}

