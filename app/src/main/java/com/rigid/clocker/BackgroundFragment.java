package com.rigid.clocker;

import android.app.WallpaperManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;

import com.rigid.clocker.colourpicker.ColourPickerDialog;

public class BackgroundFragment extends Fragment {
    private SwitchCompat wallpaperBlend, inlaidAdjust;
    private FrameLayout bgColourSelect, inlaidColourSelect,bgColour,inlaidColour;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.background_fragment,container,false);
        wallpaperBlend=v.findViewById(R.id.wallpaperBlendSwitch);
        inlaidAdjust=v.findViewById(R.id.inlaidAdjustSwitch);
        bgColourSelect = v.findViewById(R.id.bgColourSelect);
        inlaidColourSelect = v.findViewById(R.id.inlaidColourSelect);
        bgColour = v.findViewById(R.id.bgColour);
        inlaidColour = v.findViewById(R.id.inlaidColour);
        if(wallpaperBlend.isChecked()){
            Palette.from(((BitmapDrawable) WallpaperManager.getInstance(getContext()).getDrawable()).getBitmap()).generate(palette -> {
                if (palette != null) {
                    Palette.Swatch swatch = palette.getDominantSwatch();
                    if (swatch != null) {
                        //set the colours here
                        //only if user pref allow
                        bgColour.setBackgroundColor(swatch.getRgb());
                        inlaidColour.setBackgroundColor(swatch.getBodyTextColor());
                    }
                }
            });
        }
        wallpaperBlend.setOnCheckedChangeListener(checkedChangeListener());
        inlaidAdjust.setOnCheckedChangeListener(checkedChangeListener());
        bgColourSelect.setOnClickListener(onClickListener());
        inlaidColourSelect.setOnClickListener(onClickListener());
        return v;
    }
    private CompoundButton.OnCheckedChangeListener checkedChangeListener(){
        return (buttonView, isChecked) -> {
            if(buttonView.getId()==wallpaperBlend.getId()) {
                if(isChecked){
                    //we are comparing the two bitmaps here so we only call this when needed
                    //wall has changed
                    Palette.from(((BitmapDrawable) WallpaperManager.getInstance(getContext()).getDrawable()).getBitmap())
                            .generate(palette -> {
                        if (palette != null) {
                            Palette.Swatch swatch = palette.getDominantSwatch();
                            if (swatch != null) {
                                //set the colours here
                                //only if user pref allow
                                bgColour.setBackgroundColor(swatch.getRgb());
                                inlaidColour.setBackgroundColor(swatch.getBodyTextColor());
                            }
                        }
                    });
                    bgColourSelect.setAlpha(0.5f);
                    bgColourSelect.setClickable(false);
                    inlaidColourSelect.setAlpha(0.5f);
                    inlaidColourSelect.setClickable(false);
                }else{
                    bgColourSelect.setAlpha(1f);
                    bgColourSelect.setClickable(true);
                }
            }else{
                if(isChecked){
                    //get bgcolour bitmap to get its palette
                    Bitmap bitmap = Bitmap.createBitmap(bgColour.getWidth(),bgColour.getHeight(),
                            Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawColor(((ColorDrawable)bgColour.getBackground()).getColor());
                    Palette.from(bitmap).generate(palette -> {
                        if (palette != null) {
                            Palette.Swatch swatch = palette.getDominantSwatch();
                            if (swatch != null) {
                                inlaidColour.setBackgroundColor(swatch.getBodyTextColor());
                            }
                        }
                    });
                    inlaidColourSelect.setAlpha(0.5f);
                    inlaidColourSelect.setClickable(false);
                }else{
                    inlaidColourSelect.setAlpha(1f);
                    inlaidColourSelect.setClickable(true);
                }
            }
        };
    }
    private View.OnClickListener onClickListener(){
        return v -> {
            ColourPickerDialog colourPickerDialog;
            if(v.getId()==bgColourSelect.getId()){
                colourPickerDialog= new ColourPickerDialog(colour -> {
                    bgColour.setBackgroundColor(colour);
                },((ColorDrawable)bgColour.getBackground()).getColor());
            }else{
               colourPickerDialog = new ColourPickerDialog(colour -> {
                   inlaidColour.setBackgroundColor(colour);
               },((ColorDrawable)inlaidColour.getBackground()).getColor());
            }
            colourPickerDialog.show(getChildFragmentManager(),null);
        };
    }
}
