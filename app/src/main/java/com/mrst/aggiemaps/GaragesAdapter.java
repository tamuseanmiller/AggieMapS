package com.mrst.aggiemaps;

import android.content.Context;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class GaragesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static List<Pair<String,Integer>> garageData;

    // data is passed into the constructor
    GaragesAdapter( List<Pair<String,Integer>> data) {
        garageData = data;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.garages_row, parent, false);
        ViewHolder viewHolder = new ViewHolder(listItem);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ViewHolder h = (ViewHolder) holder;
        h.garagesText.setText(garageData.get(position).first);
        h.spacesText.setText( garageData.get(position).second.toString());
    }

    @Override
    public int getItemCount() {
        return garageData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView garagesText;
//        public TextView spacesText;
        public Button spacesText;

        public ViewHolder(View itemView) {
            super(itemView);
            garagesText = itemView.findViewById(R.id.garagesRow_text);
            spacesText = itemView.findViewById(R.id.spacesRow);
        }

    }

}
