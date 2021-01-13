package com.classycode.skirmish.iot.messages;

public class SentMessage {
    private String payload;
    private SentDataMessage message;
    private String gateway_id;
    private ConfigMessage config;

    public String getPayload() {
        return payload;
    }

    public SentDataMessage getMessage() {
        return message;
    }

    public String getGatewayId() {
        return gateway_id;
    }

    public ConfigMessage getConfig() {
        return config;
    }

    @Override
    public String toString() {
        return "SentMessage{" +
                "payload='" + payload + '\'' +
                ", message=" + message != null ? message.toString() : "NULL" +
                ", gateway_id='" + gateway_id + '\'' +
                ", config=" + config != null ? config.toString() : "NULL" +
                '}';
    }
}
