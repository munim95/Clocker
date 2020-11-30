package com.rigid.clocker.colourpicker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.rigid.clocker.R;

import java.util.ArrayList;
import java.util.List;

//recieves Hue values from hue slider
//sends saturation and value values to colour preview and alpha slider
public class HsvDisplay extends View implements HueChangeInterface {
    private Paint p;
    private float[] hueHsv;
    private float[] userHSV;
    private int displayColour=Color.RED; //colour chosen from the hue slider
    private int currentColour =Color.RED; //hsv values from the thumb
    private RectF surfaceRect;
    private EditText hexText;
    private float radius;
    private float x,y;
    private HexChangedInterface hexChangedInterface;
    private List<OnColourSetInterface> onColourSetInterfaces;
    private Bitmap bgBmp;
    private PorterDuffXfermode xfermode;

    public HsvDisplay(Context context) {
        super(context);
    }

    public HsvDisplay(Context context, AttributeSet attrs) {
        super(context, attrs);
        p=new Paint();
        p.setAntiAlias(true);
        //hue slider dependant hsv
        //only responsible for displaying here
        hueHsv =new float[3];
        hueHsv[0]=0; //default (RED) - hue only value that changes
        hueHsv[1]=1; //s - 100%
        hueHsv[2]=1; //v - 100%

        // user dependant hsv for preview
        //communicates with preview, hex, slider
        userHSV=new float[3];
        userHSV[0]=0;
        userHSV[1]=1;
        userHSV[2]=1;

        onColourSetInterfaces =new ArrayList<>();
    }
    //2 interfaces - alpha slider and preview
    public void addOnColourSetInterface(OnColourSetInterface onColourSetInterface){
        onColourSetInterfaces.add(onColourSetInterface);
    }
    private void updateInterfaces(int colour){
        for (OnColourSetInterface onColourSetInterface : onColourSetInterfaces)
            onColourSetInterface.onColourChanged(colour);
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

        hexText.setEnabled(false);
        hueHsv[0] =hue;
        displayColour = Color.HSVToColor(hueHsv);
        userHSV[0] = hue; //other values remain unchanged as set by user
        currentColour= Color.HSVToColor(userHSV);
        updateInterfaces(currentColour);
        hexText.setText(getContext().getString(R.string.hexString,Integer.toHexString(currentColour).substring(2)));
        hexText.setEnabled(true);

        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bgBmp = BitmapFactory.decodeResource(getResources(),R.drawable.picker_mask);
        xfermode = new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP);
        /* Defaults */
        surfaceRect =new RectF(0,0,w,h);
        radius=Math.min(w,h)*0.05f;
        x=w;
        y=0;
//        hexText=((Activity)getContext()).findViewById(R.id.hexvaluetext);
        hexText.setText(getContext().getString(R.string.hexString,Integer.toHexString(currentColour).substring(2)));
        updateInterfaces(currentColour);
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
                        //calc thumb coords corresponding hsv values
                        x = userHSV[1] * getWidth();
                        y = getHeight() - (userHSV[2] * getHeight());
                        updateInterfaces(c);
                        hexChangedInterface.onHexChanged(userHSV[0]);
                        invalidate();
                    }
                }
            }
        });
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        p.setStyle(Paint.Style.FILL);
        p.setColor(displayColour);
        canvas.drawRect(surfaceRect, p);
        p.setXfermode(xfermode);
        canvas.drawBitmap(bgBmp,null,surfaceRect,p);
        p.setXfermode(null);

        /* Thumb */
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5);
        p.setColor(Color.BLACK);
        canvas.drawCircle(x, y, radius, p);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        x = Math.min(getWidth(),Math.max(0,event.getX()));
        y = Math.min(getHeight(),Math.max(0,event.getY()));
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
//                resume();
                hexText.setEnabled(false);
                 //doing this to avoid afterTextChanged being called and ruining it smh...
                userHSV[0]= hueHsv[0];
                userHSV[1] = x/getWidth();
                userHSV[2] = (getHeight()-y)/getHeight(); //we do this to avoid rotating our BG image to fit android coords
                currentColour = Color.HSVToColor(userHSV);
                hexText.setText(getContext().getString(R.string.hexString,Integer.toHexString(currentColour).substring(2)));
                updateInterfaces(currentColour);
                break;
            case MotionEvent.ACTION_MOVE:
                userHSV[1] = x/getWidth();
                userHSV[2] = (getHeight()-y)/getHeight();
                currentColour =Color.HSVToColor(userHSV);
                hexText.setText(getContext().getString(R.string.hexString,Integer.toHexString(currentColour).substring(2)));
                updateInterfaces(currentColour);
                break;
            case MotionEvent.ACTION_UP:
                hexText.setEnabled(true);
                break;
        }
        invalidate();
        return true;
    }
}
