package com.mrst.aggiemaps;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class RecyclerViewAdapterRandom extends RecyclerView.Adapter<RecyclerViewAdapterRandom.ViewHolder> {

    private static List<String> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    RecyclerViewAdapterRandom(Context context, List<String> data) {
        this.mInflater = LayoutInflater.from(context);
        mData = data;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.search_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapterRandom.ViewHolder holder, int position) {

        String searchEntry = mData.get(position);
        holder.subtitleText.setText(searchEntry);
        holder.titleText.setText(searchEntry);

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
        TextView titleText;
        TextView subtitleText;
        MaterialButton directionIcon;

        ViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.title_text);
            subtitleText = itemView.findViewById(R.id.subtitle_text);
            directionIcon = itemView.findViewById(R.id.direction_icon);
            itemView.setOnClickListener(this);
            subtitleText.setOnClickListener(this);

        }

        @Override
        public void onClick(View view) {

            if (mClickListener != null) {
                mClickListener.onItemClick(view, getAdapterPosition());
            }
        }
    }

    // convenience method for getting data at click position
    String getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);

    }
}
