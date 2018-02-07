package com.github.reader.pdf.model;

/**
 * Created by qiaodong on 17-12-4.
 */

public class PassClickResult {
    public final boolean changed;

    public PassClickResult(boolean _changed) {
        changed = _changed;
    }

    public void acceptVisitor(PassClickResultVisitor visitor) {
    }
}
