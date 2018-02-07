package com.github.reader.app.model.manager;

import android.graphics.RectF;

import com.github.reader.pdf.model.TextWord;
import com.github.reader.utils.LogUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class TextSelector {
    private static final String TAG = "TextSelector";
    final private TextWord[][] mText;
    final private RectF mSelectBox;

    /**
     *
     * @param text　　当前页面全部文字
     * @param selectBox　用户在屏幕上选择的区域，对应到真实文档所对应的区域
     */
    public TextSelector(TextWord[][] text, RectF selectBox) {
        mText = text;
        mSelectBox = selectBox;
    }

    public void select(TextProcessor tp) {
        if (mText == null || mSelectBox == null)
            return;

//---qiaodong ---start--------------
        for (int i = 0;i<mText.length;i++){
            for(int j = 0;j<mText[i].length;j++){
                LogUtils.d(TAG,"mText===="+mText[i][j].w+"i="+i+" j="+0);
            }
        }
//----qiaodong ----end-------

        ArrayList<TextWord[]> lines = new ArrayList<TextWord[]>();
        //1.循环判断每一行，是否在用户选择的区域,如果在则加入到集合中
        for (TextWord[] line : mText) {
            if (line[0].bottom > mSelectBox.top && line[0].top < mSelectBox.bottom) {
                lines.add(line);
            }
        }
        //2,细分第一行，最后一行．　此处感觉没必要分这么清楚
        Iterator<TextWord[]> it = lines.iterator();
        while (it.hasNext()) {
            TextWord[] line = it.next();
            boolean firstLine = line[0].top < mSelectBox.top;
            boolean lastLine = line[0].bottom > mSelectBox.bottom;
            float start = Float.NEGATIVE_INFINITY;
            float end = Float.POSITIVE_INFINITY;
            LogUtils.d(TAG,"line==="+ Arrays.toString(line)+" firstLine="+firstLine+" lastLine="+lastLine+" line[0].top="+line[0].top
                    +" line[0].bottom="+line[0].bottom+" mSelectBox.top="+mSelectBox.top+" mSelectBox.bottom="+mSelectBox.bottom+" mSelectBox.left"+mSelectBox.left
                    +" mSelectBox.right="+mSelectBox.right
            );
            if (firstLine && lastLine) {
                start = Math.min(mSelectBox.left, mSelectBox.right);
                end = Math.max(mSelectBox.left, mSelectBox.right);
            } else if (firstLine) {
                start = mSelectBox.left;
            } else if (lastLine) {
                end = mSelectBox.right;
            }

            tp.onStartLine();

            //最后要绘制的区域，一定是Text所在的区域．
            for (TextWord word : line) {
                LogUtils.d(TAG,"select text word="+word.w+" line.length="+line.length);
                if (word.right > start && word.left < end)
                    tp.onWord(word);
            }

            tp.onEndLine();
        }
    }

    /**
     * 文本选择的接口，具体细分
     */
    public interface TextProcessor {
        void onStartLine();
        void onWord(TextWord word);
        void onEndLine();
    }
}
