package com.mrst.aggiemaps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.util.AttributeSet;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.material.appbar.AppBarLayout;
import com.lapism.search.widget.MaterialSearchBar;
import com.lapism.search.widget.MaterialSearchView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MaterialSearchBar materialSearchBar;
    private MaterialSearchView materialSearchView;

    private void clearFocusOnSearch() {
        materialSearchView.clearFocus();
        materialSearchView.setVisibility(View.GONE);
        materialSearchBar.setVisibility(View.VISIBLE);
        showSystemUI();
    }

    private void requestFocusOnSearch() {
        ScriptGroup.Binding binding;
        materialSearchView.setVisibility(View.VISIBLE);
        materialSearchView.requestFocus();
        materialSearchBar.setVisibility(View.VISIBLE);
        showSystemUI();
        materialSearchBar.setVisibility(View.GONE);
        hideSystemUI();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the status bar to be transparent
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Initialize the SearchBar and View
        materialSearchBar = findViewById(R.id.material_search_bar);
        materialSearchView = findViewById(R.id.material_search_view);

        // Set the toolbar and actionbar
        Toolbar toolbar = materialSearchBar.getToolbar();
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        Drawable nav = ContextCompat.getDrawable(this, R.drawable.magnify);
        if (nav != null && actionBar != null) {
            nav.setTint(getColor(R.color.foreground));
            actionBar.setIcon(nav);
        }

        // Set Search Bar Settings
        materialSearchBar.setHint("Aggie MapS");
        materialSearchBar.setBackgroundColor(getColor(R.color.background));
        materialSearchBar.setOnClickListener(v -> {
            requestFocusOnSearch();
        });
        materialSearchBar.setNavigationOnClickListener(v -> {
            requestFocusOnSearch();
        });

        // Set SearchView Settings
        List<String> l = new ArrayList<>();
        l.add("Test1");
        l.add("Test2");
        l.add("Test3");
        l.add("Test4");
        RecyclerViewAdapterRandom recyclerViewAdapterRandom = new RecyclerViewAdapterRandom(this, l);
        RecyclerView recyclerRandom = new RecyclerView(this);
        recyclerRandom.setAdapter(recyclerViewAdapterRandom);
        materialSearchView.addView(recyclerRandom);
        Drawable navigationIcon = ContextCompat.getDrawable(this, R.drawable.search_ic_outline_arrow_back_24);
        navigationIcon.setTintList(ColorStateList.valueOf(getColor(R.color.foreground)));
        materialSearchView.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.search_ic_outline_arrow_back_24));
        materialSearchView.setVisibility(View.GONE);
        materialSearchView.setHint("Try Building Numbers/Names");
        materialSearchView.setBackgroundColor(ContextCompat.getColor(this, R.color.background));

        // Set OnClick Listeners
        materialSearchView.setNavigationOnClickListener(v -> clearFocusOnSearch());

        materialSearchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(@NonNull CharSequence charSequence) {
//              adapter.filter(newText)
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(@NonNull CharSequence charSequence) {
                return true;
            }
        });

        materialSearchView.setOnFocusChangeListener(v -> {

        });


    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

}

