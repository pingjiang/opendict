package org.pj.opendict;

import junit.framework.TestCase;
import org.pj.opendict.dicts.lingoes.LDSourceFile;

public class LDSourceFileTest extends TestCase {

    LDSourceFile ldf;

    public void setUp() throws Exception {
        super.setUp();

        ldf = new LDSourceFile(ClassLoader.getSystemResource("demo.ldf").getPath());
    }

    public void testSourceFile() throws Exception {
        assertEquals("Lingoes English Dictionary", ldf.getTitle());

        assertEquals("Lingoes English Dictionary provides common English words, phrases, idioms, abbreviations and professional terms.", ldf.getDescription());
        assertEquals("Kevin", ldf.getAuthor());
        assertEquals("kevin-yau@msn.com", ldf.getEmail());
        assertEquals("http://www.lingoes.cn/", ldf.getWebsite());
        assertEquals("Copyright © 2007 Lingoes Project", ldf.getCopyright());

        assertTrue(ldf.getTerms().size() == 4);

        assertTrue(ldf.searchTerm("well") != null);

        //ldf.write("demo-out.ldf");
    }
}