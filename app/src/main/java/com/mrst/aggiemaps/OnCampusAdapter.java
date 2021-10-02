package com.mrst.aggiemaps;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class OnCampusAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static List<BusRoute> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private final Palette palette;
    private BusRouteTag tag;

    // data is passed into the constructor
    OnCampusAdapter(Context context, List<BusRoute> data, BusRouteTag tag) {
        this.mInflater = LayoutInflater.from(context);
        mData = data;
        palette = new Palette(mInflater.getContext());
        this.tag = tag;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new OnCampusAdapter.OnCampusViewHolder(mInflater.inflate(R.layout.route_card, parent, false));
    }

    public static int manipulateColor(int color, float factor) {
        int a = Color.alpha(color);
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.argb(a,
                Math.min(r, 255),
                Math.min(g, 255),
                Math.min(b, 255));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holderView, int position) {
        OnCampusViewHolder holderOn = (OnCampusViewHolder) holderView;
//        holderOn.card.setLayoutParams(new LinearLayout.LayoutParams((int)(holderOn.card.getLayoutParams().width * getDeviceParams()), (int)(holderOn.card.getLayoutParams().height * getDeviceParams())));
//        holderOn.routeName.setTextSize(holderOn.routeName.getTextSize() * getDeviceParams());
//        holderOn.routeNumber.setTextSize(holderOn.routeNumber.getTextSize() * getDeviceParams());
        if (position == 0) {
            BusRoute route = mData.get(position);
            holderOn.routeNumber.setText(route.routeNumber);
            holderOn.routeNumber.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.white));
            holderOn.card.setBackgroundTintList(ColorStateList.valueOf(route.color));
            holderOn.routeName.setText(route.routeName);
            holderOn.routeName.setSelected(true);
            holderOn.routeName.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.white_60));
            holderOn.favoriteButton.setVisibility(View.INVISIBLE);
        } else {
            BusRoute route = mData.get(position);
            holderOn.routeNumber.setText(route.routeNumber); // Set route number text

            // If no color is given, pick a random color, otherwise find the nearest color
            if (route.color == Integer.MAX_VALUE) route.color = palette.pickRandomColor();
            else route.color = palette.findClosestPaletteColorTo(route.color);
            holderOn.card.setCardBackgroundColor(route.color);
            holderOn.card.setRippleColor(ColorStateList.valueOf(manipulateColor(route.color, 0.8f)));

            // If the shade of the background color is too light, change text color to black
            if (ColorUtils.calculateLuminance(route.color) > 0.6) {
                holderOn.routeName.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.black_60));
                holderOn.routeNumber.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.black));
            }
            holderOn.routeName.setText(route.routeName); // Set route name text
            holderOn.routeName.setSelected(true); // Set selected so will marquee if needed
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
        MaterialButton favoriteButton;

        OnCampusViewHolder(View itemView) {
            super(itemView);
            routeName = itemView.findViewById(R.id.route_name);
            routeNumber = itemView.findViewById(R.id.route_number);
            favoriteButton = itemView.findViewById(R.id.favorite_button);
            card = itemView.findViewById(R.id.route_card);
            itemView.setOnClickListener(this);
            card.setOnClickListener(this);
            favoriteButton.setOnClickListener(this);

        }

        @Override
        public void onClick(View view) {

            if (mClickListener != null) {
                mClickListener.onItemClick(view, mData.get(getAdapterPosition()), getAdapterPosition(), tag);
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
        void onItemClick(View view, BusRoute busRoute, int position, BusRouteTag tag);

    }
}
