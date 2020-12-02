package com.rigid.clocker.colourpicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.rigid.clocker.R;

import java.util.ArrayList;
import java.util.List;

public class AlphaSlider extends View implements OnColourSetInterface {
    private int colour =Color.RED; //we are going to receive this value from user preview
    private Paint p=new Paint();
    private float l,r,t,b,
            ww;
    private Shader linearGradient;
    private OnAlphaSetInterface onAlphaSetInterfaces;
    private float alpha;
    public AlphaSlider(Context context) {
        super(context);
    }

    public AlphaSlider(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    protected void setOnAlphaSetInterface(OnAlphaSetInterface onAlphaSetInterface){
        onAlphaSetInterfaces=onAlphaSetInterface;
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        linearGradient = new LinearGradient(0, 0, getWidth(), 0,
                Color.argb(0, Color.red(colour),Color.green(colour),Color.blue(colour)),
                colour,
                Shader.TileMode.CLAMP);
        ww = w*0.03f;
        Bitmap bgBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c2 = new Canvas(bgBitmap);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.transparent_bg);
        c2.drawBitmap(bitmap,0,0,p);

        //to scale it
        Bitmap out = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Matrix m = new Matrix();
        m.setScale(1-(ww/w),1,w*0.5f,h*0.5f);
        canvas.drawBitmap(bgBitmap,m,p);

        BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(),out);
        setBackground(bitmapDrawable);

        l=(alpha*(getWidth()-ww))/255;
        t=0;
        r=l+ww;
        b=t+h;

        onAlphaSetInterfaces.onAlphaChange(l/(getWidth()-ww));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        p.setShader(linearGradient);
        canvas.scale(1-(ww/getWidth()),1,getWidth()*.5f,getHeight()*.5f);
        canvas.drawRect(0,0,getWidth(),getHeight(),p);
        p.reset();
        canvas.restore();

        /* Thumb */
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5);
        p.setColor(Color.BLACK);
        canvas.drawRect(l,t,r,t+b,p);
        p.reset();

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        l=Math.max(0,Math.min(event.getX(),getWidth()-ww));
        r=l+ww;
        float ratio =l/(getWidth()-ww);
        onAlphaSetInterfaces.onAlphaChange(ratio);
        invalidate();
        return true;
    }

    public void onColourReceive(int colour) {
        alpha=Color.alpha(colour);
    }

    @Override
    public void onColourChanged(int colour) {
        this.colour =colour;
        linearGradient = new LinearGradient(0, 0, getWidth(), 0,
                Color.argb(0,Color.red(colour),Color.green(colour),Color.blue(colour)),
                colour,
                Shader.TileMode.CLAMP);
        invalidate();
    }
}
