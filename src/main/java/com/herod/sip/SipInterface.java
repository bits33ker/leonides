package com.herod.sip;

/**
 * Created by eugenio.voss on 15/8/2018.
 * DAtos de Server en el Karen.json
 */
public class SipInterface {
    String id;
    String protocol;
    String net;
    int port;
    String ip;
/*
    public SipInterface(String id, String protocol, String net, int port)
    {
        this.id = id;
        this.protocol = protocol;
        this.net = net;
        this.port = port;
        try {
            this.ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            ip = "127.0.0.1";
        }
    }*/

    public SipInterface(String id, String protocol, String net, String ip, int port)
    {
        this.id = id;
        this.protocol = protocol;
        this.net = net;
        this.ip = ip;
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public String getId() {
        return id;
    }

    public String getNet() {
        return net;
    }

    public String getProtocol() {
        return protocol;
    }
}
