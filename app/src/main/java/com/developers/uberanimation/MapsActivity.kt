package com.developers.uberanimation

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentActivity
import android.os.Bundle
import android.util.Log
import android.view.animation.LinearInterpolator
import com.developers.uberanimation.models.events.BeginJourneyEvent
import com.developers.uberanimation.models.events.CurrentJourneyEvent
import com.developers.uberanimation.models.events.EndJourneyEvent
import com.developers.uberanimation.utils.JourneyEventBus
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.SquareCap
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import com.google.android.gms.maps.model.JointType.ROUND
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_maps.destinationButton
import kotlinx.android.synthetic.main.activity_maps.destinationEditText
import kotlinx.android.synthetic.main.activity_maps.linearLayout
import kotlinx.android.synthetic.main.activity_maps.switchToDriverMode
import java.lang.UnsupportedOperationException

class MapsActivity : FragmentActivity(), OnMapReadyCallback {
  private val mapFragment: SupportMapFragment by lazy {
    supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment // Obtain the SupportMapFragment and get notified when the map is ready to be used.
  }

  private val apiInterface: ApiInterface by lazy {
    Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .baseUrl(BuildConfig.BASE_URL)
        .build()
        .create(ApiInterface::class.java)
  }

  private val polyLineList = mutableListOf<LatLng>()
  private val sydney       = LatLng(28.671246, 77.317654)
  private val disposable   = CompositeDisposable()

