package com.classycode.skirmish.iot.messages;


import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

public class SentDataMessage {
    private String app_id;
    private String dev_id;
    private int port;
    private boolean confirmed;
    private String payload_raw;

    public String getApp_id() {
        return app_id;
    }

    public String getDev_id() {
        return dev_id;
    }

    public int getPort() {
        return port;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getPayloadRaw() {
        return payload_raw;
    }

    public String getPayload() {
        return payload_raw != null ? new String(Base64.decode(payload_raw)) : "NULL";
    }

    @Override
    public String toString() {
        return "SentDataMessage{" +
                "app_id='" + app_id + '\'' +
                ", dev_id='" + dev_id + '\'' +
                ", port=" + port +
                ", confirmed=" + confirmed +
                ", payload_raw='" + payload_raw + '\'' +
                ", payload='" + getPayload() + '\'' +
                '}';
    }
}
