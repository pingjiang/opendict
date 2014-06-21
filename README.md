# OpenDict 词典工具
-----------------
收集和分析常见词典文件，解析词典文件，导出词典数据和生成词典文件。

## 实现的词典格式
* Lingoes词典LD2格式的解析和导出。
* StartDict词典编辑器。

## Lingoes词典
--------------

Lingoes是一个非常小巧但是强大的词典，有很多的词典文件。但是因为其封闭的词典格式，并且没有任何详细的官方的格式说明文档，所以其它词典无法运用到Lingoes丰富的词典资源。

这里我参考了Xiaoyun Zhu分析的的[lingoes-convert](http://code.google.com/p/lingoes-extractor/)源代码。但是因为代码阅读起来比较困难，即便是有源代码也无法快速的了解词典的格式，所以我重新的整理了一份代码，目的在于学习Lingoes的LD2词典格式。

### 基本信息
 * 词典的单词和翻译是保存在压缩数据块里面，使用索引数组来保存单词或释义在数据块中的相对位置。
 * 索引表由4Byte的int数组表示，数组的每个int表示一个单词或释义的起始位置，可以通过前后两个数据计算出长度。
 * 数值使用小端序（little endian byte order）。
 * 单词和XML翻译数据使用`UTF-8`或`UTF-16LE`编码。
 
 使用SynalizeIt分析的LD2文件的结构：
 
 LD2-Format-SynalyzeIt-Tree.png
 
 使用SynalizeIt分析的LD2解压后文件的结构：
 
 LD2-Inflated-Format-SynalyzeIt-Tree.png
 
 
### 使用方法
 

### 注意事项
LD2文件格式现在只是分析出了单词和翻译的内容，还没能完全分析完所有内容，所以还仅仅只能导出词典里面的数据，还不能编辑词典文件。后面期待作者将软件开源，能够移植到Linux和Mac上。
 
 