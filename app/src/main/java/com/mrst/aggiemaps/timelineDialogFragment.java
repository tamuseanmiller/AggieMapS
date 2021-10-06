package com.mrst.aggiemaps;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class timelineDialogFragment extends DialogFragment {

    private View d;

//    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
//
//    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        d = inflater.inflate(R.layout.timetable_dialog, container, false);
        Log.e("HERE", getArguments().toString());
        @SuppressLint("ResourceType") TextView stopText = d.findViewById(R.id.viewMoreStop);
        stopText.setText(getArguments().getString("nextStop"));
        return d;
    }
}

