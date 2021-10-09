package com.mrst.aggiemaps;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class timelineDialogFragment extends DialogFragment {

    private View view;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.timetable_dialog, container, false);

        @SuppressLint("ResourceType") TextView stopText = view.findViewById(R.id.viewMoreStop);
        stopText.setText(getArguments().getString("nextStop"));
        Log.e("GOT TIMES ARRAY", String.valueOf(getArguments().getStringArrayList("timesArray")));

        return view;
    }
}

