package com.mrst.aggiemaps;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;

public class BottomPagerAdapter extends FragmentStateAdapter {
    public ArrayList<Fragment> fragmentList;

    public BottomPagerAdapter(FragmentActivity fa) {
        super(fa);
        fragmentList = new ArrayList<>();
    }

    public ArrayList<Fragment> getFragmentList() {
        return fragmentList;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                Fragment directionsFragment = new DirectionsFragment();
                fragmentList.add(directionsFragment);
                return directionsFragment;
            case 0:
                Fragment blankFragment = new BlankFragment();
                fragmentList.add(blankFragment);
                return blankFragment;
            default:
                Fragment mapsFragment = new MapsFragment();
                fragmentList.add(mapsFragment);
                return mapsFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}
