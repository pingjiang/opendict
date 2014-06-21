package org.pj.opendict;

import junit.framework.TestCase;
import org.pj.opendict.dicts.lingoes.LingoesDictReader;

import java.nio.file.Path;
import java.nio.file.Paths;

public class LingoesDictReaderTest extends TestCase {

    private static final Path DICT_PATH = Paths.get(ClassLoader.getSystemResource("dict.ld2").getPath());
    LingoesDictReader dictReader;

    public void setUp() throws Exception {
        super.setUp();

        dictReader = new LingoesDictReader(DICT_PATH.toString());
    }

    public void tearDown() throws Exception {
        dictReader = null;
    }

    public void testGetDictType() throws Exception {
        assertEquals(6448, dictReader.getInfoOffset());
        assertEquals(0x1990, dictReader.getInfoPosition());
        assertEquals(3, dictReader.getDictType());
        assertEquals(5967460, dictReader.getLimit());
        assertEquals(136580, dictReader.getCompressedDataOffset());
    }
}