package com.herod.leonides.udp;

/**
 * Created by santiago.barandiaran on 2/12/2016.
 */
public class UdpClientSendException extends Exception {
    private String dataToSend;

    public UdpClientSendException(Throwable cause, String dataToSend) {
        super(cause);
        this.dataToSend = dataToSend;
    }

    public String getDataToSend() {
        return dataToSend;
    }
}
