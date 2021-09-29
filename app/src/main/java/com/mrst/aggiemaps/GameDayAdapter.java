package com.mrst.aggiemaps;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
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

public class GameDayAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static List<BusRoute> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private final Palette palette;

    // data is passed into the constructor
    GameDayAdapter(Context context, List<BusRoute> data, BusRouteTag tag) {
        this.mInflater = LayoutInflater.from(context);
        mData = data;
        palette = new Palette(mInflater.getContext());
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new GameDayAdapter.GameDayViewHolder(mInflater.inflate(R.layout.route_card, parent, false));
    }

    public static int manipulateColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a,
                Math.min(r,255),
                Math.min(g,255),
                Math.min(b,255));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holderView, int position) {
        GameDayViewHolder holderGame = (GameDayViewHolder) holderView;
        if (position == 0) {
            BusRoute route = mData.get(position);
            holderGame.routeNumber.setText(route.routeNumber);
            holderGame.routeNumber.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.white));
            holderGame.card.setBackgroundTintList(ColorStateList.valueOf(route.color));
            holderGame.routeName.setText(route.routeName);
            holderGame.routeName.setSelected(true);
            holderGame.routeName.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.white_60));
        } else {
            BusRoute route = mData.get(position);
            holderGame.routeNumber.setText(route.routeNumber); // Set route number text

            // If no color is given, pick a random color, otherwise find the nearest color
            if (route.color == Integer.MAX_VALUE) route.color = palette.pickRandomColor();
            else route.color = palette.findClosestPaletteColorTo(route.color);
            holderGame.card.setCardBackgroundColor(route.color);
            holderGame.card.setRippleColor(ColorStateList.valueOf(manipulateColor(route.color, 0.8f)));

            // If the shade of the background color is too light, change text color to black
            if (ColorUtils.calculateLuminance(route.color) > 0.6) {
                holderGame.routeName.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.black_60));
                holderGame.routeNumber.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.black));
            }
            holderGame.routeName.setText(route.routeName); // Set route name text
            holderGame.routeName.setSelected(true); // Set selected so will marquee if needed
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
    public class GameDayViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView routeName;
        TextView routeNumber;
        MaterialCardView card;

        GameDayViewHolder(View itemView) {
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
                mClickListener.onItemClick(view, mData.get(getAdapterPosition()));
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
        void onItemClick(View view, BusRoute busRoute);

    }
}
