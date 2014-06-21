package org.pj.opendict.dicts.lingoes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Created by pingjiang on 14-6-20.
 *
 * <pre>
 * 灵格斯词典格式介绍:
 *
 * 基本信息:
 * - 词典的单词和翻译是保存在压缩数据块里面，使用索引数组来保存单词或释义在数据块中的相对位置。
 * - 索引表由4Byte的int数组表示，数组的每个int表示一个单词或释义的起始位置，可以通过前后两个数据计算出长度。
 * - 数值使用小端序（little endian byte order）。
 * - 单词和XML翻译数据使用UTF-8 or UTF-16LE 编码。
 *
 * LD2文件格式：
 * - File Header 文件头96字节
 * -- 4B 字典类型
 * -- 20B MD5或者校验和
 * -- 2B 主要版本号
 * -- 2B 次要版本号
 * -- 8B 编号
 * -- [-92B] 填充（-92B）的位置
 * -- [93B到96B]=4B 索引信息偏移地址
 * - File Description 文件描述（内容不确定）
 * - Additional Information (optional) 额外信息（从上面的`索引信息偏移地址`+96B开始）
 * -- 4B 字典类型
 * -- 4B 下面信息总长度偏移地址
 * -- 4B 索引表长度（也是压缩数据开始位置，计算方法是额外信息头28B后的位置+这个值）
 * -- 4B 解压后的单词索引长度
 * -- 4B 解压后的单词长度
 * -- 4B 解压后的xml翻译长度
 * -- 4B 未知整数
 * - 单词索引表（索引表长度为上面`索引表长度（也是压缩数据开始位置）`）
 * - 压缩数据块索引表
 * - Index Group (corresponds to definitions in dictionary) 释义索引数组？？？
 * - Deflated Dictionary Streams 压缩数据块（可以通过索引数组寻址数据块），可以Infator解压
 * -- Index Data 压缩数据块索引数组
 * --- Offsets of definitions 释义索引
 * --- Offsets of translations 翻译索引
 * --- Flags 标志
 * --- References to other translations 参考
 * -- Definitions 释义
 * -- Translations (xml) 翻译
 *
 * TODO: find encoding / language fields to replace auto-detect of encodings
 *
 * </pre>
 *
 */
public class LingoesDictReader {
    private static Logger logger = LoggerFactory.getLogger(LingoesDictReader.class);

    private static final int LENGTH_SHORT= 2;
    private static final int LENGTH_INT = 4;
    private static final int LENGTH_LONG = 8;

    private static final int LENGTH_HEADER = 96;
    private static final int LENGTH_TYPE = LENGTH_INT;
    private static final int LENGTH_CHECKSUM = 20;
    private static final int LENGTH_MAJOR_VERSION = LENGTH_SHORT;
    private static final int LENGTH_MINOR_VERSION = LENGTH_SHORT;
    private static final int LENGTH_ID = LENGTH_LONG;
    private static final int LENGTH_OFFSET = LENGTH_INT;

    private static final int LENGTH_COMPRESS_HEADER = LENGTH_INT*7;

    private final String filePath;
    private final ByteBuffer dataRawBytes;
    private int position = 0;

    /// 文件头定义

    /**
     * len=4,ASCII格式的文件类型，?LD2,?LDX等值
     */
    private String type;

    /**
     * len=20,不知道是什么内容？MD5或者checksum算法计算处理的值
     */
    private String checksum;

    /**
     * len=2,主要版本号
     */
    private short majorVersion;

    /**
     * len=2, 次要版本号
     */
    private short minorVersion;

    /**
     * len=8,Long类型的ID值
     */
    private String id;

    /**
     * len=56,填充到92字节, 还不知道什么内容
     */
    private String padding;

    /**
     * 索引信息偏移位置（相对于文件头96=0x60B的位置）
     */
    private int infoOffset;// len=4,+96(header length, except offset

    /**
     * 索引信息开始位置=0x60（文件头长度）+索引信息偏移位置
     */
    final int infoPosition;

    /// 索引信息部分

    /**
     * 3表示本地字典，其他表示网络字典;
     * 如果为3表示没有附加信息，直接从infoPosition开始读取
     */
    private int dictType;

    /**
     * 下面内容的总长度（相对于当前位置的结束位置）
     */
    private int withIndexOffset;

    /**
     * 压缩数据的结束位置
     */
    private int limit; // 自己加的字段

    /**
     * 索引表的长度（也是压缩数据的相对偏移地址{相对于信息头结束位置}
     */
    private int compressedDataOffset;

    /**
     * 单词个数=索引表长度/4
     * 因为索引表是int数组，每个int表示一个索引的起始地址
     */
    private int definitions; // 自己加字段

