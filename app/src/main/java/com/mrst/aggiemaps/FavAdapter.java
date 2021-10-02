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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class FavAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static List<BusRoute> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private final Palette palette;
    private final BusRouteTag tag;

    // data is passed into the constructor
    FavAdapter(Context context, List<BusRoute> data, BusRouteTag tag) {
        this.mInflater = LayoutInflater.from(context);
        mData = data;
        palette = new Palette(mInflater.getContext());
        this.tag = tag;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FavAdapter.FavoritesViewHolder(mInflater.inflate(R.layout.route_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holderView, int position) {
        FavoritesViewHolder holderFav = (FavoritesViewHolder) holderView;
        holderFav.favoriteButton.setIcon(ContextCompat.getDrawable(mInflater.getContext(), R.drawable.star));
        if (position == 0) {
            BusRoute route = mData.get(position);
            holderFav.routeNumber.setText(route.routeNumber);
            holderFav.routeNumber.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.white));
            holderFav.card.setBackgroundTintList(ColorStateList.valueOf(route.color));
            holderFav.routeName.setText(route.routeName);
            holderFav.routeName.setSelected(true);
            holderFav.routeName.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.white_60));
            holderFav.favoriteButton.setVisibility(View.INVISIBLE);
        } else {
            BusRoute route = mData.get(position);
            holderFav.routeNumber.setText(route.routeNumber); // Set route number text

            // If no color is given, pick a random color, otherwise find the nearest color
            if (route.color == Integer.MAX_VALUE) route.color = palette.pickRandomColor();
            else route.color = palette.findClosestPaletteColorTo(route.color);
            holderFav.card.setCardBackgroundColor(route.color);
            holderFav.card.setRippleColor(ColorStateList.valueOf(route.color));

            // If the shade of the background color is too light, change text color to black
            if (ColorUtils.calculateLuminance(route.color) > 0.6) {
                holderFav.routeName.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.black_60));
                holderFav.routeNumber.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.black));
            }
            holderFav.routeName.setText(route.routeName); // Set route name text
            holderFav.routeName.setSelected(true); // Set selected so will marquee if needed
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
    public class FavoritesViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView routeName;
        TextView routeNumber;
        MaterialCardView card;
        MaterialButton favoriteButton;

        FavoritesViewHolder(View itemView) {
            super(itemView);
            routeName = itemView.findViewById(R.id.route_name);
            routeNumber = itemView.findViewById(R.id.route_number);
            card = itemView.findViewById(R.id.route_card);
            itemView.setOnClickListener(this);
            card.setOnClickListener(this);
            favoriteButton = itemView.findViewById(R.id.favorite_button);
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