  // These are still mutable, use with precaution.
  private lateinit var googleMap: GoogleMap
  private lateinit var destination: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maps)

    destinationButton.setOnClickListener {
      destination = destinationEditText.text.toString().replace(" ", "+")
      Log.d(TAG, destination)
      mapFragment.getMapAsync(this@MapsActivity)
    }

    switchToDriverMode.setOnClickListener {
      DriverModeActivity.start(this)
      overridePendingTransition(R.anim.slide_in_left, R.anim.slide_in_right)
    }
  }

  override fun onResume() {
    super.onResume()
    /*
     * This is an event bus for receiving journey events this can be shifted anywhere in code.
     * Do remember to dispose when not in use. For eg. its necessary to dispose it in onStop as activity is not visible.
     */
    disposable.add(JourneyEventBus.instance.onJourneyEvent
        .subscribeOn(AndroidSchedulers.mainThread())
        .subscribe { o ->
          when (o) {
            is BeginJourneyEvent   -> Snackbar.make(linearLayout, "Journey started", Snackbar.LENGTH_SHORT).show()
            is EndJourneyEvent     -> Snackbar.make(linearLayout, "Journey ended", Snackbar.LENGTH_SHORT).show()
            is CurrentJourneyEvent -> {
              /**
               * This can be used to receive the current location update of the car
               */
              Log.d(TAG,"Current ${o.currentLatLng}")
            }
          }
        })
  }

  override fun onStop() {
    with (disposable) {
      if (!isDisposed) dispose()
    }

    super.onStop()
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
    googleMap = map
    with (googleMap) {
      mapType            = GoogleMap.MAP_TYPE_NORMAL
      isTrafficEnabled   = false
      isIndoorEnabled    = false
      isBuildingsEnabled = false

      with (uiSettings) {
        isZoomControlsEnabled = true
        setAllGesturesEnabled(true)
        isZoomGesturesEnabled = true
      }

      addMarker(MarkerOptions().position(sydney).title("Marker in Home"))
      moveCamera(CameraUpdateFactory.newLatLng(sydney))
      moveCamera(CameraUpdateFactory.newCameraPosition(
          CameraPosition.Builder()
              .target(map.cameraPosition.target)
              .zoom(17f)
              .bearing(30f)
              .tilt(45f)
              .build())
      )
    }

    getDirections(28.671246, 77.317654)
  }

  private fun getDirections(latitude: Double, longitude: Double) {
    disposable.add(apiInterface
        .getDirections("driving", "less_driving", "$latitude,$longitude", destination, BuildConfig.GOOGLE_DIRECTIONS_KEY)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            {
              val routeList = it.routes
              for (route in routeList) {
                val polyLine = route.overviewPolyline.points

                with(polyLineList) {
                  clear()
                  addAll(decodePoly(polyLine))
                }
                drawPolyLineAndAnimateCar()
              }
            },
            { it.printStackTrace() }
        ))
  }

  private fun drawPolyLineAndAnimateCar() {
    //Adjusting bounds
    val builder = LatLngBounds.Builder()
    polyLineList.forEach { builder.include(it) }
    val bounds = builder.build()
    val mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2)
    googleMap.animateCamera(mCameraUpdate)

    val polylineOptions = PolylineOptions().apply {
      color(Color.GRAY)
      width(5f)
      startCap(SquareCap())
      endCap(SquareCap())
      jointType(ROUND)
      addAll(polyLineList)
    }
    val greyPolyLine = googleMap.addPolyline(polylineOptions)

    val blackPolylineOptions = PolylineOptions().apply {
      width(5f)
      color(Color.BLACK)
      startCap(SquareCap())
      endCap(SquareCap())
      jointType(ROUND)
    }
    val blackPolyline = googleMap.addPolyline(blackPolylineOptions)

    googleMap.addMarker(
        MarkerOptions().position(polyLineList[polyLineList.size - 1])
    )

    val polylineAnimator = ValueAnimator.ofInt(0, 100).apply {
      duration     = 2000
      interpolator = LinearInterpolator()
      addUpdateListener { valueAnimator ->
        val greyPolylinePoints     = greyPolyLine.points
        val percentValue           = valueAnimator.animatedValue as Int
        val greyPolylinePointsSize = greyPolylinePoints.size
        val newPoints              = (greyPolylinePointsSize * (percentValue / 100.0f)).toInt()
        val requiredPolylinePoints = greyPolylinePoints.subList(0, newPoints)
        blackPolyline.points       = requiredPolylinePoints
      }
      start()
    }

    val marker = googleMap.addMarker(
        MarkerOptions()
            .position(sydney)
            .flat(true)
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car))
    )
    val handler = Handler()
    var index   = -1
    var next    = 1
    var startPosition = LatLng(0.toDouble(), 0.toDouble())
    var endPosition   = LatLng(0.toDouble(), 0.toDouble())

    handler.postDelayed(object : Runnable {
      override fun run() {
        if (index < polyLineList.size - 1) {
          index++
          next = index + 1
        }

        if (index < polyLineList.size - 1) {
          startPosition = polyLineList[index]
          endPosition = polyLineList[next]
        }

        if (index == 0) {
          val beginJourneyEvent = BeginJourneyEvent()
          beginJourneyEvent.beginLatLng = startPosition
          JourneyEventBus.instance.setOnJourneyBegin(beginJourneyEvent)
        }

        if (index == polyLineList.size - 1) {
          val endJourneyEvent = EndJourneyEvent()
          endJourneyEvent.endLatLng = LatLng(polyLineList[index].latitude,
              polyLineList[index].longitude)
          JourneyEventBus.instance.setOnJourneyEnd(endJourneyEvent)
        }

        val valueAnimator = ValueAnimator.ofFloat(0F, 1F).apply {
          duration = 3000
          interpolator = LinearInterpolator()
          addUpdateListener {
            val animatedFraction  = it.animatedFraction
            val lng = animatedFraction * endPosition.longitude + (1 - animatedFraction) * startPosition.longitude
            val lat = animatedFraction * endPosition.latitude + (1 - animatedFraction) * startPosition.latitude
            val updatedPosition = LatLng(lat, lng)
            val currentJourneyEvent = CurrentJourneyEvent()
            currentJourneyEvent.currentLatLng = updatedPosition
            JourneyEventBus.instance.setOnJourneyUpdate(currentJourneyEvent)
            with (marker) {
              position = updatedPosition
              setAnchor(0.5f, 0.5f)
              rotation = getBearing(startPosition, updatedPosition)
            }
            googleMap.animateCamera(
                CameraUpdateFactory
                    .newCameraPosition(
                        CameraPosition.Builder()
                            .target(updatedPosition)
                            .zoom(15.5f)
                            .build()
                    )
            )
          }
          start()
        }

        if (index != polyLineList.size - 1) {
          handler.postDelayed(this, 3000)
        }
      }
    }, 3000)
  }

  private fun decodePoly(encoded: String): List<LatLng> {
    val polyline = mutableListOf<LatLng>()
    var index    = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
      var b: Int
      var shift = 0
      var result = 0
      do {
        b = encoded[index++].toInt() - 63
        result = result or (b and 0x1f shl shift)
        shift += 5
      } while (b >= 0x20)
      val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
      lat += dlat

      shift = 0
      result = 0
      do {
        b = encoded[index++].toInt() - 63
        result = result or (b and 0x1f shl shift)
        shift += 5
      } while (b >= 0x20)
      val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
      lng += dlng

      val p = LatLng(lat.toDouble() / 1E5,
          lng.toDouble() / 1E5)
      polyline.add(p)
    }

    return polyline
  }

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

  companion object {
    private val TAG = MapsActivity::class.java.simpleName
  }
}
