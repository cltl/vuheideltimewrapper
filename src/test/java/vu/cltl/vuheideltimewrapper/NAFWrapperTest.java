package vu.cltl.vuheideltimewrapper;

import eu.kyotoproject.kaf.KafSaxParser;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class NAFWrapperTest {
    private String mapping = "lib/alpino-to-treetagger.csv";

    @Test
    void testFileWithoutCreationTime() {
        KafSaxParser kafSaxParser = new KafSaxParser();
        kafSaxParser.parseFile("example/nofiledesc.in.naf");
        NAFWrapper wrapper = new NAFWrapper(kafSaxParser, mapping);

        Date documentCreationTime = wrapper.creationTime(false);
        assertNotNull(documentCreationTime);
        documentCreationTime = wrapper.creationTime(true);
        assertNull(documentCreationTime);

    }

}