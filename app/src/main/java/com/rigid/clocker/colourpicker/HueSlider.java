package com.rigid.clocker.colourpicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

public class HueSlider extends View implements HexChangedInterface{
    private HueChangeInterface hueChangeInterFace;
    private float[] hsv = new float[3];
    private Paint p = new Paint();
    //to avoid unnecessarily drawing the slider which is costly on memory,
    //we save a 'snapshot' in a bitmap
    private Bitmap bitmap;
    private float radius;
    private float x, y;
    public HueSlider(Context context) {
        super(context);
//        hueChangeInterFace.OnHueChanged(0); //default to 0 (red)
    }

    public HueSlider(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setHueChangeInterFace(HueChangeInterface hueChangeInterFace){
        this.hueChangeInterFace = hueChangeInterFace;
    }
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        p.setAntiAlias(true);
        radius=getHeight()/2f;
        x= (((getWidth()-2*radius)*hsv[0])/360) +radius;
        y=getHeight()/2f;
        /* these values do not change*/
        hsv[1]=1;
        hsv[2]=1;

        //get the snapshot of the canvas and use the bitmap
        float[] bmpHsv = new float[3];
        bmpHsv[1]=1;
        bmpHsv[2]=2;
        Bitmap bitmap1= Bitmap.createBitmap(getWidth(),getHeight(),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap1);
        float ratio = 360f / getWidth();
        for (int y = 0; y <= getHeight(); y++) {
            for (int i = 0; i <= getWidth(); i++) {
                bmpHsv[0] = i * ratio;
                p.setColor(
                        Color.HSVToColor(bmpHsv)
                );
                canvas.drawPoint(i, y, p);
            }
        }
        //i tried to use the same bitmap above but I can't since
        // the canvas gets cleared out and we need it as our source bitmap to apply PorterDuff
        //round the corners
        bitmap = getRoundedCornerBitmap(bitmap1, 20);
        //scale the bitmap so our thumb is
        Matrix m = new Matrix();
        m.setScale(1-(radius/getWidth()),1-(radius/getHeight()),
                getWidth()/2f,getHeight()/2f);
        Bitmap out = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        c.drawBitmap(bitmap,m,null);
        //set as bg
        BitmapDrawable bd = new BitmapDrawable(getResources(),out);
        setBackground(bd);

    }
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, float cornerSizePx) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        // prepare canvas for transfer
        paint.setColor(0xFFFFFFFF);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(rectF, cornerSizePx, cornerSizePx, paint);

        // draw bitmap
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //(FIXED)BUG: for future ref - Throws a Failed Binder Exception although everything seems fine in the profiler--
        //    does not affect the functionality nonetheless.
        //    Happens when canvas draw calls are called furiously on drawBitmap().
        //FIXED: Set the bitmap as the background for this view
        /* Thumb */
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.HSVToColor(hsv));
        canvas.drawCircle(x,y,radius,p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1);
        p.setColor(Color.BLACK);
        canvas.drawCircle(x,y,radius,p);

    }
    @Override
    public void onHexChanged(float hue){
        hsv[0]=hue;
        //set thumbs x value
        x=(((getWidth()-2*radius)*hue)/360) +radius;
        invalidate();
    }
    public void onColourReceive(int colour) {
        Color.colorToHSV(colour,hsv);
        hsv[1]=1;
        hsv[2]=1;
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //subtracting radius (height/2) as we have scaled our hue slider to start from radius and end at w-radius
        x=Math.max(radius,Math.min(getWidth()-radius,event.getX()));
        hsv[0]=(360f/(getWidth()-radius*2))*(x-radius);
        hueChangeInterFace.OnHueChanged(hsv[0]);
        invalidate();
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec,heightMeasureSpec);
    }

}
