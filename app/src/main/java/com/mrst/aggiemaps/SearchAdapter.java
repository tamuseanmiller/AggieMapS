package com.mrst.aggiemaps;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class SearchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    enum SearchTag {
        CATEGORY,
        LIST
    }

    private static List<SearchResult> mData;
    private final LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private SearchTag tag;

    // data is passed into the constructor
    SearchAdapter(Context context, List<SearchResult> data, SearchTag tag) {
        this.mInflater = LayoutInflater.from(context);
        mData = data;
        this.tag = tag;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (tag) {
            case CATEGORY:
                return new SearchAdapter.CategoryViewHolder(mInflater.inflate(R.layout.search_category, parent, false));
            case LIST:
                return new SearchAdapter.ListViewHolder(mInflater.inflate(R.layout.search_row, parent, false));
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holderView, int position) {
        switch (tag) {
            case CATEGORY:
                CategoryViewHolder holderCat = (CategoryViewHolder) holderView;
                holderCat.categoryName.setText(mData.get(position).title);

                break;
            case LIST:
                ListViewHolder holderList = (ListViewHolder) holderView;
                holderList.titleText.setText(mData.get(position).title);
                holderList.subtitleText.setText(mData.get(position).subtitle);
                break;
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
    public class ListViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView titleText;
        TextView subtitleText;
        FloatingActionButton directionIcon;
        RelativeLayout rlSearch;

        ListViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.title_text);
            subtitleText = itemView.findViewById(R.id.subtitle_text);
            directionIcon = itemView.findViewById(R.id.direction_icon);
            rlSearch = itemView.findViewById(R.id.rl_search);
            itemView.setOnClickListener(this);
            rlSearch.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {

            if (mClickListener != null) {
                mClickListener.onItemClick(view, getAdapterPosition());
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
    SearchResult getItem(int id) {
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