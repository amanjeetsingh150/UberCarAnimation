package com.developers.uberanimation.utils

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage


/**
 * Class to connect to MQTT broker. Needed Username, password and the server URI which are
 * present on the dashboard of the server.
 *
 * {@author} Amanjeet Singh on 9/9/18.
 */
class MQTTHelper(context: Context) {
  private val subscriptionTopic = "location/+"

  // TODO(gs) 28/Oct/18 - These should be extracted into a config file/module.
  private val username  = "YOUR USERNAME HERE"// put your MQTT username here
  private val password  = "PASSWORD HERE"// put your MQTT password here
  private val serverUri = "SERVER URI HERE" //of form tcp://domain:port

  private val mqttAndroidClient: MqttAndroidClient
  private val clientId = "UberMqttClient"

  init {
    mqttAndroidClient = MqttAndroidClient(context, serverUri, clientId)
    mqttAndroidClient.setCallback(object : MqttCallbackExtended {
      override fun connectComplete(reconnect: Boolean, serverURI: String) {
        Log.d(TAG, serverURI)
      }

      override fun connectionLost(cause: Throwable) {
        cause.printStackTrace()
      }

      @Throws(Exception::class)
      override fun messageArrived(topic: String, message: MqttMessage) {
        Log.d(TAG, message.toString())
      }

      override fun deliveryComplete(token: IMqttDeliveryToken) {
        Log.d(TAG, "Delivery complete")
      }
    })
    connect()
  }

  /**
   * Setter for MQTT callbacks.
   * @param callback MQTTCallBack to receive messages and delivery complete statuses.
   */
  fun setCallback(callback: MqttCallbackExtended) {
    mqttAndroidClient.setCallback(callback)
  }

  /**
   * Connect with MQTT broker by applying the username and password
   */
  private fun connect() {
    val mqttConnectOptions = MqttConnectOptions().apply {
      isAutomaticReconnect = true
      isCleanSession = false
      userName = username
      password = this@MQTTHelper.password.toCharArray()
    }

    try {
      mqttAndroidClient.connect(mqttConnectOptions, null, object : IMqttActionListener {
        override fun onSuccess(asyncActionToken: IMqttToken) {
          val disconnectedBufferOptions = DisconnectedBufferOptions().apply {
            isBufferEnabled = true
            bufferSize      = 100
            isPersistBuffer = false
            isDeleteOldestMessages = false
          }
          mqttAndroidClient.setBufferOpts(disconnectedBufferOptions)
          subscribeToTopic()
        }

        override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
          Log.d(TAG, "Failed to connect to: " + serverUri + exception.toString())
        }
      })
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  /**
   * Used for subscribing to the topic.
   */
  private fun subscribeToTopic() {
    try {
      mqttAndroidClient.subscribe(subscriptionTopic, 0, null, object : IMqttActionListener {
        override fun onSuccess(asyncActionToken: IMqttToken) {
          Log.w(TAG, "Subscribed!")
        }

        override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
          Log.w(TAG, "Subscribed fail!")
        }
      })
    } catch (ex: MqttException) {
      Log.e(TAG, "Exception whilst subscribing")
      ex.printStackTrace()
    }
  }

  companion object {
    private val TAG = MQTTHelper::class.java.simpleName
  }
}
