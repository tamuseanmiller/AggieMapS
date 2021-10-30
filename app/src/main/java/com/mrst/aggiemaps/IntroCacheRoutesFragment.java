package com.mrst.aggiemaps;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.MapsInitializer;
import com.google.android.material.button.MaterialButton;

import org.jetbrains.annotations.Contract;

public class IntroCacheRoutesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        MapsInitializer.initialize(requireActivity());
        return inflater.inflate(R.layout.custom_intro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MaterialButton tv = view.findViewById(R.id.cacheBusesBtn);
        tv.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), CacheRoutesService.class);
            requireActivity().startForegroundService(i);
        });
    }

    @NonNull
    @Contract(" -> new")
    public static IntroCacheRoutesFragment newInstance() {
        return new IntroCacheRoutesFragment();
    }
}
