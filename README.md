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

# Driver mode
<img src="https://user-images.githubusercontent.com/12881364/45456295-d4f0b900-b707-11e8-8067-e1adb9c98716.gif" />
The driver mode is the real world example of the situation where the driver app is communicating with user app and the car is animating accordingly.
<br>
Youtube Link: https://www.youtube.com/watch?v=-gTGJF7mZQI
<br>
Here the <b>python script</b> is acting like a driver for the user app.
<br><br>


# Explained Logic

<UL>
<LI>Establish a MQTT broker by logging into one of the MQTT providers. I used <a href="https://customer.cloudmqtt.com/login">CloudMQTT</a>.</LI>
<LI>Create the instance for MQTT and generate credentials.</LI>
<LI>Integrate the <b>MQTT Paho Client</b> for android by including following in your app module <code>build.gradle</code>:
<pre>
implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.1.0'
implementation 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'
</pre>
</LI>
<LI>Fill your credentials in <code>MQTTHelper</code> class. The username, password and the server URI which is of form tcp://uri:port.</LI>
<LI>Similarly add them in <code>UberMQTT.py</code> file.</LI>
<LI>The Python script will be acting as driver and publishing the location on MQTT server in <b>flex interval</b> of 1 seconds using topic of <code>location/track</code>.
The android app will connect to the broker and subscribe to the topic of kind <code>location/+</code>. As soon as the MQTT server receives the location it will push it to the android client.</LI>
<LI>We will receive location in form of String which will be converted to LatLng type by function <code>convertStringToLatLng</code>.</LI>
<LI>Then RxRelay is used to create stream of the LatLng's. Now as we need pair of LatLng for dispatching animation we will be taking <b>buffer</b> operator with count 2. This is shown below:
In <code>messageReceived</code> callback:
<pre>
@Override
public void messageArrived(String topic, MqttMessage message) throws Exception {
 String payload = new String(message.getPayload());
 LatLng currentLatLng = convertStringToLatLng(payload);
 Log.d(TAG, topic + " " + currentLatLng);
 latLngPublishRelay.accept(currentLatLng);
}
</pre>
And subscribing to this relay with buffer 2:
<pre>
latLngPublishRelay
   .buffer(2)
   .observeOn(AndroidSchedulers.mainThread())
   .subscribe(new Consumer<List<LatLng>>() {

     @Override
     public void accept(List<LatLng> latLngs) throws Exception {
      emission++;
      animateCarOnMap(latLngs);
     }

   });
</pre>
</LI>
<LI>As soon as the Relay will emit two LatLng the <code>animateCarOnMap</code> function will dispatch animation through ValueAnimator. This animation will be based on same logic as was explained above.</LI>
</UL>

 

# Developers
<UL>
<LI><a href="https://github.com/amanjeetsingh150">Amanjeet Singh</a>
</UL>
<blockquote>
If you found this code demo helpful or you learned something today and want to thank me, consider buying me a cup of :coffee: at
<a href="https://www.paypal.me/amanjeetsingh150">PayPal</a>
</blockquote>
