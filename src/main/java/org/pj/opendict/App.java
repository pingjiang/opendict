package org.pj.opendict;

import org.pj.opendict.dicts.lingoes.LingoesDictReader;
import sun.misc.BASE64Decoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 *
 */
public class App 
{
    private static Logger logger = LoggerFactory.getLogger(App.class);

    private static final String LD2 = "/Users/pingjiang/Downloads/Oxford Advanced Learner's Dictionary.ld2";

    public static void main( String[] args ) {
        if (args.length == 0) {
            logger.error("Usage: PROGRAM path/to/ld2/file [inflated file path]");
            return;
        }

        try {
            LingoesDictReader dictReader = new LingoesDictReader(args[0]);
            logger.debug("Read file {} successfully", args[0]);

            String inflatedFilePath = (args.length > 1 ? args[1] : (args[0] + ".inflated"));
            dictReader.decompress(inflatedFilePath);

            dictReader.export(inflatedFilePath);

            logger.debug("Export successfully");
        } catch (IOException e) {
            logger.error("Process exception: {}", e);
        }
    }
}
