# UberCarAnimation
A demo application which demonstrates movement of car on map developed after inspiration from Uber.
<br><br>
# Demo
<img src="https://user-images.githubusercontent.com/12881364/29244386-7f164cd6-7fd4-11e7-9d1a-13af7ee237ba.gif"/>
<br>
Youtube Link: https://www.youtube.com/watch?v=JIs4kLZ8qQI
<br><br>
APIs and Libraries used
<UL>
<LI>Google Maps Api</LI>
<LI>Google Maps Directions API</LI>
<LI>Volley</LI>
</UL>

<br><br>
# Explained Logic
Steps:
<UL>
<LI>Parse the "overview_polyline" from the JSON by providing the appropriate GET parameters. For eg:
<pre>
<code>
"https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&"
                    + "transit_routing_preference=less_driving&"
                    + "origin=" + latitude + "," + longitude + "&"
                    + "destination=" + destination + "&"
                    + "key=" + getResources().getString(R.string.google_directions_key)
</code>
</pre>
</LI>
<LI>Decode the polyline which will provide you with list of latitudes and longitudes that is List&ltLatLng&gt to be apt.</LI>
<LI>Setting up of Value animator:Create a value animator by providing the ofFloatValue, setting duration and adding update listener in Handler
<pre>
<code>
ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
valueAnimator.setDuration(3000);
valueAnimator.setInterpolator(new LinearInterpolator());
valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
  @Override
  public void onAnimationUpdate(ValueAnimator valueAnimator) {
    //CODE
    
});
</code>
</pre>
</LI>
<LI>In the value animator Update listener get the Animation fraction and evaluate the latitudes and longitudes as shown:
<pre>
<code>
v=valueAnimator.getAnimatedFraction();
lng = v * endPosition.longitude + (1 - v)* startPosition.longitude;
lat = v * endPosition.latitude + (1 - v)* startPosition.latitude;
</code>
</pre>
where v is animation fraction
and startposition and endPostion refer to each pair of LatLng from the decoded list from polyline for eg (0,1) then (1,2) then(2,3)
and so on.<br>
According to linear interpolation:
The parameter 'v' defines where to estimate the value on the interpolated line, it is 0 at the first point and 1 and the second point. 
For interpolated values between the two points v ranges between 0 and 1.
We evaluate values one by one between each pair of LatLng by traversing through the list.
</LI>
<LI>
Finally  set position of marker to the new position, also evaluating the bearing between the consecutive points so that it seems car is turning literally
and finally update camera as:
<pre>
<code>
marker.setPosition(newPos);
marker.setAnchor(0.5f, 0.5f);
marker.setRotation(getBearing(startPosition, newPos));
mMap.moveCamera(CameraUpdateFactory
                .newCameraPosition
                (new CameraPosition.Builder()
                target(newPos)
                .zoom(15.5f)
                .build()));
</code>
</pre>
</LI>
</UL>


<br><br>
# Running the project
The application uses <b>Google Maps Api Key</b> and <b>Google Map Directions key</b>. Get these api key on google developers console after enabling them for your project. Replace your google maps directions api key in <a href="https://github.com/amanjeetsingh150/UberCarAnimation/blob/master/app/src/main/res/values/strings.xml">strings.xml</a> and google maps key in <a href="https://github.com/amanjeetsingh150/UberCarAnimation/blob/master/app/src/debug/res/values/google_maps_api.xml">google_maps_api.xml</a>. For convenience a TODO has been added there just follow them.
<br><br>

# Developers
<UL>
<LI><a href="https://github.com/amanjeetsingh150">Amanjeet Singh</a>
</UL>
<blockquote>
If you found this code demo helpful or you learned something today and want to thank me, consider buying me a cup of :coffee: at
<a href="https://www.paypal.me/amanjeetsingh150">PayPal</a>
</blockquote>