    /**
     * 压缩数据的起始地址，用于提取压缩数据块
     */
    private int offsetCompressedDataHeader; // 自己加字段

    /**
     * 解压后的索引表长度（单词索引是从解压文件的开始位置开始的，起始位置为0）
     */
    private int inflatWordsIndexLength; // 解压后索引长度

    /**
     * 解压后的单词长度(单词内容的起始地址为0+inflatWordsIndexLength）
     */
    private int inflatWordsLength;// 解压后单词长度

    /**
     * 解压后的xml翻译内容的长度(xml翻译内容的起始地址为inflatWordsIndexLength+inflatWordsLength)
     */
    private int inflatedXmlLength;// 解压后XML长度

    /**
     * 单词索引的开始位置
     */
    private int offsetIndex;// 自己加的字段

    // deflate,inflate
    // 索引数组
    private final List<Integer> definitionsArray = new ArrayList<Integer>();
    // 压缩数据块数组
    private final List<Integer> deflateStreams = new ArrayList<Integer>();
    //int flatOffset;


    public String getType() {
        return type;
    }

    public String getChecksum() {
        return checksum;
    }

    public short getMajorVersion() {
        return majorVersion;
    }

    public short getMinorVersion() {
        return minorVersion;
    }

    public String getVersion() {
        return String.format("%d.%d", majorVersion, minorVersion);
    }

    public String getId() {
        return id;
    }

    public int getInfoOffset() {
        return infoOffset;
    }

    public int getDictType() {
        return dictType;
    }

    public int getWithIndexOffset() {
        return withIndexOffset;
    }

    public int getCompressedDataOffset() {
        return compressedDataOffset;
    }

    public int getInflatWordsIndexLength() {
        return inflatWordsIndexLength;
    }

    public int getInflatWordsLength() {
        return inflatWordsLength;
    }

    public int getInflatedXmlLength() {
        return inflatedXmlLength;
    }

    public int getInfoPosition() {
        return infoPosition;
    }

    public int getLimit() {
        return limit;
    }

    public int getDefinitions() {
        return definitions;
    }

    public int getOffsetCompressedDataHeader() {
        return offsetCompressedDataHeader;
    }

    public int getOffsetIndex() {
        return offsetIndex;
    }

    public LingoesDictReader(String filePath) throws IOException {
        this.filePath = filePath;

        try (RandomAccessFile file = new RandomAccessFile(filePath, "r"); final FileChannel fChannel = file.getChannel();) {
            dataRawBytes = ByteBuffer.allocate((int) fChannel.size());
            fChannel.read(dataRawBytes);
        }
        dataRawBytes.order(ByteOrder.LITTLE_ENDIAN);
        dataRawBytes.rewind();

        readHeader();
        infoPosition = position;

        assert (dataRawBytes.limit() > infoPosition);
        int type = dataRawBytes.getInt(infoPosition);

        // 暂时只能处理type=3的情况
        //int compressedDataOffset = dataRawBytes.getInt(infoPosition + 4);

        // 这里是一个索引列表
        //int compressedDataStart = compressedDataOffset + infoPosition + LENGTH_INT + LENGTH_INT;//?
        //assert dataRawBytes.limit() > (compressedDataStart - LENGTH_COMPRESS_HEADER);
        //withInfoPosition = position + unknown1;

        if (type == 3) {
            readDictionary(infoPosition);
        } else {
            throw new IOException(String.format("Dictionary type %d is not supported yet", type));
        }

        buildDefinitionsArray();

        deflateFile();
    }

    private void readHeader() throws UnsupportedEncodingException {
        type = new String(dataRawBytes.array(), position, LENGTH_TYPE, "ASCII");
        position += LENGTH_TYPE;

        checksum = new String(dataRawBytes.array(), position, LENGTH_CHECKSUM, "ASCII");
        position += LENGTH_CHECKSUM;

        majorVersion = dataRawBytes.getShort(position);
        position += LENGTH_MAJOR_VERSION;

        minorVersion = dataRawBytes.getShort(position);
        position += LENGTH_MINOR_VERSION;

        id = Long.toHexString(dataRawBytes.getLong(position));
        position += LENGTH_ID;

        int paddingLength = (LENGTH_HEADER - position - 4);
        padding = new String(dataRawBytes.array(), position, paddingLength, "ASCII");
        position += paddingLength;

        infoOffset = dataRawBytes.getInt(position);
        position += (LENGTH_OFFSET + infoOffset);// position = (LENGTH_HEADER + infoOffset);
    }

