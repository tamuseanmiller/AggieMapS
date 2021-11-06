package com.mrst.aggiemaps;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.divider.MaterialDivider;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class DirectionsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static List<ListItem> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    DirectionsAdapter(Context context, List<ListItem> data) {
        this.mInflater = LayoutInflater.from(context);
        mData = data;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case 1:
                return new LandmarkViewHolder(mInflater.inflate(R.layout.landmark_row, parent, false));
            case 0:
                return new FeatureViewHolder(mInflater.inflate(R.layout.list_row, parent, false));
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holderView, int position) {
        if (mData.size() <= position) return;
        switch (mData.get(position).tag) {
            case CATEGORY:
                LandmarkViewHolder holderCat = (LandmarkViewHolder) holderView;
                holderCat.categoryName.setText(mData.get(position).title);
                break;
            case RESULT:
                FeatureViewHolder holderList = (FeatureViewHolder) holderView;
                holderList.titleText.setText(mData.get(position).title);
                if (mData.get(position).subtitle != null)
                    holderList.subtitleText.setText(mData.get(position).subtitle);
                else
                    holderList.subtitleText.setVisibility(View.GONE);
                if (mData.size() - 1 != position && mData.get(position + 1).tag != MainActivity.SearchTag.CATEGORY)
                    holderList.divider.setVisibility(View.VISIBLE);
                Drawable icon = ContextCompat.getDrawable(mInflater.getContext(), mData.get(position).direction);
                icon.setTintList(ColorStateList.valueOf(ContextCompat.getColor(mInflater.getContext(), R.color.white)));
                holderList.directionIcon.setImageDrawable(icon);
                holderList.itemView.setEnabled(false);
                break;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        if (mData.size() <= position) return position;
        switch (mData.get(position).tag) {
            case RESULT:
                return 0;
            case CATEGORY:
                return 1;
        }
        return position;
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class FeatureViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView titleText;
        TextView subtitleText;
        FloatingActionButton directionIcon;
        RelativeLayout rlSearch;
        MaterialDivider divider;

        FeatureViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.title_text);
            subtitleText = itemView.findViewById(R.id.subtitle_text);
            directionIcon = itemView.findViewById(R.id.direction_icon);
            rlSearch = itemView.findViewById(R.id.rl_search);
            divider = itemView.findViewById(R.id.search_divider);
            itemView.setOnClickListener(this);
            rlSearch.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

            if (mClickListener != null) {
                mClickListener.onSEARCHClick(view, getAdapterPosition());
            }
        }
    }

    // stores and recycles views as they are scrolled off screen
    public class LandmarkViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView categoryName;

        LandmarkViewHolder(View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.landmark_text);

        }

        @Override
        public void onClick(View view) {

        }
    }

    // convenience method for getting data at click position
    ListItem getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onSEARCHClick(View view, int position);

    }
}