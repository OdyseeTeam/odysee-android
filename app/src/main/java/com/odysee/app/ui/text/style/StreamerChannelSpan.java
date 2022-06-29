package com.odysee.app.ui.text.style;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

public class StreamerChannelSpan extends ReplacementSpan {
    private float padding;
    private RectF rect;
    private int foregroundColour;
    private int backgroundColour;

    public StreamerChannelSpan(int foregroundColour, int backgroundColour, float padding) {
        rect = new RectF();
        this.foregroundColour = foregroundColour;
        this.backgroundColour = backgroundColour;
        this.padding = padding;
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        rect.set(x, top, x + paint.measureText(text, start, end) + padding, bottom);
        paint.setColor(backgroundColour);
        canvas.drawRect(rect, paint);


        paint.setColor(foregroundColour);
        int xPos = Math.round(x + (padding / 2));
        canvas.drawText(text, start, end, xPos, y, paint);
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end) + padding);
    }
}
