package org.pj.opendict.dicts;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pingjiang on 14-6-20.</br>
 *
 * 解压后的字典数据文件索引表</br>
 *
 * 索引表由一个10Byte的数组组成，最后一个索引内容不能用，只能作为前一个索引的参考。
 *
 * @see DictOffset 索引表的内容
 */
public class DictOffsetTable {
    private final List<DictOffset> data = new ArrayList<DictOffset>();
    private DictOffset last = null;

    /**
     * 从索引表里面增加一个索引，同时设置好prev, next指针
     * @param dictOffset
     */
    public void add(DictOffset dictOffset) {
        if (last != null) {
            last.setNext(dictOffset);
            dictOffset.setPrev(last);
        }
        last = dictOffset;
        data.add(dictOffset);
    }

    public int size() {
        return data.size();
    }

    public DictOffset get(int i) {
        return data.get(i);
    }
}
