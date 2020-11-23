package com.rigid.clocker.colourpicker;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.rigid.clocker.R;

public class ColourPickerDialog extends DialogFragment {
    @SuppressLint("StaticFieldLeak")
    private static ColourPickerDialog colourPickerDialog;
    public static ColourPickerDialog getInstance(){
        if(colourPickerDialog==null)
            colourPickerDialog=new ColourPickerDialog();
        return colourPickerDialog;
    }
    private View hueDisplaySurface;
    private View hueSlider;
    private View userColourPreview;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.colour_picker, container,false);
        hueDisplaySurface = v.findViewById(R.id.huedisplay);
        hueSlider=v.findViewById(R.id.hueslider);
        userColourPreview=v.findViewById(R.id.colourpreview);
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
//        ((HueDisplaySurface)hueDisplaySurface).stop();

    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().setTitle("Colour Picker");

        ((HueSlider)hueSlider).setHueChangeInterFace((HueDisplaySurface)hueDisplaySurface);
        ((HueDisplaySurface)hueDisplaySurface).setUserColourPreviewInterface((UserColourPreview)userColourPreview);
        ((HueDisplaySurface)hueDisplaySurface).setHexText(getView().findViewById(R.id.hexvaluetext));
        ((HueDisplaySurface)hueDisplaySurface).setHexChangedInterface((HueSlider)hueSlider);

        ((HueDisplaySurface)hueDisplaySurface).resume();
    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }
}
