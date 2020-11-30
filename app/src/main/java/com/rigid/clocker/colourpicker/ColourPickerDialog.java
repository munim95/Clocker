package com.rigid.clocker.colourpicker;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.rigid.clocker.MainActivity;
import com.rigid.clocker.R;

public class ColourPickerDialog extends DialogFragment implements OnFinalColourSetInterface {
    private View hsvDisplay;
    private View hueSlider, alphaSlider;
    private View colourPreview;
    private int finalColour;
    private final OnPickerDialogResponse onPickerDialogResponse;

    public ColourPickerDialog(OnPickerDialogResponse onPickerDialogResponse){
        this.onPickerDialogResponse = onPickerDialogResponse;
        setStyle(DialogFragment.STYLE_NORMAL,R.style.CustomDialog);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.colour_picker, container,false);
        hsvDisplay = v.findViewById(R.id.huedisplay);
        hueSlider=v.findViewById(R.id.hueslider);
        alphaSlider=v.findViewById(R.id.alphaSlider);
        colourPreview=v.findViewById(R.id.colourpreview);
        v.findViewById(R.id.pickerOkBtn).setOnClickListener(clickListener());
        v.findViewById(R.id.pickerResetBtn).setOnClickListener(clickListener());
        return v;
    }
    private View.OnClickListener clickListener(){
        return v->{
            if(v.getId()==R.id.pickerOkBtn){
                //set values here
                onPickerDialogResponse.response(finalColour);
            }
            dismiss();
        };
    }

    @Override
    public void onColourSet(int colour) {
        finalColour=colour;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity)getActivity()).getMotionLayout().transitionToStart();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        ((MainActivity)getActivity()).getMotionLayout().transitionToEnd();
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().setTitle("Colour Picker");

        ((HueSlider)hueSlider).setHueChangeInterFace((HsvDisplay) hsvDisplay);

        ((AlphaSlider)alphaSlider).addOnAlphaSetInterface((ColourPreviewDisplay)colourPreview);

        ((ColourPreviewDisplay)colourPreview).setViews(
                getView().findViewById(R.id.alphaText),
                getView().findViewById(R.id.redText),
                getView().findViewById(R.id.blueText),
                getView().findViewById(R.id.greenText));
        ((ColourPreviewDisplay)colourPreview).setOnFinalColourSetInterface(this);

        ((HsvDisplay) hsvDisplay).addOnColourSetInterface((ColourPreviewDisplay)colourPreview);
        ((HsvDisplay) hsvDisplay).addOnColourSetInterface((AlphaSlider)alphaSlider);
        ((HsvDisplay) hsvDisplay).setHexText(getView().findViewById(R.id.hexvaluetext));
        ((HsvDisplay) hsvDisplay).setHexChangedInterface((HueSlider)hueSlider);

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
