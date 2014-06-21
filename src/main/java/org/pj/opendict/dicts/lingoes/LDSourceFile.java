package org.pj.opendict.dicts.lingoes;

import org.pj.opendict.dicts.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by pingjiang on 14-6-21.
 *
 * Reference: http://www.lingoes.net/en/dictionary/dict_format.php
 *
 * The LDF dictionary source file contains the terms (glossary) and their definitions comprising the dictionary.
 * The main body of the dictionary source text file consists of a collection of entries called terms.
 * These are the words and phrases defined in the dictionary. The term is the primary key (index) of the dictionary.
 * it is stored with Unicode text.
 *
 */
public class LDSourceFile {
    private static Logger logger = LoggerFactory.getLogger(LDSourceFile.class);

    public static final String EXT = ".ldf";

    private String title;
    private String description;
    private String author;
    private String email;
    private String website;
    private String copyright;

    List<Term> terms = new ArrayList<Term>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getCopyright() {
        return copyright;
    }

    public void setCopyright(String copyright) {
        this.copyright = copyright;
    }

    public List<Term> getTerms() {
        return terms;
    }

    public Term searchTerm(String word) {
        for (Term term : terms) {
            if (term.getWords().contains(word)) {
                return term;
            }
        }

        return null;
    }

    /**
     * 读取一个IDF文件，然后解析其中的单词和释义
     *
     * @param filePath IDF文件路径
     * @throws IOException
     */
    public LDSourceFile(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath), Charset.forName("UTF-8"));
        readLines(lines);
    }

    /**
     * @see #write(String, String)
     *
     * @param filePath
     * @throws IOException
     */
    public void write(String filePath) throws IOException {
        write(filePath, "UTF-8");
    }

    /**
     * 将单词和释义按照IDF定义的格式写入文件
     *
     * @param filePath 写入文件
     * @param charset 字符集，默认是UTF-8
     * @throws IOException
     */
    public void write(String filePath, String charset) throws IOException {
        // BufferedOutputStream buffer = new BufferedOutputStream(new FileOutputStream(filePath), 4096);
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath), Charset.forName(charset));
        writer.write("###Title: " + getTitle());
        writer.newLine();
        writer.write("###Description: " + getDescription());
        writer.newLine();
        writer.write("###Author: " + getAuthor());
        writer.newLine();
        writer.write("###Email: " + getEmail());
        writer.newLine();
        writer.write("###Website: " + getWebsite());
        writer.newLine();
        writer.write("###Copyright: " + getCopyright());
        writer.newLine();
        writer.newLine();

        for (Term term : terms) {
            writer.write(join(term.getWords(), "|"));
            writer.write(term.getExplain());
            writer.newLine();
        }

        writer.flush();
        writer.close();
    }

    private static <T> String join(List<T> items, String delimiter) {
        if (items.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (T item : items) {
            if (!first) {
                sb.append(delimiter);
            } else {
                first = false;
            }

            sb.append(item);
        }

        return sb.toString();
    }

    private static String readHeadInfo(String line) {
        int pos = line.indexOf(':');
        if (pos != -1) {
            return line.substring(pos + 1).trim();
        }

        return "";
    }

    private void readLines(List<String> lines) throws IOException {
        int state = 0;// empty
        int i = 0;
        if (lines.size() < 6) {
            throw new IOException("Too short, lines is less than 6");
        }

        setTitle(readHeadInfo(lines.get(i++)));
        setDescription(readHeadInfo(lines.get(i++)));
        setAuthor(readHeadInfo(lines.get(i++)));
        setEmail(readHeadInfo(lines.get(i++)));
        setWebsite(readHeadInfo(lines.get(i++)));
        setCopyright(readHeadInfo(lines.get(i++)));

        String termWord = null;
        String termExplain = null;
        final String[] STATE_NAMES = { "EMPTY", "WORD", "EXPLAIN" };

        while (i < lines.size()) {
            String line = lines.get(i);
            System.out.println("state: " + STATE_NAMES[state] + ", line= " + i + ", content= " + line);
            switch (state) {
                case 0: // 支持前面有多个空行
                {
                    if (!line.isEmpty()) {
                        state = 1;
                    } else {
                        i += 1;
                    }
                    break;
                }
                case 1: // 解析word
                {
                    termWord = line;
                    i += 1;
                    state = 2;
                    break;
                }
                case 2: // 解析explain
                {
                    termExplain = line;
                    terms.add(new Term(termWord, termExplain));
                    i += 1;
                    state = 0;
                    break;
                }
                default:
                {
                    throw new IllegalStateException("state is " + state);
                }
            }
        }
    }

    /**
     * 转义HTML
     *
     * <table>
     *     <tr>
     *         <td>Entity</td><td>HTML</td>
     *     </tr>
     *     <tr>
     *         <td>\n</td><td>&lt;/br&gt;</td>
     *     </tr>
     *     <tr>
     *         <td>&amp;lt;</td><td>&lt;</td>
     *     </tr>
     *     <tr>
     *         <td>&amp;gt;</td><td>&gt;</td>
     *     </tr>
     *     <tr>
     *         <td>& amp;</td><td>&amp;</td>
     *     </tr>
     * </table>
     *
     * @param html
     * @return
     */
    public String escapeHTML(String html) {
        return html;
    }
}
