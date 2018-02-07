package com.github.reader.pdf.model;


/**
 * Created by qiaodong on 17-12-4.
 */

public class PassClickResultText extends PassClickResult {
    public final String text;

    public PassClickResultText(boolean _changed, String _text) {
        super(_changed);
        text = _text;
    }

    public void acceptVisitor(PassClickResultVisitor visitor) {
        visitor.visitText(this);
    }
}
