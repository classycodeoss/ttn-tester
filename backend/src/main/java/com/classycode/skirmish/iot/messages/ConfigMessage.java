package com.classycode.skirmish.iot.messages;

public class ConfigMessage {
    private String modulation;
    private String data_rate;
    private String airtime;
    private int counter;
    private int frequency;
    private int power;

    public String getModulation() {
        return modulation;
    }

    public String getDataRate() {
        return data_rate;
    }

    public String getAirtime() {
        return airtime;
    }

    public int getCounter() {
        return counter;
    }

    public int getFrequency() {
        return frequency;
    }

    public int getPower() {
        return power;
    }

    @Override
    public String toString() {
        return "ConfigMessage{" +
                "modulation='" + modulation + '\'' +
                ", data_rate='" + data_rate + '\'' +
                ", airtime='" + airtime + '\'' +
                ", counter='" + counter + '\'' +
                ", frequency=" + frequency +
                ", power=" + power +
                '}';
    }
}
