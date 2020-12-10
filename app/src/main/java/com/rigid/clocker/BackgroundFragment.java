package com.rigid.clocker;

import android.app.WallpaperManager;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.palette.graphics.Palette;

import com.rigid.clocker.colourpicker.ColourPickerDialog;

import java.io.FileDescriptor;

public class BackgroundFragment extends Fragment {
    private SwitchCompat wallpaperBlend, inlaidAdjust;
    private FrameLayout bgColourSelect, inlaidColourSelect,bgColour,inlaidColour;
    private RadioGroup bgRadioGroup;
    private boolean inlaidCheckedFromBgSwitch =false;
    private Bitmap oldWallBmp;
    private RadioButton bgColourRadio, customImgRadio;
    private Button browse;
    private Thread thread;

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

        bgRadioGroup=v.findViewById(R.id.bgRadioGroup);
        bgColourRadio=v.findViewById(R.id.bgColourRadio);
        customImgRadio=v.findViewById(R.id.customImageRadio);
        browse=v.findViewById(R.id.browseImage);

        wallpaperBlend.setOnCheckedChangeListener(checkedChangeListener());
        inlaidAdjust.setOnCheckedChangeListener(checkedChangeListener());
        bgColourSelect.setOnClickListener(onClickListener());
        inlaidColourSelect.setOnClickListener(onClickListener());

        wallpaperBlend.setChecked(false);
        wallpaperBlend.setChecked(true);

        bgRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if(checkedId==bgColourRadio.getId()){
                bgColourSelect.setClickable(true);
                bgColourSelect.setAlpha(1f);
                browse.setEnabled(false);
            }else{
                bgColourSelect.setClickable(false);
                bgColourSelect.setAlpha(0.5f);
                browse.setEnabled(true);
            }
        });
        //user pref
        bgRadioGroup.check(bgColourRadio.getId());

        browse.setOnClickListener(v1 -> {
            //file picker then
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");

            startActivityForResult(intent,0);

        });
        return v;
    }

    private Bitmap image;
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(data!=null) {
            if(thread==null)
                thread=new Thread(()->{
                    try {
                        ParcelFileDescriptor parcelFileDescriptor =
                                getActivity().getContentResolver().openFileDescriptor(data.getData(), "r");
                        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                        parcelFileDescriptor.close();
                        
                        Thread.currentThread().interrupt();
                    }catch (Exception e){}
                });
            if(!thread.isAlive())
                thread.start();
        }
    }

    private CompoundButton.OnCheckedChangeListener checkedChangeListener(){
        return (buttonView, isChecked) -> {
            if(buttonView.getId()==wallpaperBlend.getId()) {
                if(isChecked){
                    Bitmap current = ((BitmapDrawable) WallpaperManager.getInstance(getContext()).getDrawable()).getBitmap();
                    if(oldWallBmp==null || !oldWallBmp.sameAs(current)) {
                        if(oldWallBmp!=null)
                            oldWallBmp.recycle();
                        oldWallBmp = current;
                    }
                    Palette.from(oldWallBmp).generate(palette -> {
                        if (palette != null) {
                            Palette.Swatch swatch = palette.getDominantSwatch();
                            if (swatch != null) {
                                bgColour.setBackgroundColor(swatch.getRgb());
                                inlaidColour.setBackgroundColor(swatch.getBodyTextColor());
                            }
                        }
                    });
                    //disabled look
                    bgRadioGroup.setAlpha(0.5f);
                    bgColourRadio.setEnabled(false);
                    customImgRadio.setEnabled(false);

                    inlaidCheckedFromBgSwitch =true;
                    inlaidAdjust.setChecked(true);
                }else{
                    bgRadioGroup.setAlpha(1f);
                    bgColourRadio.setEnabled(true);
                    customImgRadio.setEnabled(true);
                }
            }else{
                if(isChecked){
                    if(!inlaidCheckedFromBgSwitch) {
                        if (oldWallBmp==null || !oldWallBmp.sameAs(((BitmapDrawable) WallpaperManager.getInstance(getContext()).getDrawable()).getBitmap())) {
                            //get bgcolour bitmap to get its palette
                            Bitmap bitmap = Bitmap.createBitmap(bgColour.getWidth(), bgColour.getHeight(),
                                    Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bitmap);
                            canvas.drawColor(((ColorDrawable) bgColour.getBackground()).getColor());
                            Palette.from(bitmap).generate(palette -> {
                                if (palette != null) {
                                    Palette.Swatch swatch = palette.getDominantSwatch();
                                    if (swatch != null) {
                                        inlaidColour.setBackgroundColor(swatch.getBodyTextColor());
                                    }
                                }
                            });
                        }
                    }
                    inlaidColourSelect.setAlpha(0.5f);
                    inlaidColourSelect.setClickable(false);

                    inlaidCheckedFromBgSwitch =false;
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
