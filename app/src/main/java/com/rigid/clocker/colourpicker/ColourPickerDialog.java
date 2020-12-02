package com.rigid.clocker.colourpicker;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.rigid.clocker.R;

public class ColourPickerDialog extends DialogFragment implements OnFinalColourSetInterface {
    private View hsvDisplay;
    private View hueSlider, alphaSlider;
    private View colourPreview;
    private int receivingColour;
    private int finalColour;
    private final OnPickerDialogResponse onPickerDialogResponse;

    public ColourPickerDialog(OnPickerDialogResponse onPickerDialogResponse, int color){
        this.onPickerDialogResponse = onPickerDialogResponse;
        receivingColour=color;
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

        ((HueSlider)hueSlider).setHueChangeInterFace((HsvDisplay) hsvDisplay);

        ((AlphaSlider)alphaSlider).setOnAlphaSetInterface((ColourPreviewDisplay)colourPreview);

        ((ColourPreviewDisplay)colourPreview).setViews(
                v.findViewById(R.id.alphaText),
                v.findViewById(R.id.redText),
                v.findViewById(R.id.blueText),
                v.findViewById(R.id.greenText));
        ((ColourPreviewDisplay)colourPreview).setOnFinalColourSetInterface(this);

        ((HsvDisplay) hsvDisplay).addOnColourSetInterface((ColourPreviewDisplay)colourPreview);
        ((HsvDisplay) hsvDisplay).addOnColourSetInterface((AlphaSlider)alphaSlider);
        ((HsvDisplay) hsvDisplay).setHexText(v.findViewById(R.id.hexvaluetext));
        ((HsvDisplay) hsvDisplay).setHexChangedInterface((HueSlider)hueSlider);

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
        ((HsvDisplay)hsvDisplay).onColourReceive(receivingColour);
        ((HueSlider)hueSlider).onColourReceive(receivingColour);
        ((AlphaSlider)alphaSlider).onColourReceive(receivingColour);

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getDialog().setTitle("Colour Picker");
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
