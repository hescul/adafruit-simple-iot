package com.hescul.adafruitdb;


import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.hescul.adafruitdb.databinding.ActivityMainBinding;
import com.hescul.adafruitdb.databinding.FragmentDashboardBinding;
import com.hescul.adafruitdb.databinding.FragmentLedBinding;
import com.hescul.adafruitdb.databinding.FragmentServoBinding;
import com.hescul.adafruitdb.databinding.FragmentTemperatureBinding;
import com.hescul.adafruitdb.ui.dashboard.DashboardFragment;
import com.hescul.adafruitdb.ui.dashboard.DashboardViewModel;
import com.hescul.adafruitdb.ui.dashboard.LedFragment;
import com.hescul.adafruitdb.ui.dashboard.ServoFragment;
import com.hescul.adafruitdb.ui.dashboard.TemperatureFragment;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity
        implements DashboardFragment.DashboardFragmentListener, LedFragment.LedFragmentListener,
        ServoFragment.ServoFragmentListener, TemperatureFragment.TemperatureFragmentListener
{
    // private fields
    // -----
    private static final String logTag = "mqtt";

    private MqttAndroidClient _client;
    private MqttConnectOptions _connectOptions;
    private DisconnectedBufferOptions _disconnectedBufferOptions;

    private DashboardViewModel _dashboardViewModel;


    // lifecycle methods
    // -----
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // inflate root view for this activity using view bing library
        setContentView(ActivityMainBinding.inflate(getLayoutInflater()).getRoot());

        // setup view model for fragments
        _dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        // initialize and connect a client
        initClient();
        connectClient();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectClient();
    }

    // mqtt utilities
    // -----
    private void initClient() {
        final String serverURI = BrokerConfig.AUTH_CONTEXT + "://" + BrokerConfig.HOST_NAME + ':' + BrokerConfig.HOST_PORT;
        _client = new MqttAndroidClient(getApplicationContext(), serverURI, ClientConfig.CLIENT_ID);
        setupClient();
        setupCallback();
    }

    private void setupCallback() {
        _client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Timber.tag(logTag).d("Connected successfully: %s", serverURI);
            }

            @Override
            public void connectionLost(Throwable cause) {
                if (cause != null) {
                    Timber.tag(logTag).e("Lost connection to broker: %s", cause.getMessage());
                    _dashboardViewModel.setErrorMessage(cause.getMessage() + _dashboardViewModel.errorSuffix);
                    _dashboardViewModel.setConnected(false);
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                Timber.tag(logTag).d("Received '%s' from %s", message, topic.substring(topic.indexOf('.') + 1));
                if (topic.contains(ClientConfig.SUBSCRIBE_TOPICS[ClientConfig.TOPICS.LED.ordinal()])) {             // led message
                    _dashboardViewModel.setLedStatus(message.toString());
                }
                else if (topic.contains(ClientConfig.SUBSCRIBE_TOPICS[ClientConfig.TOPICS.SERVO.ordinal()])) {      // servo message
                    _dashboardViewModel.setServoValue(message.toString());
                }
                else if (topic.contains(ClientConfig.SUBSCRIBE_TOPICS[ClientConfig.TOPICS.TEMPERATURE.ordinal()])) {// temperature message
                    _dashboardViewModel.setTemperatureValue(message.toString());
                }
                else if (topic.contains(ClientConfig.SUBSCRIBE_TOPICS[ClientConfig.TOPICS.HUMIDITY.ordinal()])) {   // humidity message
                    _dashboardViewModel.setHumidityValue(message.toString());
                }
                else if (topic.contains(ClientConfig.SUBSCRIBE_TOPICS[ClientConfig.TOPICS.STATUS.ordinal()])) {     // status message
                    _dashboardViewModel.setGtwStatus(message.toString());
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                try {
                    if (!token.getMessage().toString().isEmpty()) {
                        Timber.tag("mqtt").d("Delivered successfully (messageID = %d)", token.getMessageId());
                    }
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private void setupClient() {
        // setup connection options
        _connectOptions = new MqttConnectOptions();
        _connectOptions.setUserName(BrokerConfig.USERNAME);
        _connectOptions.setPassword(BrokerConfig.PASSWORD.toCharArray());
        _connectOptions.setAutomaticReconnect(false);
        _connectOptions.setCleanSession(true);

        // setup disconnected buffer options
        _disconnectedBufferOptions = new DisconnectedBufferOptions();
        _disconnectedBufferOptions.setBufferEnabled(true);
        _disconnectedBufferOptions.setBufferSize(100);
        _disconnectedBufferOptions.setPersistBuffer(false);
        _disconnectedBufferOptions.setDeleteOldestMessages(false);
    }

    private void connectClient() {
        _dashboardViewModel.setConnecting(true);
        try {
            _client.connect(_connectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    _client.setBufferOpts(_disconnectedBufferOptions);
                    subscribe();
                    fetchLatest();
                    _dashboardViewModel.setConnected(true);
                    _dashboardViewModel.setConnecting(false);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Timber.tag(logTag).e("%s failed to reach %s: %s", ClientConfig.CLIENT_ID, BrokerConfig.HOST_NAME, exception.getMessage());
                    _dashboardViewModel.setErrorMessage(exception.getMessage() + _dashboardViewModel.errorSuffix);
                    _dashboardViewModel.setConnected(false);
                    _dashboardViewModel.setConnecting(false);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void disconnectClient() {
        if (_client.isConnected()) {
            try {
                IMqttToken token = _client.disconnect();
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Timber.tag(logTag).d("%s disconnected successfully", ClientConfig.CLIENT_ID);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Timber.tag(logTag).e("%s failed to disconnect: %s", ClientConfig.CLIENT_ID, exception.getMessage());
                    }
                });

            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    private void subscribe() {

        for (String topic : ClientConfig.SUBSCRIBE_TOPICS) {
            try {
                String realTopic = String.format("%s/feeds/%s.%s", BrokerConfig.USERNAME, ClientConfig.GROUP_KEY, topic);
                _client.subscribe(realTopic, 0, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {

                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Timber.tag(logTag).e("Failed to subscribe to %s", topic);
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    private void fetchLatest() {
        for (String topic : ClientConfig.SUBSCRIBE_TOPICS) {
            publish(String.format("%s/get", topic), "");
        }
    }
    private void fetchLatest(String topic) {
        publish(String.format("%s/get", topic), "");
    }

    private int publish(String topic, String msg) {
        String realTopic = String.format("%s/feeds/%s.%s", BrokerConfig.USERNAME, ClientConfig.GROUP_KEY, topic);
        int msgID = 0;
        try {
            IMqttToken token = _client.publish(realTopic, msg.getBytes(StandardCharsets.UTF_8), 0, false);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Timber.tag("mqtt").e("Failed to publish %s to %s",msg ,topic);
                }
            });
            msgID = token.getMessageId();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        return msgID;
    }


    // implement fragment interfaces
    // -----
    @Override
    public void onDashboardCreate(FragmentDashboardBinding binding, int bindingVariable) {
        assert binding != null;
        binding.setVariable(bindingVariable, _dashboardViewModel);
    }

    @Override
    public void onDashboardRefresh() {
        if (!_client.isConnected()) {
            connectClient();
        }
    }

    @Override
    public void onLedCreate(FragmentLedBinding binding, int bindingVariable) {
        assert binding != null;
        binding.setVariable(bindingVariable, _dashboardViewModel);
    }

    @Override
    public void onLedModify(String msg) {
        publish(ClientConfig.SUBSCRIBE_TOPICS[0], msg);
    }

    @Override
    public void onServoCreate(FragmentServoBinding binding, int bindingVariable) {
        assert binding != null;
        binding.setVariable(bindingVariable, _dashboardViewModel);
    }

    @Override
    public void onServoModify(int value) {
        if (_client.isConnected()) {
            publish(ClientConfig.SUBSCRIBE_TOPICS[ClientConfig.TOPICS.SERVO.ordinal()], String.valueOf(value));
        }
    }

    @Override
    public void onTemperatureCreate(FragmentTemperatureBinding binding, int bindingVariable) {
        assert binding != null;
        binding.setVariable(bindingVariable, _dashboardViewModel);
    }
}