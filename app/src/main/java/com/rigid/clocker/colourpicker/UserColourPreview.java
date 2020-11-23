package com.rigid.clocker.colourpicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
//This is where we get the users final chosen colour
public class UserColourPreview extends View implements UserColourPreviewInterface {
    private RectF rectF;
    private Paint p;

    public UserColourPreview(Context context) {
        super(context);
    }

    public UserColourPreview(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        p=new Paint();
        p.setColor(Color.RED);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rectF = new RectF(0,0,w,h);
    }
    //hue display
    @Override
    public void onChanged(int colour){
        p.setColor(colour);
        invalidate();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(rectF,p);
    }
}
