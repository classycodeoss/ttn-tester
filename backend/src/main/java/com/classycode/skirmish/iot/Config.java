package com.classycode.skirmish.iot;

public class Config {
    private final String region;
    private final String appId;
    private final String accessKey;
    private final String gatewayEUI;
    /**
     * TTN downlink is limited, disable it for testing
     */
    private final boolean downlinkEnabled = true;

    /**
     * controller uses confirmed downlinks for communication with node, if enabled a it isn't enough to receive
     * a TTN sent event but a node downlink ACK event is necessary to initiate a state transition
     */
    private final boolean useConfirmation;

    public Config(String region, String appId, String accessKey, String gatewayEUI, String useConfirmation) {
        this.region = region;
        this.appId = appId;
        this.accessKey = accessKey;
        this.gatewayEUI = gatewayEUI;
        this.useConfirmation = Boolean.parseBoolean(useConfirmation);
    }

    @Override
    public String toString() {
        return "Config{" +
                "region='" + region + '\'' +
                ", appId='" + appId + '\'' +
                ", accessKey='" + accessKey + '\'' +
                ", gatewayEUI='" + gatewayEUI + '\'' +
                ", downlinkEnabled=" + downlinkEnabled +
                ", useConfirmation=" + useConfirmation +
                '}';
    }

    public String getRegion() {
        return region;
    }

    public String getAppId() {
        return appId;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getGatewayEUI() {
        return gatewayEUI;
    }

    public boolean isDownlinkEnabled() {
        return downlinkEnabled;
    }

    public boolean isUseConfirmation() {
        return useConfirmation;
    }
}
