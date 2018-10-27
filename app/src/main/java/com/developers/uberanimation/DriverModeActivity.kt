package com.developers.uberanimation

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.view.animation.LinearInterpolator
import com.developers.uberanimation.utils.MQTTHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage

class DriverModeActivity : FragmentActivity(), OnMapReadyCallback {
  companion object {
    fun start(context: Context): Intent {
      val intent = Intent(context, DriverModeActivity::class.java)
      context.startActivity(intent)

      return intent
    }

    private val TAG = DriverModeActivity::class.java.simpleName
  }

  private val latLngPublishRelay = PublishRelay.create<LatLng>()
  private val disposable         = CompositeDisposable()

  // Variables, use with caution.
  private lateinit var googleMap: GoogleMap
  private lateinit var marker: Marker
  private var emission = 0

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_driver_mode)

    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).getMapAsync(this)
    startMqtt()
  }

  override fun onResume() {
    super.onResume()
    disposable.add(latLngPublishRelay
        .buffer(2)
        .subscribeOn(AndroidSchedulers.mainThread())
        .subscribe { latLngs ->
          emission++
          animateCarOnMap(latLngs)
        }
    )
  }

  override fun onStop() {
    with (disposable) {
      if (!disposable.isDisposed) dispose()
    }

    super.onStop()
  }

  /**
   * Take the emissions from the Rx Relay as a pair of LatLng and starts the animation of
   * car on map by taking the 2 pair of LatLng's.
   *
   * @param latLngs List of LatLng emitted by Rx Relay with size two.
   */
  private fun animateCarOnMap(latLngs: List<LatLng>) {
    val builder = LatLngBounds.Builder()
    for (latLng in latLngs) {
      builder.include(latLng)
    }
    val bounds = builder.build()
    val mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2)
    googleMap.animateCamera(mCameraUpdate)
    if (emission == 1) {
      marker = googleMap.addMarker(MarkerOptions().position(latLngs[0])
          .flat(true)
          .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car)))
    }
    if (::marker.isInitialized) {
      with (marker) {
        position = latLngs[0]
        val valueAnimator = ValueAnimator.ofFloat(0F, 1F).apply {
          duration = 1000
          interpolator = LinearInterpolator()
          addUpdateListener {
            val animatedFraction = it.animatedFraction
            val lng = animatedFraction * latLngs[1].longitude + (1 - animatedFraction) * latLngs[0].longitude
            val lat = animatedFraction * latLngs[1].latitude + (1 - animatedFraction) * latLngs[0].latitude
            val newPos = LatLng(lat, lng)
            position = newPos
            setAnchor(0.5f, 0.5f)
            rotation = getBearing(latLngs[0], newPos)
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(newPos)
                .zoom(15.5f).build()))
          }
          start()
        }
      }
    }
  }

  /**
   * Initialize the MQTT helper to connect to the broker and subscribe to the topic
   */
  private fun startMqtt() {
    val mqttHelper = MQTTHelper(applicationContext)
    mqttHelper.setCallback(object : MqttCallbackExtended {
      override fun connectComplete(reconnect: Boolean, serverURI: String) {
        Log.d(TAG, "URI $serverURI")
      }

      override fun connectionLost(cause: Throwable) {
        cause.printStackTrace()
      }

      @Throws(Exception::class)
      override fun messageArrived(topic: String, message: MqttMessage) {
        val payload = String(message.payload)
        val currentLatLng = convertStringToLatLng(payload)
        Log.d(TAG, "$topic $currentLatLng")
        latLngPublishRelay.accept(currentLatLng)
      }

      override fun deliveryComplete(token: IMqttDeliveryToken) {
        Log.d(TAG, "Delivery complete")
      }
    })
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
  override fun onMapReady(map: GoogleMap) {
    this.googleMap = map

    // Add a marker in Sydney and move the camera
    val sydney = LatLng(-34.0, 151.0)
    googleMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
    googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
  }

  /**
   * Converts the LatLng string as (28.612, 77.6545) to LatLng type.
   *
   * @param latLngPair String representing latitude and longitude pair of form (28.612, 77.6545).
   * @return The LatLng type of the string.
   */
  private fun convertStringToLatLng(latLngPair: String): LatLng {
    val latLng    = latLngPair.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val latitude  = latLng[0].substring(1, latLng[0].length).toDouble()
    val longitude = latLng[1].substring(0, latLng[1].length - 1).toDouble()

    return LatLng(latitude, longitude)
  }

  override fun onBackPressed() {
    super.onBackPressed()
    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_in_right)
  }

  /**
   * Bearing between two LatLng pair
   *
   * @param begin First LatLng Pair
   * @param end   Second LatLng Pair
   * @return The bearing or the angle at which the marker should rotate for going to `end` LAtLng.
   */
  private fun getBearing(begin: LatLng, end: LatLng): Float {
    val lat = Math.abs(begin.latitude - end.latitude)
    val lng = Math.abs(begin.longitude - end.longitude)

    return when {
      begin.latitude < end.latitude && begin.longitude < end.longitude   -> Math.toDegrees(Math.atan(lng / lat)).toFloat()
      begin.latitude >= end.latitude && begin.longitude < end.longitude  -> (90 - Math.toDegrees(Math.atan(lng / lat)) + 90).toFloat()
      begin.latitude >= end.latitude && begin.longitude >= end.longitude -> (Math.toDegrees(Math.atan(lng / lat)) + 180).toFloat()
      begin.latitude < end.latitude && begin.longitude >= end.longitude  -> (90 - Math.toDegrees(Math.atan(lng / lat)) + 270).toFloat()

      else -> throw UnsupportedOperationException("This should never be reached.")
    }
  }
}
