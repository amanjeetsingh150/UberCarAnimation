package com.developers.uberanimation.utils;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


/**{@code Class to connect to MQTT broker. Needed Username, password and the server URI which are
 * present on the dashboard of the server.}
 * You c
 * {@author} Amanjeet Singh on 9/9/18.
 */
public class MQTTHelper {

    private static final String TAG = MQTTHelper.class.getSimpleName();
    final String subscriptionTopic = "location/+";
    private final String username = "YOUR USERNAME HERE";// put your MQTT username here
    private final String password = "PASSWORD HERE";// put your MQTT password here
    private final String serverUri = "SERVER URI HERE"; //of form tcp://domain:port
    private MqttAndroidClient mqttAndroidClient;
    private String clientId = "UberMqttClient";


    /**
     * Constructor use to init MQTT Helper class
     *
     * @param context The context of client from which its initialized.
     */
    public MQTTHelper(Context context) {
        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d(TAG, serverURI);
            }

            @Override
            public void connectionLost(Throwable cause) {
                cause.printStackTrace();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(TAG, message.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d(TAG,"Delivery complete");
            }
        });
        connect();
    }

    /**
     * Setter for MQTT callbacks.
     * @param callback MQTTCallBack to receive messages and delivery complete statuses.
     */
    public void setCallback(MqttCallbackExtended callback) {
        mqttAndroidClient.setCallback(callback);
    }

    /**
     * Connect with MQTT broker by applying the username and password
     */
    private void connect() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(password.toCharArray());
        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Failed to connect to: " + serverUri + exception.toString());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Used for subscribing to the topic.
     */
    private void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w(TAG, "Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w(TAG, "Subscribed fail!");
                }
            });

        } catch (MqttException ex) {
            Log.e(TAG, "Exception whilst subscribing");
            ex.printStackTrace();
        }
    }
}
