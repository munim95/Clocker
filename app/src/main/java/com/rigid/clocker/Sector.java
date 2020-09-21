package com.rigid.clocker;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
//represents a sector for the pie chart
public class Sector {
    private String name;
    private int colour;
    private long startTime,endTime;

    public Sector(String name, long startTime, long endTime,int colour){
        this.name=name;
        this.startTime=startTime;
        this.endTime=endTime;
        this.colour=colour;
    }
    public long getTotalTime(int ClockMode){
        if(endTime<startTime) { // end gone beyond 12/24 hrs
            return (ClockMode*60+endTime) - startTime;
        }
        return endTime-startTime;
    }
    public long getStartTime(){
        return startTime;
    }
    public long getEndTime(){
        return endTime;
    }
    public String getName(){return name;}
    public int getColour(){return colour;}

    public void setColour(int colour) {
        this.colour = colour;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return super.equals(obj);
    }

}