    /**
     * INFO=28B
     * type|limit|index_length|word_index_length|words|xml_length|total?
     *
     * @param startPosition
     */
    private void readDictionary(int startPosition) {
        position = startPosition;
        offsetIndex = position + LENGTH_COMPRESS_HEADER;

        // 词典类型3
        dictType = dataRawBytes.getInt(position);
        position += LENGTH_INT;

        // 下面内容总长度(相对于当前位置结束位置）能够计算出压缩数据的结束位置
        withIndexOffset = dataRawBytes.getInt(position);
        // 压缩数据的结束位置
        limit = (position + withIndexOffset);
        position += LENGTH_INT;

        // 索引长度（或者说是压缩头开始偏移位置【相对于索引开始位置】）
        // 索引内容是int数组，所以索引个数=索引长度/4
        // 这里可以计算出索引个数definitions,根据这个可以计算出索引数组
        compressedDataOffset = dataRawBytes.getInt(position);
        definitions = compressedDataOffset/LENGTH_INT;
        offsetCompressedDataHeader = compressedDataOffset + offsetIndex;
        position += LENGTH_INT;

        // 索引单词长度
        inflatWordsIndexLength = dataRawBytes.getInt(position);
        position += LENGTH_INT;

        // 单词数
        inflatWordsLength = dataRawBytes.getInt(position);
        position += LENGTH_INT;

        // xml数
        inflatedXmlLength = dataRawBytes.getInt(position);
        position += LENGTH_INT;
    }

    private void buildDefinitionsArray() {
        for (int i =0; i < definitions; i++) {
            int val = dataRawBytes.getInt(offsetIndex + i*LENGTH_INT);
            definitionsArray.add(val);
        }
    }

    private void deflateFile() {
        // 跳过前面两个整数
        position = (offsetCompressedDataHeader + LENGTH_INT + LENGTH_INT);
        dataRawBytes.position(position);

        // 读取第一个整数，为0，可以直接丢掉
        // 因为块长度为下一个值-前一个值。处理第一个块的时候没有前一个值，所以不好处理
        // 但是如果我们丢掉这个值后，我们处理的是第二个块，前一个值为0
        int flatOffset = dataRawBytes.getInt();

        // 将偏移地址和当前位置比较，不能操作limit的位置
        // limit后的内容，还不知道是什么
        while ((flatOffset + dataRawBytes.position()) < limit) {
            flatOffset = dataRawBytes.getInt();
            deflateStreams.add(Integer.valueOf(flatOffset));
        }
    }

    public void decompress(String inflatedFilePath) throws IOException {
        // 索引读完了就到了数据块blocks
        // 索引内容是int数组，内容是记录了块的开始地址和结束地址
        // 块长度=数组下一个值-当前值

        // 因为前面索引部分读完了，这里就是blocks数据块开始位置了
        // 索引内容的偏移地址都是相对于这个地址的
        // 0表示前一个数据块偏移地址
        final int startOffset = dataRawBytes.position() + 0;
        int offset = -1;
        int lastOffset = startOffset;
        //boolean append = false;

        //BufferedWriter writer = Files.newBufferedWriter(Paths.get(filePath + ".inflated"), Charset.defaultCharset(), append ? StandardOpenOption.APPEND : StandardOpenOption.WRITE);
        //final String inflatedFilePath = filePath + ".inflated";
        //final BufferedWriter writer = Files.newBufferedWriter(Paths.get(inflatedFilePath), Charset.defaultCharset());
        final FileOutputStream out = new FileOutputStream(inflatedFilePath);
        final Inflater inflator = new Inflater();
        final byte[] buffer = new byte[1024 * 8];
        int len = -1;

        // 这里处理的一个技巧是，从第二个值开始处理，第一个值肯定是0
        for (final Integer offsetRelative : deflateStreams) {
            // 下一个数据块的offset
            offset = startOffset + offsetRelative.intValue();
            inflator.reset();

            try (final InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(dataRawBytes.array(),
                    lastOffset, offset - lastOffset), inflator, 1024 * 8)) {
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }

            lastOffset = offset;
        }

        out.flush();
        out.close();
    }

    private static final void writeInputStream(final InputStream in, final OutputStream out) throws IOException {
        final byte[] buffer = new byte[1024 * 8];
        int len;
        while ((len = in.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
    }

    /**
     * 根据解压后的数据导出字典内容
     *
     * @param inflatedFilePath 解压后的文件路径
     * @throws IOException
     */
    public void export(String inflatedFilePath) throws IOException {
        LingoesInflateDictReader inflateDictReader = new LingoesInflateDictReader(inflatedFilePath, inflatWordsIndexLength, inflatWordsLength, inflatedXmlLength);
        for (Map.Entry<String, String> entry : inflateDictReader.getDict().entrySet()) {
            logger.debug("Dictionary: {}={}", entry.getKey(), entry.getValue());
        }
    }
}
