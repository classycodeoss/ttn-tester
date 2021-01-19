package com.classycode.skirmish.iot;

import org.thethingsnetwork.data.mqtt.Client;

import java.net.URISyntaxException;
import java.util.logging.Logger;

public class TTNTesterMain {
    private static final Logger LOGGER = Logger.getLogger(TTNTesterMain.class.getSimpleName());

    public static void main(String[] args) throws Exception {

        final String region = System.getProperty("region");
        final String appId = System.getProperty("appId");
        final String accessKey = System.getProperty("accessKey");
        final String gatewayEUI = System.getProperty("gatewayEUI");
        final String enableConfirmation = System.getProperty("enableConfirmation");
        if (region == null || appId == null || accessKey == null || gatewayEUI == null) {
            LOGGER.severe("set java -D properties  (region, appId, accessKey, gatewayEUI)! exiting");
            System.exit(-1);
        }
        final Config config = new Config(region, appId, accessKey, gatewayEUI, enableConfirmation);
        final TTNController controller = new TTNController(config);
        LOGGER.info(() -> String.format("starting client%nconfig: %s", config));
        createClient(config.getRegion(), config.getAppId(), config.getAccessKey(), controller, config).start();
    }

    private static Client createClient(String region, String appId, String accessKey, TTNController controller, Config config) throws URISyntaxException {
        final Client client = new Client(region, appId, accessKey);
        // uplink received
        client.onMessage(controller.uplinkReceivedHandler(client));
        // device OTA
        client.onActivation(controller.devActivationHandler());
        client.onError(controller.errorHandler());
        client.onConnected(controller.connectedHandler());
        // hook for downlink queued
        client.onDevice(null, "down/scheduled", controller.downlinkScheduledHandler(client));
        // hook for confirmed and unconfirmed downlinks
        client.onDevice(null, "down/sent", controller.downlinkSentHandler(client));
        if (config.isUseConfirmation()) {
            // hook for confirmed downlinks
            client.onDevice(null, "down/acks", controller.downlinkConfirmedHandler(client));
        }
        return client;
    }
}
