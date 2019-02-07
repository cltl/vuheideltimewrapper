package vu.cltl.vuheideltimewrapper;

import de.unihd.dbs.heideltime.standalone.exceptions.DocumentCreationTimeMissingException;
import eu.kyotoproject.kaf.KafSaxParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VuNafHeideltimeTest {

    @Test
    void testMissingCreationTime() {
        KafSaxParser kafSaxParser = new KafSaxParser();
        kafSaxParser.parseFile("example/nofiledesc.in.naf");

        VuNafHeideltime timeRequiresCreationTime = new VuNafHeideltime("nl",
                "lib/alpino-to-treetagger.csv", "conf/config.props", false);
        assertThrows(DocumentCreationTimeMissingException.class, () -> {timeRequiresCreationTime.process(kafSaxParser);});

        VuNafHeideltime timeAcceptsMissingCreationTime = new VuNafHeideltime("nl",
                "lib/alpino-to-treetagger.csv", "conf/config.props", true);
        assertDoesNotThrow(() -> {timeAcceptsMissingCreationTime.process(kafSaxParser);});

    }
}