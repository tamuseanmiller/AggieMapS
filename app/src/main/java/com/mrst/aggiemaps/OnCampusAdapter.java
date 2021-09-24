package com.mrst.aggiemaps;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class OnCampusAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static List<BusRoute> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private final Palette palette;

    // data is passed into the constructor
    OnCampusAdapter(Context context, List<BusRoute> data, BusRouteTag tag) {
        this.mInflater = LayoutInflater.from(context);
        mData = data;
        palette = new Palette(mInflater.getContext());
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new OnCampusAdapter.OnCampusViewHolder(mInflater.inflate(R.layout.recyclerview_route, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holderView, int position) {
        OnCampusViewHolder holderOn = (OnCampusViewHolder) holderView;
        if (position == 0) {
            BusRoute route = mData.get(position);
            holderOn.routeNumber.setText(route.routeNumber);
            holderOn.routeNumber.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.white));
            holderOn.card.setBackgroundTintList(ColorStateList.valueOf(route.color));
            holderOn.routeName.setText(route.routeName);
            holderOn.routeName.setSelected(true);
            holderOn.routeName.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.white_60));
        } else {
            BusRoute route = mData.get(position);
            holderOn.routeNumber.setText(route.routeNumber);
            if (route.color == Integer.MAX_VALUE) route.color = palette.pickRandomColor();
            else route.color = palette.findClosestPaletteColorTo(route.color);
            holderOn.card.setCardBackgroundColor(route.color);
            if (ColorUtils.calculateLuminance(route.color) > 0.3) {
                holderOn.routeName.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.black_60));
                holderOn.routeNumber.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.black));
            }
            holderOn.routeName.setText(route.routeName);
            holderOn.routeName.setSelected(true);
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
    public class OnCampusViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView routeName;
        TextView routeNumber;
        MaterialCardView card;

        OnCampusViewHolder(View itemView) {
            super(itemView);
            routeName = itemView.findViewById(R.id.route_name);
            routeNumber = itemView.findViewById(R.id.route_number);
            card = itemView.findViewById(R.id.route_card);
            itemView.setOnClickListener(this);
            card.setOnClickListener(this);

        }

        @Override
        public void onClick(View view) {

            if (mClickListener != null) {
                mClickListener.onItemClick(view, getAdapterPosition());
            }
        }
    }

    // convenience method for getting data at click position
    BusRoute getItem(int id) {
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
