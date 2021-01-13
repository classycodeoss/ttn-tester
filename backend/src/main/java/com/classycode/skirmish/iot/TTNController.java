package com.classycode.skirmish.iot;

import com.classycode.skirmish.iot.messages.ConfirmedDownlinkMessage;
import com.classycode.skirmish.iot.messages.SentMessage;
import org.thethingsnetwork.data.common.Connection;
import org.thethingsnetwork.data.common.TriConsumer;
import org.thethingsnetwork.data.common.messages.ActivationMessage;
import org.thethingsnetwork.data.common.messages.DataMessage;
import org.thethingsnetwork.data.common.messages.RawMessage;
import org.thethingsnetwork.data.common.messages.UplinkMessage;
import org.thethingsnetwork.data.mqtt.Client;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TTNController {
    private static final Logger LOGGER = Logger.getLogger(TTNController.class.getSimpleName());
    public static final String PONG = "PONG";
    public static final String SUCCESS = "SUCC";
    public static final String TEST_SUCCESSFUL = "\t\t>>>test successful<<<";
    public static final String TEST_FAILED = "\t\t>>>test failed<<<";

    private final Config config;
    private EchoState internalState = EchoState.CONNECTING;

    public TTNController(Config config) {
        this.config = config;
        Thread t = new Thread();
        t.start();
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(() -> {
            LOGGER.info(String.format(">>> Reset Timer elapsed <<< rest state from %s", internalState));
            internalState = EchoState.READY_FOR_PING;
        }, 150, 150, TimeUnit.MINUTES); // 10 downlink messages per 24h plus grace
    }

    public TriConsumer<String, String, RawMessage> downlinkConfirmedHandler(Client client) {
        return (String devId, String event, RawMessage data) -> {
            // note: only confirmed, payload_raw, port are set by TTN in this case
            //'{"message":{"port":1,"confirmed":true,"payload_raw":"UE9ORyAyMDIwMTIxOCAxNDo0MzoyMg=="}}'
            LOGGER.info(() -> String.format("received downlink ACK from node, state: %s, devId: %s, event: %s, data: '%s'", internalState, devId, event, data.asString()));
            try {
                final SentMessage msg = data.as(SentMessage.class);
                if (!config.isUseConfirmation()) {
                    LOGGER.info(() -> "confirmation disabled, ignoring ACK handler");
                    return;
                }
                LOGGER.info(() -> String.format("msg: %s", msg.toString()));
                switch (internalState) {
                    case PONG_SENT:
                        if (msg.getMessage().getPayload().startsWith(PONG) && config.isDownlinkEnabled()) {
                            internalState = EchoState.PONG_ACK;
                            sendDownlink(client, devId, SUCCESS);
                        } else {
                            LOGGER.warning("unknown payload: " + msg.getPayload());
                            internalState = EchoState.ERROR;
                        }
                        break;
                    case SUCCESS_SENT:
                        if (msg.getMessage().getPayload().startsWith(SUCCESS) && config.isDownlinkEnabled()) {
                            internalState = EchoState.SUCCESS_ACK;
                            LOGGER.info(TEST_SUCCESSFUL);
                        } else {
                            LOGGER.warning("unknown payload: " + msg.getPayload());
                            internalState = EchoState.ERROR;
                        }
                        break;
                    default:
                        LOGGER.fine(() -> String.format("down/acks, ignoring uplink, state: %s", internalState));
                        break;
                }
                LOGGER.info(() -> String.format("-> leaving in state: %s", internalState));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "parsing ack message failed ", e);
                internalState = EchoState.ERROR;
                LOGGER.log(Level.SEVERE, TEST_FAILED);
            }
        };
    }

    public TriConsumer<String, String, RawMessage> downlinkSentHandler(Client client) {
        return (String devId, String event, RawMessage data) -> {
            LOGGER.info(() -> String.format("state: %s, devId: %s, event: %s, data: '%s'", internalState, devId, event, data.asString()));
            final SentMessage msg;
            try {
                msg = data.as(SentMessage.class);
                if (config.getGatewayEUI().equals(msg.getGatewayId())) {
                    LOGGER.info(() -> String.format("downlink sent over known gateway, msg: %s", msg));
                } else {
                    LOGGER.warning(() -> String.format("downlink sent over unknown gateway, gw: %s%n\tmsg: %s", msg.getGatewayId(), msg));
                    internalState = EchoState.ERROR;
                    LOGGER.severe(TEST_FAILED);
                    return;
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "parsing sent message failed", e);
                internalState = EchoState.ERROR;
                LOGGER.severe(TEST_FAILED);
            }
            switch (internalState) {
                case PONG_DOWNLINK_SCHEDULED:
                    internalState = EchoState.PONG_SENT;
                    if (!config.isUseConfirmation()) {
                        sendDownlink(client, devId, SUCCESS);
                    }
                    break;
                case SUCCESS_SCHEDULED:
                    internalState = EchoState.SUCCESS_SENT;
                    LOGGER.info(TEST_SUCCESSFUL);
                    break;
                default:
                    LOGGER.fine(() -> String.format("down/sent, ignoring sent, state: %s", internalState));
                    break;
            }
            LOGGER.info(() -> String.format("-> leaving in state: %s", internalState));
        };
    }

    public TriConsumer<String, String, RawMessage> downlinkScheduledHandler(Client client) {
        return (String devId, String event, RawMessage data) -> {
            LOGGER.info(() -> String.format("state: %s, devId: %s, event: %s, data: '%s'", internalState, devId, event, data.asString()));
            switch (internalState) {
                case READY_FOR_PING:
                    internalState = EchoState.PONG_DOWNLINK_SCHEDULED;
                    break;
                case PONG_SENT:
                case PONG_ACK:
                    internalState = EchoState.SUCCESS_SCHEDULED;
                    break;
                default:
                    LOGGER.info(() -> String.format("ignoring state: %s", internalState));
                    break;
            }
            LOGGER.info(() -> String.format("-> leaving in state: %s", internalState));
        };
    }

    public Consumer<Connection> connectedHandler() {
        return (Connection client) -> {
            LOGGER.info("connected");
            internalState = EchoState.READY_FOR_PING;
            LOGGER.info(() -> String.format("-> leaving in state: %s", internalState));
        };
    }

    public BiConsumer<String, DataMessage> uplinkReceivedHandler(Client client) {
        return (String devId, DataMessage data) -> {
            UplinkMessage upMsg = (UplinkMessage) data;
            String payloadRaw = "NULL";
            try {
                payloadRaw = new String(upMsg.getPayloadRaw());
            } catch (Exception e) {
                // don't care, some uplink messages like downlink ack contain no payload
            }
            LOGGER.info(String.format("state: %s, devId: %s, data: '%s'", internalState, devId, payloadRaw));
            upMsg.getMetadata().getGateways().forEach(gw -> LOGGER.info(String.format("\t\treceived by: %s, rssi: %s, channel: %s", gw.getId(), gw.getRssi(), gw.getChannel())));
            final boolean gwIsKnown = upMsg.getMetadata().getGateways().stream().anyMatch(gw ->
                    gw.getId().equals(config.getGatewayEUI()));
            if (gwIsKnown) {
                if (internalState == EchoState.READY_FOR_PING) {
                    LOGGER.info("=>\t\tuplink message received by known gatewayEUI, replying with confirmed PONG downlink to node");
                    if (config.isDownlinkEnabled()) {
                        sendDownlink(client, devId, PONG);
                    }
                } else {// ignore
                    LOGGER.info(() -> String.format("ignoring uplink, state: %s", internalState));
                }
            }
            LOGGER.info(() -> String.format("-> leaving in state: %s", internalState));
        };
    }

    public BiConsumer<String, ActivationMessage> devActivationHandler() {
        return (String devId, ActivationMessage data) -> LOGGER.info(() -> String.format("node came online, devId: %s, devEUI: %s, devAddr: %s", devId, data.getDevEui(), data.getDevAddr()));
    }

    public Consumer<Throwable> errorHandler() {
        return (Throwable error) -> {
            internalState = EchoState.ERROR;
            LOGGER.log(Level.SEVERE, ".errorHandler()", error);
        };
    }

    private void sendDownlink(Client client, String devId, String msg) {
        LOGGER.info(() -> String.format("sending downlink message to devId: %s, msg: %s", devId, msg));
        try {
            client.send(devId, new ConfirmedDownlinkMessage(1,
                    String.format("%s %s", msg, getTimeStamp()).getBytes(), config.isUseConfirmation()));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "sending message failed", e);
            internalState = EchoState.ERROR;
        }
    }

    private String getTimeStamp() {
        return new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(new Date());
    }

    enum EchoState {
        CONNECTING, READY_FOR_PING, PONG_DOWNLINK_SCHEDULED, PONG_SENT, PONG_ACK, SUCCESS_SCHEDULED, SUCCESS_SENT, SUCCESS_ACK, ERROR, LIMIT_EXHAUSTED
    }
}
