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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import org.w3c.dom.Text;

import java.util.List;

public class RecyclerViewAdapterBusRoutes extends RecyclerView.Adapter<RecyclerViewAdapterBusRoutes.ViewHolder> {

    private static List<BusRoute> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    RecyclerViewAdapterBusRoutes(Context context, List<BusRoute> data) {
        this.mInflater = LayoutInflater.from(context);
        mData = data;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.recyclerview_route, parent, false);
        return new ViewHolder(view);
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
    public void onBindViewHolder(@NonNull RecyclerViewAdapterBusRoutes.ViewHolder holder, int position) {

        if (position == 0) {
            BusRoute route = mData.get(position);
            holder.routeNumber.setText(route.routeNumber);
            holder.routeNumber.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.white));
            holder.card.setBackgroundTintList(ColorStateList.valueOf(route.color));
            holder.routeName.setText(route.routeName);
            holder.routeName.setSelected(true);
            holder.routeName.setTextColor(ContextCompat.getColor(mInflater.getContext(), R.color.white_60));
        } else {
            BusRoute route = mData.get(position);
            holder.routeNumber.setText(route.routeNumber);
            holder.card.setBackgroundTintList(ColorStateList.valueOf(getLighterShade(route.color, 0)));
            holder.routeName.setText(route.routeName);
            holder.routeName.setSelected(true);
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
        TextView routeName;
        TextView routeNumber;
        MaterialCardView card;

        ViewHolder(View itemView) {
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
