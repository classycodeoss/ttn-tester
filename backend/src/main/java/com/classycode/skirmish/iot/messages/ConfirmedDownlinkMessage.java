package com.classycode.skirmish.iot.messages;

import org.thethingsnetwork.data.common.messages.DownlinkMessage;

// class fields will be automatically included in JSON through parent class
public class ConfirmedDownlinkMessage extends DownlinkMessage {
    private String schedule = "replace";
    private boolean confirmed;

    public ConfirmedDownlinkMessage(int _port, String _payload, boolean confirmed) {
        super(_port, _payload);
        this.confirmed = confirmed;
    }

    public ConfirmedDownlinkMessage(int _port, byte[] _payload, boolean confirmed) {
        super(_port, _payload);
        this.confirmed = confirmed;
    }
}
