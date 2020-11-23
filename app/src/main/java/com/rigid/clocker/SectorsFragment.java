package com.rigid.clocker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SectorsFragment extends Fragment implements View.OnClickListener {
    private RecyclerView rv;
    private SectorsAdapter sectorsAdapter;
    private ArrayList<Object> data;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.sectors_fragment,container,false);
        rv=v.findViewById(R.id.sectorsRv);
        sectorsAdapter = new SectorsAdapter(this);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setItemAnimator(null);
        rv.setAdapter(sectorsAdapter);
        ImageButton addGroupBtn = v.findViewById(R.id.addGroup);
        addGroupBtn.setOnClickListener(this);
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        if(data==null)
            data= new ArrayList<>();
        SectorGroup sectorGroup=null;
        if(data.size()!=0){
            //find the last instance of sector group
            for(int i=data.size()-1;;i--){
                if(data.get(i) instanceof SectorGroup) {
                    sectorGroup = (SectorGroup) data.get(i);
                    break;
                }
            }
        }
        //increment last sector group ID in the list to keep unique IDs
        data.add(new SectorGroup(sectorGroup!=null?sectorGroup.getGroupId()+1:0));
        sectorsAdapter.addData(data);
    }
}
