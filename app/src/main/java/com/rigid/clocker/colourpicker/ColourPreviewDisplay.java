package com.rigid.clocker.colourpicker;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

//This is where we get the users final chosen colour
public class ColourPreviewDisplay extends View implements OnColourSetInterface, OnAlphaSetInterface {
    private Rect rect;
    private Paint p;
    private int colour, alpha=255;
    private TextView alphaText,redText,greenText,blueText;
    private OnFinalColourSetInterface onFinalColourSetInterface;

    public ColourPreviewDisplay(Context context) {
        super(context);
    }

    public ColourPreviewDisplay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        p=new Paint();
        p.setColor(colour);
    }
    public void setViews(TextView... views){
        alphaText=views[0];
        redText=views[1];
        blueText= views[2];
        greenText=views[3];
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rect = new Rect(0,0,w,h);
    }
    //hue display
    @Override
    public void onColourChanged(int colour){
        this.colour=Color.argb(alpha,Color.red(colour),Color.green(colour),Color.blue(colour));
        p.setColor(this.colour);
        invalidate();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(rect,p);

        //we do this here as both final alpha and colour values fall here
        if(alphaText!=null) {
            alphaText.setText("A "+Color.alpha(colour));
            redText.setText("R "+Color.red(colour));
            greenText.setText("G "+Color.green(colour));
            blueText.setText("B "+Color.blue(colour));
        }
        onFinalColourSetInterface.onColourSet(colour);
    }

    @Override
    public void onAlphaChange(float alpha) {
        this.alpha=(int)(alpha*255f);
        colour=Color.argb(this.alpha,Color.red(colour),Color.green(colour),Color.blue(colour));
        p.setColor(colour);
        invalidate();
    }

    public void setOnFinalColourSetInterface(OnFinalColourSetInterface onFinalColourSetInterface) {
        this.onFinalColourSetInterface=onFinalColourSetInterface;
    }
}
