package com.developers.uberanimation.models.events;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Amanjeet Singh on 9/9/18.
 */
public class BeginJourneyEvent {

    private String event = "BEGIN_JOURNEY";
    private LatLng beginLatLng;

    public LatLng getBeginLatLng() {
        return beginLatLng;
    }

    public void setBeginLatLng(LatLng beginLatLng) {
        this.beginLatLng = beginLatLng;
    }

}
