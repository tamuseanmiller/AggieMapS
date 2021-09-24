package com.mrst.aggiemaps;

import android.content.Context;
import android.content.res.XmlResourceParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*
 * Class taken https://codereview.stackexchange.com/questions/115046/fast-way-to-find-the-most-similar-color-in-an-array
 */
public final class Palette {

    public List<PaletteColor> colors;
    private Context context;

    public Palette(Context context) {
        colors = new ArrayList<>();
        this.context = context;
        int[] colorsInt = context.getResources().getIntArray(R.array.material_colors);
        for (int j : colorsInt) {
            colors.add(new PaletteColor(j));
        }
    }

    public int findClosestPaletteColorTo(final int color) {
        int closestColor = -1;
        int closestDistance = Integer.MAX_VALUE;
        for (final PaletteColor paletteColor : this.colors) {
            final int distance = paletteColor.distanceTo(color);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestColor = paletteColor.asInt();
            }
        }
        return closestColor;
    }

    private static final class PaletteColor {
        private final int r;
        private final int g;
        private final int b;
        private final int color;

        public PaletteColor(final int color) {
            this.r = ((color & 0xff000000) >>> 24);
            this.g = ((color & 0x00ff0000) >>> 16);
            this.b = ((color & 0x0000ff00) >>> 8);
            this.color = color;
        }

        public int distanceTo(final int color) {
            final int deltaR = this.r - ((color & 0xff000000) >>> 24);
            final int deltaG = this.g - ((color & 0x00ff0000) >>> 16);
            final int deltaB = this.b - ((color & 0x0000ff00) >>> 8);
            return (deltaR * deltaR) + (deltaG * deltaG) + (deltaB * deltaB);
        }

        public int asInt() {
            return this.color;
        }
    }
}
