package org.pj.opendict.dicts.lingoes;

import org.pj.opendict.dicts.DictOffset;
import org.pj.opendict.dicts.DictOffsetTable;
import org.pj.opendict.dicts.SensitiveStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
* Created by pingjiang on 14-6-20.
*/ // 读取解压后的文件
public class LingoesInflateDictReader {

    private static Logger logger = LoggerFactory.getLogger(LingoesInflateDictReader.class);

    // 默认编码为utf-8,还可以为UTF-16BE
    private SensitiveStringDecoder wordStringDecoder = new SensitiveStringDecoder(Charset.forName("UTF-8"));
    private SensitiveStringDecoder xmlStringDecoder = new SensitiveStringDecoder(Charset.forName("UTF-8"));

    private final ByteBuffer buffer;
    private int position = 0;
    private final int totalLength;
    private final int tableLength;
    private final int wordsLength;
    private final int xmlsLength;
    private final int wordsOffset;
    private final int xmlOffset;

    private final DictOffsetTable offsetTable = new DictOffsetTable();
    private final Map<String, String> dict = new HashMap<String, String>();

    public DictOffsetTable getOffsetTable() {
        return offsetTable;
    }

    public Map<String, String> getDict() {
        return dict;
    }

    public LingoesInflateDictReader(String filePath, int tableLength, int wordsLength, int xmlsLength) throws IOException {

        this.tableLength = tableLength;
        this.wordsLength = wordsLength;
        this.xmlsLength = xmlsLength;
        this.totalLength = this.tableLength + this.wordsLength + this.xmlsLength;

        this.wordsOffset = this.tableLength;
        this.xmlOffset = this.tableLength + this.wordsLength;

        try (RandomAccessFile file = new RandomAccessFile(filePath, "r"); final FileChannel fChannel = file.getChannel();) {
            buffer = ByteBuffer.allocate((int) fChannel.size());
            fChannel.read(buffer);
        }
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.rewind();

        readDeflate();
    }

    private DictOffset readDictOffset() {
        int wordOffset = buffer.getInt();
        int xmlOffset = buffer.getInt();
        byte flag = buffer.get();
        byte ref = buffer.get();

        return new DictOffset(wordOffset, xmlOffset, flag, ref);
    }

    private String readDictWord(DictOffset dictOffset, int refs) {
        int indexPos = wordsOffset + dictOffset.getWordOffset();
        int wordPos = indexPos + refs*4;

        int wordLength;// 应该忽略-（未知字段index）长度4
        if (dictOffset.getNext() == null) {
            wordLength = xmlOffset - dictOffset.getWordOffset() - refs*4;
        } else {
            wordLength = dictOffset.getWordLength() - refs*4;
        }
        System.out.println("read word: position= " + wordPos + ", length= " + wordLength);
        return new String(wordStringDecoder.decode(buffer.array(), wordPos, wordLength));
    }

    private String readXml(DictOffset dictOffset) {
        int xmlPos = xmlOffset + dictOffset.getXmlOffset();
        int xmlLength;
        if (dictOffset.getNext() == null) {
            xmlLength = totalLength - dictOffset.getXmlOffset();
        } else {
            xmlLength = dictOffset.getXmlLength();
        }
        System.out.println("read xml: position= " + xmlPos + ", length= " + xmlLength + ", offset= " + dictOffset);
        return new String(xmlStringDecoder.decode(buffer.array(), xmlPos, xmlLength));
    }

    private void constructOffsetTable() {
        final int tableSize = tableLength/DictOffset.bytes();
        //System.out.println("Construct index table size= " + tableSize);

        for (int i = 0; i < tableSize; i++) {
            DictOffset dictOffset = readDictOffset();
            offsetTable.add(dictOffset);
        }
    }

    private void readDeflate() {
        constructOffsetTable();

        // 这里应该如何来做？
        for (int i = 0; i < offsetTable.size() - 1; i++) {
            System.out.println("process i= " + i);
            DictOffset dictOffset = offsetTable.get(i);
            // 首先应该获取refs数
            int refs = dictOffset.getRefInt();
            String dictWord = readDictWord(dictOffset, refs);
            int wordPosBase = wordsOffset + dictOffset.getWordOffset();
            String xml = readXml(dictOffset);

            for (int j = 0; j < refs; j++) {
                int newIndex = buffer.getInt(wordPosBase + 4*j);
                DictOffset newOffset = offsetTable.get(newIndex);
                if (xml == null || xml.isEmpty()) {
                    xml = readXml(newOffset);
                } else {
                    xml = xml + ", " + readXml(newOffset);
                }
            }

            if (dictWord != null) {
                dict.put(dictWord, xml);
            }
        }
    }
}
