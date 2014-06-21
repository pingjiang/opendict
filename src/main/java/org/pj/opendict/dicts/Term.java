package org.pj.opendict.dicts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
* Created by pingjiang on 14-6-21.
*/
public class Term {
    /**
     * You can add alternate words for one entry with | between 2 alternate words. e.g. good|well|best,
     * the main entry is good, well and best are alternate words.
     * What's Alternate word(s)?
     * When you input well or best, Lingoes will display explanation of itself(if any) and main word good.
     * The method can apply to tense, deformation, and synonym of a word.
     */
    private final List<String> words;
    private final String explain;

    public List<String> getWords() {
        return words;
    }

    public String getExplain() {
        return explain;
    }

    public Term(String termWord, String termExplain) {
        this.words = new ArrayList<String>();
        // 注意：这里是正则表达式，|是需要转义才能正确工作的。
        String[] parts = termWord.split("\\|");
        for (String part : parts) {
            this.words.add(part);
        }

        explain = termExplain;
    }
}
