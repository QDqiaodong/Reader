package com.github.reader.pdf.model;


/**
 * 指示,点击PDF的区域
 * Nothing:空白
 * Widget:
 * BaseAnnotation:有高亮,下划线,删除先,墨迹的区域
 */

public enum Hit {
    Nothing, Widget, Annotation
}
