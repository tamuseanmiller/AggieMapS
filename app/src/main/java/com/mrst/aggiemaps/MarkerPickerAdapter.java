package com.mrst.aggiemaps;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class MarkerPickerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static List<POIPickerModalSheet.MarkerStruct> mData;
    private final LayoutInflater mInflater;
    private MarkerPickerAdapter.ItemClickListener mClickListener;

    // data is passed into the constructor
    MarkerPickerAdapter(Context context, List<POIPickerModalSheet.MarkerStruct> data) {
        this.mInflater = LayoutInflater.from(context);
        mData = data;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(mInflater.inflate(R.layout.marker_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holderView, int position) {
        MarkerPickerAdapter.ViewHolder mHolder = (MarkerPickerAdapter.ViewHolder) holderView;

        POIPickerModalSheet.MarkerStruct marker = mData.get(position);
        mHolder.markerTitle.setText(marker.text);
        mHolder.markerImage.setBackground(marker.drawable);

        // If the marker is selected
        if (marker.selected) {
            mHolder.markerCard.setStrokeColor(ContextCompat.getColor(mInflater.getContext(), R.color.blue_300));
        } else {
            mHolder.markerCard.setStrokeColor(ContextCompat.getColor(mInflater.getContext(), R.color.background));
        }

    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView markerTitle;
        ShapeableImageView markerImage;
        MaterialCardView markerCard;
        LinearLayout llMarker;

        ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            markerImage = itemView.findViewById(R.id.marker_image);
            markerCard = itemView.findViewById(R.id.marker_card);
            markerTitle = itemView.findViewById(R.id.marker_title);
            llMarker = itemView.findViewById(R.id.ll_marker);
            llMarker.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

            if (mClickListener != null) {
                mClickListener.onItemClick(view, getBindingAdapterPosition());
            }
        }
    }

    // convenience method for getting data at click position
    POIPickerModalSheet.MarkerStruct getItem(int id) {
        return mData.get(id);
    }



    // allows clicks events to be caught
    void setClickListener(MarkerPickerAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);

    }
}
