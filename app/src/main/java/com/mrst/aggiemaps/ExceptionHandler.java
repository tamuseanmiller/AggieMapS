package com.mrst.aggiemaps;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionHandler implements
        java.lang.Thread.UncaughtExceptionHandler {
    private final Activity myContext;

    public ExceptionHandler(Activity context) {
        myContext = context;
    }

    public void uncaughtException(@NonNull Thread thread, Throwable exception) {
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));

        String LINE_SEPARATOR = "\n";

        Intent intent = new Intent(myContext, MainActivity.class);
        String errorReport = "************ CAUSE OF ERROR ************\n\n" +
                stackTrace.toString() +
                "\n************ DEVICE INFORMATION ***********\n" +
                "Brand: " +
                Build.BRAND +
                LINE_SEPARATOR +
                "Device: " +
                Build.DEVICE +
                LINE_SEPARATOR +
                "Model: " +
                Build.MODEL +
                LINE_SEPARATOR +
                "Id: " +
                Build.ID +
                LINE_SEPARATOR +
                "Product: " +
                Build.PRODUCT +
                LINE_SEPARATOR +
                "\n************ FIRMWARE ************\n" +
                "SDK: " +
                Build.VERSION.PREVIEW_SDK_INT +
                LINE_SEPARATOR +
                "Release: " +
                Build.VERSION.RELEASE +
                LINE_SEPARATOR +
                "Incremental: " +
                Build.VERSION.INCREMENTAL +
                LINE_SEPARATOR;
        intent.putExtra("error", errorReport);
        myContext.startActivity(intent);

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

}