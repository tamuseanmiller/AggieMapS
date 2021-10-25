package com.mrst.aggiemaps;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.divider.MaterialDivider;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class RecentSearchesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static List<ListItem> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    RecentSearchesAdapter(Context context, List<ListItem> data) {
        this.mInflater = LayoutInflater.from(context);
        mData = data;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case 1:
                return new RecentSearchesAdapter.CategoryViewHolder(mInflater.inflate(R.layout.search_category, parent, false));
            case 0:
                return new RecentSearchesAdapter.ListViewHolder(mInflater.inflate(R.layout.list_col, parent, false));
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holderView, int position) {
        if (mData.size() <= position) return;
        switch (mData.get(position).tag) {
            case CATEGORY:
                CategoryViewHolder holderCat = (CategoryViewHolder) holderView;
                holderCat.categoryName.setText(mData.get(position).title);
                break;
            case RESULT:
                ListViewHolder holderList = (ListViewHolder) holderView;
                holderList.titleText.setText(mData.get(position).title);
                if (mData.get(position).subtitle != null) {
                    holderList.subtitleText.setText(mData.get(position).subtitle);
                    holderList.subtitleText.setSelected(true);
                } else {
                    holderList.subtitleText.setVisibility(View.GONE);
                }

                // Set icon if it's passed
                if (mData.get(position).direction != 0) {
                    Drawable icon = ContextCompat.getDrawable(mInflater.getContext(), mData.get(position).direction);
                    icon.setTintList(ColorStateList.valueOf(ContextCompat.getColor(mInflater.getContext(), R.color.white)));
                    holderList.directionIcon.setImageDrawable(icon);
                }

                // Set background color
                if (mData.get(position).color != 0)
                    holderList.directionIcon.setBackgroundTintList(ColorStateList.valueOf(mData.get(position).color));
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
    public class ListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView titleText;
        TextView subtitleText;
        FloatingActionButton directionIcon;
        RelativeLayout rlSearch;
        MaterialDivider divider;

        ListViewHolder(View itemView) {
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
                mClickListener.onRecentSearchClick(view, getAdapterPosition());
            }
        }
    }

    // stores and recycles views as they are scrolled off screen
    public class CategoryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView categoryName;

        CategoryViewHolder(View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.category_name);

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
        void onRecentSearchClick(View view, int position);

    }
}