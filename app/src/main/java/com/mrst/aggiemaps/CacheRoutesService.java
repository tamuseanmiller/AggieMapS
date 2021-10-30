package com.mrst.aggiemaps;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import com.esri.core.geometry.Point;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CacheRoutesService extends Service {

    private OkHttpClient client = new OkHttpClient();
    private Notification.Builder notification;
    private NotificationManager service;
    private final String channelId = "cache_service";

    /*
     * Method to make a GET request to a given URL
     * returns response body as String
     */
    private String getApiCall(String url) {
        try {
            // Create request
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            // Execute request and get response
            Response response = client.newCall(request).execute();
            ResponseBody body = response.body();

            return Objects.requireNonNull(body).string(); // Return the response as a string

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
     * Method to convert transportation coords to LatLng
     * returns Point
     */
    private static Point convertWebMercatorToLatLng(final double x, final double y) {
        CRSFactory crsFactory = new CRSFactory();
        CoordinateReferenceSystem targetCRS = crsFactory.createFromName("EPSG:4236");
        CoordinateReferenceSystem sourceCRS = crsFactory.createFromName("EPSG:3857");
        CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
        CoordinateTransform wgsToUtm = ctFactory.createTransform(sourceCRS, targetCRS);
        ProjCoordinate result = new ProjCoordinate();
        wgsToUtm.transform(new ProjCoordinate(x, y), result);

        return new Point(result.x, result.y);
    }

    /*
     * Helper method to convert a drawable to a BitmapDescriptor for use with a maps marker
     * Taken from somewhere similar to here
     * https://stackoverflow.com/questions/42365658/custom-marker-in-google-maps-in-android-with-vector-asset-icon
     */
    private static BitmapDescriptor BitmapFromVector(Context context, int vectorResId, int color, int modifySize) {
        // below line is use to generate a drawable.
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);

        // below line is use to set bounds to our vector drawable.
        assert vectorDrawable != null;
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth() + modifySize, vectorDrawable.getIntrinsicHeight() + modifySize);
        vectorDrawable.setTintList(ColorStateList.valueOf(color));

        // below line is use to create a bitmap for our
        // drawable which we have added.
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        // below line is use to add bitmap in our canvas.
        Canvas canvas = new Canvas(bitmap);

        // below line is use to draw our
        // vector drawable in canvas.
        vectorDrawable.draw(canvas);

        // after generating our bitmap we are returning our bitmap.
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void cacheAllRoutes() {
        try {
            String str = getApiCall("https://transport.tamu.edu/BusRoutesFeed/api/Routes");
            if (str == null) return;  // TODO: Call dialog or retry
            JSONArray routes = new JSONArray(str);
            notification.setProgress(routes.length(), 0, false);
            service.notify(2, notification.build());

            // Traverse through all routes
            for (int i = 0; i < routes.length(); i++) {
                // See what group the route lies in, then add it
                String shortName = routes.getJSONObject(i).getString("ShortName");

                // Update Notification
                notification.setProgress(routes.length(), i, false);
                notification.setContentText("Caching Route " + shortName + ", " + (routes.length() - i - 1) + " routes left");
                service.notify(2, notification.build());

                cacheRoute(shortName);
            }

            // End notification
            stopSelf();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void cacheRoute(String routeNo) {
        try {
            // Make the API call
            String str = getApiCall("https://transport.tamu.edu/BusRoutesFeed/api/route/" + routeNo + "/pattern");

            // Create a builder for bounds to zoom to
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            // Parse the waypoints and stops
            JSONArray stops = new JSONArray(str);
            PolylineOptions polylineOptions = new PolylineOptions();
            LatLng first = null;

            ArrayList<Pair<String, LatLng>> busStops = new ArrayList<>();
            for (int i = 0; i < stops.length(); i++) {

                // Convert point and add to the polyline and builder
                Point p = convertWebMercatorToLatLng(stops.getJSONObject(i).getDouble("Longtitude"),
                        stops.getJSONObject(i).getDouble("Latitude"));
                double y = p.getX();
                double x = p.getY();
                if (i == 0) {
                    first = new LatLng(x, y);
                }
                polylineOptions.add(new LatLng(x, y));
                builder.include(new LatLng(x, y));

                // Add bus stop circles
                if (stops.getJSONObject(i).getString("PointTypeCode").equals("1")) {
                    MarkerOptions marker = new MarkerOptions();
                    String title = stops.getJSONObject(i).getJSONObject("Stop").getString("Name");
                    marker.flat(true);
                    marker.icon(BitmapFromVector(getApplicationContext(), R.drawable.checkbox_blank_circle, ContextCompat.getColor(getApplicationContext(), R.color.accent), -20));
                    marker.title(title);
                    marker.anchor(0.5F, 0.5F);
                    marker.position(new LatLng(x, y));
                    busStops.add(new Pair<>(title, new LatLng(x, y)));
                }
            }

            // Animate the camera to the new bounds
            LatLngBounds bounds = builder.build();

            // Draw polyline
            assert first != null;
            polylineOptions.add(first);
            polylineOptions.color(ContextCompat.getColor(getApplicationContext(), R.color.accent));
            polylineOptions.width(10);
            polylineOptions.geodesic(true);
            polylineOptions.pattern(null);
            polylineOptions.clickable(true);
            AggieBusRoute newRoute = new AggieBusRoute(polylineOptions, busStops, bounds.northeast, bounds.southwest);
            AggieBusRoute.writeData(getApplicationContext(), newRoute, routeNo);

        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }
        Log.v("ROUTECACHE", "Cached  " + routeNo);
    }

    public CacheRoutesService() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        service.cancelAll();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, AppIntroduction.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        String channelName = "Cache Background Service";

        // Create the notification manager and channel
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_HIGH);
        chan.setLightColor(getColor(R.color.accent));
        chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        chan.setSound(null, null);
        chan.enableVibration(false);
        service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);

        // Create the notification
        notification = new Notification.Builder(this, channelId)
                .setContentTitle("Caching Routes")
                .setSmallIcon(R.drawable.bus)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setProgress(100, 0, true)
                .setOnlyAlertOnce(true);
        Notification n = notification.build();

        // Start the actual service
        service.notify(2, n);
        startForeground(2, n);
        service.getNotificationChannel(channelId).setImportance(NotificationManager.IMPORTANCE_LOW);
        new Thread(this::cacheAllRoutes).start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}