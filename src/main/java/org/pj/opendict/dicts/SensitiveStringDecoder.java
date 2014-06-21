package org.pj.opendict.dicts;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.Arrays;

/**
* Created by pingjiang on 14-6-20.
*/
public class SensitiveStringDecoder {
    public final String name;
    private final CharsetDecoder cd;

    public SensitiveStringDecoder(final Charset cs) {
        this.cd = cs.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        this.name = cs.name();
    }

    public char[] decode(final byte[] ba, final int off, final int len) {
        final int en = (int) (len * (double) this.cd.maxCharsPerByte());
        final char[] ca = new char[en];
        if (len == 0) {
            return ca;
        }
        this.cd.reset();
        final ByteBuffer bb = ByteBuffer.wrap(ba, off, len);
        final CharBuffer cb = CharBuffer.wrap(ca);
        try {
            CoderResult cr = this.cd.decode(bb, cb, true);
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
            cr = this.cd.flush(cb);
            if (!cr.isUnderflow()) {
                cr.throwException();
            }
        } catch (final CharacterCodingException x) {
            // Substitution is always enabled,
            // so this shouldn't happen
            throw new Error(x);
        }
        return SensitiveStringDecoder.safeTrim(ca, cb.position());
    }

    private static char[] safeTrim(final char[] ca, final int len) {
        if (len == ca.length) {
            return ca;
        } else {
            return Arrays.copyOf(ca, len);
        }
    }
}
