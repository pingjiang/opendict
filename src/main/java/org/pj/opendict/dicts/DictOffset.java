package org.pj.opendict.dicts;

/**
 * Created by pingjiang on 14-6-20.</br>
 *
 * 字典数据偏移地址数据,总共10Byte</br>
 *
 * <pre>
 *     单词偏移地址|XML偏移地址|flag|ref
 *     ----4B----|---4B-----|-1B-|-1B-
 * </pre>
 *
 */
public class DictOffset {
    private final int wordOffset;
    private final int xmlOffset;
    private final byte flag;
    private final byte ref;

    private DictOffset prev = null;
    private DictOffset next = null;

    public DictOffset(int wordOffset, int xmlOffset, byte flag, byte ref) {
        this.wordOffset = wordOffset;
        this.xmlOffset = xmlOffset;
        this.flag = flag;
        this.ref = ref;
    }

    public int getWordOffset() {
        return wordOffset;
    }

    public int getXmlOffset() {
        return xmlOffset;
    }

    public int getWordLength() {
        return next.getWordOffset() - getWordOffset();
    }

    public int getXmlLength() {
        return next.getXmlOffset() - getXmlOffset();
    }

    public byte getFlag() {
        return flag;
    }

    public byte getRef() {
        return ref;
    }

    public int getRefInt() {
        return getRef() & 0xFF;
    }

    public DictOffset getPrev() {
        return prev;
    }

    public DictOffset getNext() {
        return next;
    }

    public void setPrev(DictOffset prev) {
        this.prev = prev;
    }

    public void setNext(DictOffset next) {
        this.next = next;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DictOffset that = (DictOffset) o;

        if (flag != that.flag) return false;
        if (ref != that.ref) return false;
        if (wordOffset != that.wordOffset) return false;
        if (xmlOffset != that.xmlOffset) return false;
        if (next != null ? !next.equals(that.next) : that.next != null) return false;
        if (prev != null ? !prev.equals(that.prev) : that.prev != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = wordOffset;
        result = 31 * result + xmlOffset;
        result = 31 * result + (int) flag;
        result = 31 * result + (int) ref;
        result = 31 * result + (prev != null ? prev.hashCode() : 0);
        result = 31 * result + (next != null ? next.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DictOffset{" +
                "wordOffset=" + wordOffset +
                ", xmlOffset=" + xmlOffset +
                ", flag=" + flag +
                ", ref=" + ref +
                '}';
    }

    /**
     * 每个索引数据占用10字节
     * @return 索引数据字节数
     */
    public static int bytes() {
        return 10;
    }
}
