package vu.cltl.vuheideltimewrapper;


import eu.kyotoproject.kaf.KafSaxParser;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static vu.cltl.vuheideltimewrapper.CLI.loadConfig;
import static vu.cltl.vuheideltimewrapper.CLI.processNafFile;

public class CLITest {
    String inputFile = "example/wikinews_1173_nl.input.naf";
    String outputFile = "example/out/wikinews_1173_nl.input.naf";
    String config = "conf/wrapper.props";
    Properties props = loadConfig(config);

    @Test
    public void testRun() {
        processNafFile(inputFile, outputFile, false, props);
        Path path = Paths.get(outputFile);
        assertTrue(Files.exists(path));
    }

    @Test
    public void testReplace() {
        processNafFile(inputFile, outputFile, false, props);
        KafSaxParser parser = new KafSaxParser();
        parser.parseFile(outputFile);
        int timexSize = parser.kafTimexLayer.size();

        processNafFile(outputFile, outputFile, true, props);
        parser = new KafSaxParser();
        parser.parseFile(outputFile);
        int timexSizeReplaced = parser.kafTimexLayer.size();
        assertEquals(timexSize, timexSizeReplaced);

        processNafFile(outputFile, outputFile, false, props);
        parser = new KafSaxParser();
        parser.parseFile(outputFile);
        int timexSizeAdded = parser.kafTimexLayer.size();
        assertEquals(timexSizeAdded, 2 * timexSize);

    }
}