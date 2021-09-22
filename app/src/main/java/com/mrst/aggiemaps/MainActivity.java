package com.mrst.aggiemaps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewbinding.ViewBinding;

import android.graphics.Color;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.lapism.search.widget.MaterialSearchBar;
import com.lapism.search.widget.MaterialSearchView;

public class MainActivity extends AppCompatActivity {

    private MaterialSearchBar materialSearchBar;
    private MaterialSearchView materialSearchView;

    private void clearFocusOnSearch() {
        materialSearchView.clearFocus();
        materialSearchView.setVisibility(View.GONE);
    }

    private void requestFocusOnSearch() {
        ScriptGroup.Binding binding;
        materialSearchView.setVisibility(View.VISIBLE);
        materialSearchView.requestFocus();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set the status bar to be transparent
        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        NavHostFragment navHostFragment =
                (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        //navController.navigate(R.id.mapsFragment);

        materialSearchBar = findViewById(R.id.material_search_bar);
        materialSearchView = findViewById(R.id.material_search_view);

        materialSearchBar.setBackgroundColor(Color.TRANSPARENT);

        Toolbar toolbar = materialSearchBar.getToolbar();
        toolbar.setBackgroundColor(Color.TRANSPARENT);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setIcon(R.drawable.magnify);

        materialSearchBar.setHint(" Yeet ");
        materialSearchBar.setOnClickListener(v -> {
            requestFocusOnSearch();
        });
        materialSearchBar.setNavigationOnClickListener(v -> {
            requestFocusOnSearch();
        });

        materialSearchView.setVisibility(View.GONE);
//        materialSearchView.addView(recyclerView);
        materialSearchView.setNavigationIcon(ContextCompat.getDrawable(this, R.drawable.search_ic_outline_arrow_back_24));
        materialSearchView.setNavigationOnClickListener(v -> clearFocusOnSearch());
        materialSearchView.setHint("Yeet");
        materialSearchView.setBackgroundColor(ContextCompat.getColor(this, R.color.cardview_light_background));

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
}