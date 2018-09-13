package com.developers.uberanimation;


import android.animation.ValueAnimator;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.LinearInterpolator;

import com.developers.uberanimation.utils.MQTTHelper;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jakewharton.rxrelay2.PublishRelay;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class DriverModeActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = DriverModeActivity.class.getSimpleName();

    private GoogleMap mMap;
    private PublishRelay<LatLng> latLngPublishRelay = PublishRelay.create();
    private Disposable latLngDisposable;
    private Marker marker;
    private float v;
    private int emission = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_mode);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        startMqtt();
    }

    @Override
    protected void onResume() {
        super.onResume();
        latLngDisposable = latLngPublishRelay
                .buffer(2)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<LatLng>>() {

                    @Override
                    public void accept(List<LatLng> latLngs) throws Exception {
                        emission++;
                        animateCarOnMap(latLngs);
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!latLngDisposable.isDisposed()) {
            latLngDisposable.dispose();
        }
    }

    /**
     * Take the emissions from the Rx Relay as a pair of LatLng and starts the animation of
     * car on map by taking the 2 pair of LatLng's.
     *
     * @param latLngs List of LatLng emitted by Rx Relay with size two.
     */
    private void animateCarOnMap(final List<LatLng> latLngs) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : latLngs) {
            builder.include(latLng);
        }
        LatLngBounds bounds = builder.build();
        CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2);
        mMap.animateCamera(mCameraUpdate);
        if (emission == 1) {
            marker = mMap.addMarker(new MarkerOptions().position(latLngs.get(0))
                    .flat(true)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car)));
        }
        marker.setPosition(latLngs.get(0));
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
        valueAnimator.setDuration(1000);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                v = valueAnimator.getAnimatedFraction();
                double lng = v * latLngs.get(1).longitude + (1 - v)
                        * latLngs.get(0).longitude;
                double lat = v * latLngs.get(1).latitude + (1 - v)
                        * latLngs.get(0).latitude;
                LatLng newPos = new LatLng(lat, lng);
                marker.setPosition(newPos);
                marker.setAnchor(0.5f, 0.5f);
                marker.setRotation(getBearing(latLngs.get(0), newPos));
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition
                        (new CameraPosition.Builder().target(newPos)
                                .zoom(15.5f).build()));
            }
        });
        valueAnimator.start();
    }

    /**
     * Initialize the MQTT helper to connect to the broker and subscribe to the topic
     */
    private void startMqtt() {
        MQTTHelper mqttHelper = new MQTTHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d(TAG, "URI " + serverURI);
            }

            @Override
            public void connectionLost(Throwable cause) {
                cause.printStackTrace();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String payload = new String(message.getPayload());
                LatLng currentLatLng = convertStringToLatLng(payload);
                Log.d(TAG, topic + " " + currentLatLng);
                latLngPublishRelay.accept(currentLatLng);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG, "Delivery complete");
            }
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    /**
     * Converts the LatLng string as (28.612, 77.6545) to LatLng type.
     *
     * @param latLngPair String representing latitude and longitude pair of form (28.612, 77.6545).
     * @return The LatLng type of the string.
     */
    private LatLng convertStringToLatLng(String latLngPair) {
        String[] latLng = latLngPair.split(",");
        double latitude = Double.parseDouble(latLng[0].substring(1, latLng[0].length()));
        double longitude = Double.parseDouble(latLng[1].substring(0, latLng[1].length() - 1));
        return new LatLng(latitude, longitude);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_in_right);
    }

    /**
     * Bearing between two LatLng pair
     *
     * @param begin First LatLng Pair
     * @param end Second LatLng Pair
     * @return The bearing or the angle at which the marker should rotate for going to {@code end} LAtLng.
     */
    private float getBearing(LatLng begin, LatLng end) {
        double lat = Math.abs(begin.latitude - end.latitude);
        double lng = Math.abs(begin.longitude - end.longitude);

        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        return -1;
    }
}
