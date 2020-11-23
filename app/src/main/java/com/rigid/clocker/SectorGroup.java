package com.rigid.clocker;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class SectorGroup {
    private boolean isExpanded;
    private String groupName;
    private int groupId;
    private ArrayList<Sector> sectorList;

    public SectorGroup(int groupId){
        this.groupId = groupId;
        this.groupName="";
        sectorList = new ArrayList<>();
    }

    public void setSectorList(ArrayList<Sector> sectorList) {
        this.sectorList = sectorList;
    }

    public int getGroupId() {
        return groupId;
    }

    public String getGroupInfo() {
        return sectorList.size()+" Sectors";
    }

    public ArrayList<Sector> getSectorList() {
        return sectorList;
    }
    private void setGroupName(String groupName){
        this.groupName=groupName;
    }
    public String getGroupName(){
        return groupName;
    }
    public void setExpanded(boolean expanded){
        isExpanded=expanded;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    @Override
    public boolean equals(@Nullable Object obj) {

        if(obj instanceof SectorGroup) {
            return groupName.equals(((SectorGroup) obj).groupName) && groupId==((SectorGroup) obj).groupId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
