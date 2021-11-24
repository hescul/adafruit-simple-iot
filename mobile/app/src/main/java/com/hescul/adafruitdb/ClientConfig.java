package com.hescul.adafruitdb;

public class ClientConfig {
    static final String CLIENT_ID  = "hescul";
    static final String GROUP_KEY  = "iot-lab";
    static final String STAT_TOPIC = "gateway-status";
    static final String[] SUBSCRIBE_TOPICS = {
            "arduino-led",
            "arduino-servo",
            "temperature",
            "humidity",
            STAT_TOPIC,
    };
    enum TOPICS {
        LED,
        SERVO,
        TEMPERATURE,
        HUMIDITY,
        STATUS
    }
}
