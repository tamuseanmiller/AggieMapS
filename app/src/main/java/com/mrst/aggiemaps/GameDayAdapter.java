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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class GameDayAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static List<BusRoute> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private final BusRouteTag tag;

    // data is passed into the constructor
    GameDayAdapter(Context context, List<BusRoute> data, BusRouteTag tag) {
        this.mInflater = LayoutInflater.from(context);
        mData = data;
        this.tag = tag;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (tag) {
            case ON_CAMPUS:
                return new GameDayAdapter.OnCampusViewHolder(mInflater.inflate(R.layout.recyclerview_route, parent, false));
            case OFF_CAMPUS:
                return new GameDayAdapter.OffCampusViewHolder(mInflater.inflate(R.layout.recyclerview_route, parent, false));
            case GAME_DAY:
                return new GameDayAdapter.GameDayViewHolder(mInflater.inflate(R.layout.recyclerview_route, parent, false));
            default:
                return new GameDayAdapter.FavoritesViewHolder(mInflater.inflate(R.layout.recyclerview_route, parent, false));
        }
    }

    private static int lightenColor(int color, double fraction) {
        return (int) Math.min(color + (color * fraction), 255);
    }

    public static int getLighterShade(int color, double fraction) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        red = lightenColor(red, fraction);
        green = lightenColor(green, fraction);
        blue = lightenColor(blue, fraction);
        int alpha = Color.alpha(color);
        return Color.argb(alpha, red, green, blue);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holderView, int position) {
        switch (tag) {
            case ON_CAMPUS:
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
                    holderOn.card.setBackgroundTintList(ColorStateList.valueOf(getLighterShade(route.color, 0)));
                    holderOn.routeName.setText(route.routeName);
                    holderOn.routeName.setSelected(true);
                }
                break;
            case OFF_CAMPUS:
                OffCampusViewHolder holderOff = (OffCampusViewHolder) holderView;
                if (position == 0) {
                    BusRoute route = mData.get(position);
                    holderOff.routeNumber.setText(route.routeNumber);
                    holderOff.routeNumber.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.white));
                    holderOff.card.setBackgroundTintList(ColorStateList.valueOf(route.color));
                    holderOff.routeName.setText(route.routeName);
                    holderOff.routeName.setSelected(true);
                    holderOff.routeName.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.white_60));
                } else {
                    BusRoute route = mData.get(position);
                    holderOff.routeNumber.setText(route.routeNumber);
                    holderOff.card.setBackgroundTintList(ColorStateList.valueOf(getLighterShade(route.color, 0)));
                    holderOff.routeName.setText(route.routeName);
                    holderOff.routeName.setSelected(true);
                }
                break;
            case GAME_DAY:
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
                    holderGame.routeNumber.setText(route.routeNumber);
                    holderGame.card.setBackgroundTintList(ColorStateList.valueOf(getLighterShade(route.color, 0)));
                    holderGame.routeName.setText(route.routeName);
                    holderGame.routeName.setSelected(true);
                }
                break;
            default:
                FavoritesViewHolder holderFav = (FavoritesViewHolder) holderView;
                if (position == 0) {
                    BusRoute route = mData.get(position);
                    holderFav.routeNumber.setText(route.routeNumber);
                    holderFav.routeNumber.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.white));
                    holderFav.card.setBackgroundTintList(ColorStateList.valueOf(route.color));
                    holderFav.routeName.setText(route.routeName);
                    holderFav.routeName.setSelected(true);
                    holderFav.routeName.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.white_60));
                } else {
                    BusRoute route = mData.get(position);
                    holderFav.routeNumber.setText(route.routeNumber);
                    holderFav.card.setBackgroundTintList(ColorStateList.valueOf(getLighterShade(route.color, 0)));
                    holderFav.routeName.setText(route.routeName);
                    holderFav.routeName.setSelected(true);
                }
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        switch (tag) {
            case ON_CAMPUS:
                return 0;
            case OFF_CAMPUS:
                return 1;
            case GAME_DAY:
                return 2;
            default:
                return 3;
        }
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

        FavoritesViewHolder(View itemView) {
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

    // stores and recycles views as they are scrolled off screen
    public class OffCampusViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView routeName;
        TextView routeNumber;
        MaterialCardView card;

        OffCampusViewHolder(View itemView) {
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
