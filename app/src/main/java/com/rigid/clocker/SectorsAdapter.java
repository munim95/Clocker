package com.rigid.clocker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.rigid.clocker.colourpicker.ColourPickerDialog;

import java.util.ArrayList;

public class SectorsAdapter extends RecyclerView.Adapter {
    private ArrayList<Object> data;
    private RecyclerView recyclerView;
    private Fragment fragment;

    public SectorsAdapter(Fragment fragment){
        this.fragment = fragment;
    }

    public void addData(ArrayList<Object> data){
        this.data = data;
        notifyDataSetChanged();

    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if(viewType==0)
            return new SectorGroupViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sector_group_item, parent, false));
        else
            return new SectorsViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.sector_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(getItemViewType(position)==0)
            ((SectorGroupViewHolder)holder).bind(position);
        else
            ((SectorsViewHolder)holder).bind(position);
    }

    @Override
    public int getItemViewType(int position) {
        if(data.get(position) instanceof Sector)
            return 1;
       return 0;
    }

    @Override
    public int getItemCount() {
        return data!=null?data.size():0;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView=recyclerView;
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
    }

    class SectorGroupViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView groupInfo;
        private EditText groupName;
        private ImageButton deleteGroup,addSector;
        private RadioButton makeDefault;
        private ImageView groupExpandArrow;

        SectorGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupInfo= itemView.findViewById(R.id.groupInfo);
            groupName = itemView.findViewById(R.id.groupName);
            addSector = itemView.findViewById(R.id.addSectorToGroup);
            deleteGroup = itemView.findViewById(R.id.deleteGroup);
            makeDefault = itemView.findViewById(R.id.makeGroupDefault);
            groupExpandArrow = itemView.findViewById(R.id.groupExpandArrowImage);
            addSector.setOnClickListener(this);
            deleteGroup.setOnClickListener(this);
            groupExpandArrow.setOnClickListener(this);
            makeDefault.setOnCheckedChangeListener((buttonView, isChecked) -> {
                //make a group default
                //this will determine the current sector group

            });
        }
        private void bind(int position){
            //update views
            SectorGroup sectorGroup = (SectorGroup)data.get(position);
            groupExpandArrow.setRotation(sectorGroup.isExpanded() ? 180 : 0);
            groupName.setText(sectorGroup.getGroupName());
            groupInfo.setText(sectorGroup.getGroupInfo());

        }

        @Override
        public void onClick(View v) {
            SectorGroup sectorGroup = ((SectorGroup) data.get(getAdapterPosition()));
            if(v.getId() == R.id.groupExpandArrowImage) {
                if(sectorGroup.getSectorList()!=null && !sectorGroup.getSectorList().isEmpty()) {
                    if (!sectorGroup.isExpanded()) {
                        sectorGroup.setExpanded(true);
                        //add sectors to data then notify
                        data.addAll(getAdapterPosition() + 1, sectorGroup.getSectorList());
                        notifyItemRangeInserted(getAdapterPosition() + 1, sectorGroup.getSectorList().size());
                    } else {
                        sectorGroup.setExpanded(false);
                        if (!data.removeAll(sectorGroup.getSectorList())) {
                            //remove manually if failed
                            for (int i = 1; i <= sectorGroup.getSectorList().size(); i++) {
                                data.remove(getAdapterPosition() + i);
                            }
                        }
                        notifyItemRangeRemoved(getAdapterPosition() + 1, sectorGroup.getSectorList().size());
                    }
                    // rotate the icon based on the current state
                    groupExpandArrow.animate()
                            .rotation(sectorGroup.isExpanded() ? 180 : 0)
                            .start();
                }else
                    Toast.makeText(v.getContext(),"Please add sectors.",Toast.LENGTH_SHORT).show();

        } else if(v.getId()==R.id.deleteGroup){
                if(sectorGroup.isExpanded()) {
                    data.remove(getAdapterPosition());
                    data.removeAll(sectorGroup.getSectorList());
                    notifyItemRangeRemoved(getAdapterPosition(),getAdapterPosition()+sectorGroup.getSectorList().size());
                }else {
                    data.remove(getAdapterPosition());
                    notifyItemRemoved(getAdapterPosition());
                }
            }
            else {
                // add sector
                //todo sectors to be arranged according to time
                //add sectors to data then notify
                Sector sector = new Sector("LOL TEST",20,120, Color.RED);
                sector.setGroupId(sectorGroup.getGroupId());
                sectorGroup.getSectorList().add(sector);
                groupInfo.setText(sectorGroup.getGroupInfo());
                if (!sectorGroup.isExpanded()) {
                    data.addAll(getAdapterPosition() + 1, sectorGroup.getSectorList());
                    notifyItemRangeInserted(getAdapterPosition() + 1,sectorGroup.getSectorList().size());
                }else {
                    data.add(getAdapterPosition() + 1, sector);
                    notifyItemInserted(getAdapterPosition() + 1);
                }
                sectorGroup.setExpanded(true);

                // rotate the icon based on the current state
                groupExpandArrow.animate()
                        .rotation(sectorGroup.isExpanded() ? 180 : 0)
                        .start();

            }
        }
    }
    class SectorsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView sectorInfo;
        private EditText sectorName;
        private View sectorColourImage;
        private ImageButton deleteSectorBtn;
        private LinearLayout sectorInfoDetails;
        private TextView startHourText, startMinText,
                endHourText,endMinText;
        SectorsViewHolder(@NonNull View itemView) {
            super(itemView);
            sectorInfo = itemView.findViewById(R.id.sectorInfo);
            sectorInfoDetails = itemView.findViewById(R.id.sectorInfoDetails);
            sectorName = itemView.findViewById(R.id.sectorName);
            sectorColourImage = itemView.findViewById(R.id.sectorColourImage);
            deleteSectorBtn = itemView.findViewById(R.id.deleteSector);

            startHourText=itemView.findViewById(R.id.startHourTimerText);
            startMinText=itemView.findViewById(R.id.startMinTimerText);
            endHourText=itemView.findViewById(R.id.endHourTimerText);
            endMinText=itemView.findViewById(R.id.endMinTimerText);

            sectorInfo.setOnClickListener(this);
            deleteSectorBtn.setOnClickListener(this);
            sectorColourImage.setOnClickListener(this);
            itemView.findViewById(R.id.startHourUpBtn).setOnClickListener(this);
            itemView.findViewById(R.id.startHourDownBtn).setOnClickListener(this);
            itemView.findViewById(R.id.startMinUpBtn).setOnClickListener(this);
            itemView.findViewById(R.id.startMinDownBtn).setOnClickListener(this);

            itemView.findViewById(R.id.endHourUpBtn).setOnClickListener(this);
            itemView.findViewById(R.id.endHourDownBtn).setOnClickListener(this);
            itemView.findViewById(R.id.endMinUpBtn).setOnClickListener(this);
            itemView.findViewById(R.id.endMinDownBtn).setOnClickListener(this);
        }
        private void bind(int position){
            Sector sector = (Sector) data.get(position);
            sectorName.setText(sector.getName());
            sectorColourImage.setBackgroundColor(sector.getColour());
            sectorInfoDetails.setVisibility(sector.isExpanded()?View.VISIBLE:View.GONE);
            sectorInfoDetails.setAlpha(sector.isExpanded()?1:0);

            int[] start = Helpers.timeConversion(sector.getStartTime()*60);
            int[] end = Helpers.timeConversion(sector.getEndTime()*60);
            sectorInfo.setText((start[0]<10?"0"+start[0]:start[0])+":"+(start[1]<10?"0"+start[1]:start[1])+" - "+
                    (end[0]<10?"0"+end[0]:end[0])+":"+(end[1]<10?"0"+end[1]:end[1]));
            startHourText.setText((start[0]<10?"0"+start[0]:start[0]+""));
            startMinText.setText((start[1]<10?"0"+start[1]:start[1]+""));
            endHourText.setText((end[0]<10?"0"+end[0]:end[0]+""));
            endMinText.setText((end[1]<10?"0"+end[1]:end[1]+""));

        }

        @Override
        public void onClick(View v) {
            Sector sector = (Sector) data.get(getAdapterPosition());
            if(v.getId()==sectorInfo.getId()) {
                if (sectorInfoDetails.getVisibility() != View.VISIBLE) {
                    sector.setExpanded(true);
                    sectorInfoDetails.animate().alpha(1).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            super.onAnimationStart(animation);
                            sectorInfoDetails.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            notifyItemChanged(getAdapterPosition());
                        }
                    }).start();
                } else {
                    sector.setExpanded(false);
                    sectorInfoDetails.animate().alpha(0).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            sectorInfoDetails.setVisibility(View.GONE);
                            notifyItemChanged(getAdapterPosition());
                        }
                    }).start();
                }
            }else if(v.getId()==deleteSectorBtn.getId()){
                data.remove(getAdapterPosition());
                notifyItemRemoved(getAdapterPosition());
                for(int i =0;;i++){
                    Object s = data.get(i);
                    if(s instanceof SectorGroup && ((SectorGroup) s).getGroupId()==sector.getGroupId()){
                        ((SectorGroup) s).getSectorList().remove(sector);
                        notifyItemChanged(i);
                        break;
                    }
                }
            }else if(v.getId()==sectorColourImage.getId()){
                ColourPickerDialog colourPickerDialog = new ColourPickerDialog(colour -> {
                    //low alpha should show a warning
                    if(Color.alpha(colour)<255*.5f)
                        Toast.makeText(fragment.getContext(),"CAUTION - consider higher alpha for better visibility.",Toast.LENGTH_LONG).show();
                    sector.setColour(colour);
                    notifyItemChanged(getAdapterPosition());
                },((ColorDrawable)sectorColourImage.getBackground()).getColor());
                colourPickerDialog.show(fragment.getChildFragmentManager(),null);
            }else if(v.getId()==R.id.startHourUpBtn){
                sector.setStartTime(sector.getStartTime()+60);
                notifyItemChanged(getAdapterPosition());
            }else if(v.getId()==R.id.startHourDownBtn){
                sector.setStartTime(sector.getStartTime()-60);
                notifyItemChanged(getAdapterPosition());

            } else if(v.getId()==R.id.startMinUpBtn){
                sector.setStartTime(sector.getStartTime()+1);
                notifyItemChanged(getAdapterPosition());
            }else if(v.getId()==R.id.startMinDownBtn){
                sector.setStartTime(sector.getStartTime()-1);
                notifyItemChanged(getAdapterPosition());

            } else if(v.getId()==R.id.endHourUpBtn){
                sector.setEndTime(sector.getEndTime()+60);
                notifyItemChanged(getAdapterPosition());
            }else if(v.getId()==R.id.endHourDownBtn){
                sector.setEndTime(sector.getEndTime()-60);
                notifyItemChanged(getAdapterPosition());
            }else if(v.getId()==R.id.endMinUpBtn){
                sector.setEndTime(sector.getEndTime()+1);
                notifyItemChanged(getAdapterPosition());

            }else if(v.getId()==R.id.endMinDownBtn){
                sector.setEndTime(sector.getEndTime()-1);
                notifyItemChanged(getAdapterPosition());
            }
        }
    }
}
