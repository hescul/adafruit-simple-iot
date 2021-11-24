package com.hescul.adafruitdb.ui.dashboard;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class DashboardViewModel extends ViewModel {
    public final String initialUnknown  = "???";
    public final String ledStatusOn  = "ON";
    public final String ledStatusOff = "OFF";
    public final String gtwStatusActive = "online";
    public final String temperatureSuffix = "°C";
    public final String humiditySuffix    = "%";
    public final String servoSuffix       = "°";
    public final int    servoMin          = 100;
    public final int    servoMax          = 180;
    public final String errorSuffix       = ". Please refresh.";

    private MutableLiveData<String> ledStatus    = new MutableLiveData<>(initialUnknown);
    private MutableLiveData<String> servoValue   = new MutableLiveData<>(initialUnknown);
    private MutableLiveData<String> temperatureValue = new MutableLiveData<>(initialUnknown);
    private MutableLiveData<String> humidityValue = new MutableLiveData<>(initialUnknown);
    private MutableLiveData<String> gtwStatus    = new MutableLiveData<>(initialUnknown);
    private MutableLiveData<Boolean> connecting  = new MutableLiveData<>(false);
    private MutableLiveData<Boolean> connected   = new MutableLiveData<>(false);
    private MutableLiveData<String> errorMessage = new MutableLiveData<>(initialUnknown);

    public void setLedStatus(String status) { ledStatus.setValue(status); }
    public void setServoValue(String value) { servoValue.setValue(value); }
    public void setTemperatureValue(String value) {temperatureValue.setValue(value);}
    public void setHumidityValue(String value) {humidityValue.setValue(value);}
    public void setGtwStatus(String status) { gtwStatus.setValue(status); }
    public void setConnecting(boolean status) { connecting.setValue(status); }
    public void setConnected(boolean status) { connected.setValue(status); }
    public void setErrorMessage(String msg) { errorMessage.setValue(msg); }

    public MutableLiveData<String> getLedStatus() { return ledStatus; }
    public MutableLiveData<String> getServoValue() { return servoValue; }
    public MutableLiveData<String> getTemperatureValue() { return temperatureValue; }
    public MutableLiveData<String> getHumidityValue() { return humidityValue; }
    public MutableLiveData<String> getGtwStatus() { return gtwStatus; }
    public MutableLiveData<Boolean> isConnecting() { return connecting; }
    public MutableLiveData<Boolean> isConnected() { return connected; }
    public MutableLiveData<String> getErrorMessage() { return errorMessage; }
}
