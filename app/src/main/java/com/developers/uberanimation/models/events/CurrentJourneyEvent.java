package com.developers.uberanimation.models.events;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Amanjeet Singh on 9/9/18.
 */
public class CurrentJourneyEvent {

    private String event = "BEGIN_JOURNEY";
    private LatLng currentLatLng;

    public LatLng getCurrentLatLng() {
        return currentLatLng;
    }

    public void setCurrentLatLng(LatLng currentLatLng) {
        this.currentLatLng = currentLatLng;
    }
}
