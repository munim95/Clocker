package com.rigid.clocker.colourpicker;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.EditText;
import android.widget.Toast;

import com.rigid.clocker.R;

public class HueDisplaySurface extends SurfaceView implements Runnable, HueChangeInterface, SurfaceHolder.Callback {
    private Thread thread;
    private Paint p;
    private float[] hueHsv;
    private float[] userHSV;
    private int displayColour=Color.RED;
    private SurfaceHolder mSurfaceHolder;
    private RectF surfaceRect;
    private UserColourPreviewInterface userColourPreviewInterface;
    private EditText hexText;
    private int currentColour =Color.RED;
    private float radius;
    private float x,y;
    private HexChangedInterface hexChangedInterface;

    public HueDisplaySurface(Context context) {
        super(context);
    }

    public HueDisplaySurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        p=new Paint();
        p.setAntiAlias(true);
        //hue slider dependant hsv
        //only responsible for displaying here
        hueHsv =new float[3];
        hueHsv[0]=0; //default (RED) - hue only value that changes
        hueHsv[1]=1; //s - 100%
        hueHsv[2]=1; //v - 100%
        mSurfaceHolder = getHolder();

        // user dependant hsv for preview
        //communicates with preview, hex, slider
        userHSV=new float[3];
        userHSV[0]=0;
        userHSV[1]=1;
        userHSV[2]=1;

    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceHolder=holder;
        mSurfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        holder.removeCallback(this);
    }
    public void setUserColourPreviewInterface(UserColourPreviewInterface userColourPreviewInterface){
        this.userColourPreviewInterface = userColourPreviewInterface;
    }
    public void setHexChangedInterface(HexChangedInterface hexChangedInterface){
        this.hexChangedInterface=hexChangedInterface;
    }
    public void setHexText(EditText editText){
        hexText=editText;
    }
    //from the colour slider
    @Override
    public void OnHueChanged(float hue) {
        resume();

        hexText.setEnabled(false);
        hueHsv[0] =hue;
        displayColour = Color.HSVToColor(hueHsv);
        userHSV[0] = hue; //other values remain unchanged as set by user
        currentColour= Color.HSVToColor(userHSV);
        userColourPreviewInterface.onChanged(currentColour);
        hexText.setText(getContext().getString(R.string.hexString,Integer.toHexString(currentColour).substring(2)));
        hexText.setEnabled(true);

       stop();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        /* Defaults */
        surfaceRect =new RectF(0,0,w,h);
        radius=Math.min(w,h)*0.05f;
        x=w;
        y=0;
//        hexText=((Activity)getContext()).findViewById(R.id.hexvaluetext);
        hexText.setText(getContext().getString(R.string.hexString,Integer.toHexString(currentColour).substring(2)));
        userColourPreviewInterface.onChanged(currentColour);

        hexText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (hexText.isEnabled()) {
                    //always keep the '#' at the start
                    if (!s.toString().startsWith("#"))
                        s.insert(0, "#");
                    if (s.length() == 7) {
                        for (int i = 1; i < s.length(); i++) {
                            if (Character.digit(s.charAt(i), 16) == -1) { //check if valid hex
                                s.clear();
                                Toast.makeText(getContext(), "Invalid HEX colour code.", Toast.LENGTH_SHORT)
                                        .show();
                                return;
                            }
                        }
                        int c = Color.parseColor(s.toString()); //convert Hex to int
                        Color.colorToHSV(c, userHSV);
                        hueHsv[0] = userHSV[0];
                        displayColour=Color.HSVToColor(hueHsv);
                        //calc coords corresponding hsv values
                        x = userHSV[1] * getWidth();
                        y = getHeight() - (userHSV[2] * getHeight());
                        userColourPreviewInterface.onChanged(c);
                        hexChangedInterface.onHexChanged(userHSV[0]);
                    }
                }
            }
        });
    }

    @Override
    public void run() {
        Canvas canvas;
        while (!thread.isInterrupted()) {
            if (mSurfaceHolder.getSurface().isValid()) {
                canvas = mSurfaceHolder.lockCanvas();
                if(canvas==null)
                    return;
                p.setStyle(Paint.Style.FILL);
                p.setColor(displayColour);
                canvas.drawRect(surfaceRect, p);

                /* Thumb */
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(5);
                p.setColor(Color.BLACK);
                canvas.drawCircle(x, y, radius, p);
                mSurfaceHolder.unlockCanvasAndPost(canvas);

            }
        }
    }

    /**
     * Kill thread on activity paused
     */
    public void stop() {
        if(thread!=null && thread.isAlive()) {
//            if(mSurfaceHolder.getSurface().isValid())
//                mSurfaceHolder.getSurface().release();
//            try {
            // Stop the thread == rejoin the main thread.
            thread.interrupt(); //using interrupt instead of join because no need to wait for thread to finish task in this case
//            } catch (InterruptedException e) {
//            }
//            mSurfaceHolder.removeCallback(this);
        }
    }

    /**
     * Start the thread when activity started
     */
    public void resume() {
        if (thread == null || !thread.isAlive()) {
            thread = new Thread(this);
            thread.start();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        x = Math.min(getWidth(),Math.max(0,event.getX()));
        y = Math.min(getHeight(),Math.max(0,event.getY()));
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                resume();
                hexText.setEnabled(false);
                 //doing this to avoid afterTextChanged being called and ruining it smh...
                userHSV[0]= hueHsv[0];
                userHSV[1] = x/getWidth();
                userHSV[2] = (getHeight()-y)/getHeight(); //we do this to avoid rotating our BG image to fit android coords
                currentColour = Color.HSVToColor(userHSV);
                hexText.setText(getContext().getString(R.string.hexString,Integer.toHexString(currentColour).substring(2)));
                userColourPreviewInterface.onChanged(currentColour);
                break;
            case MotionEvent.ACTION_MOVE:
                userHSV[1] = x/getWidth();
                userHSV[2] = (getHeight()-y)/getHeight();
                currentColour =Color.HSVToColor(userHSV);
                hexText.setText(getContext().getString(R.string.hexString,Integer.toHexString(currentColour).substring(2)));
                userColourPreviewInterface.onChanged(currentColour);
                break;
            case MotionEvent.ACTION_UP:
                stop();
                hexText.setEnabled(true);
                break;
        }
        return true;
    }
}
