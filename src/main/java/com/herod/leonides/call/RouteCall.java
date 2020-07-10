package com.herod.leonides.call;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.herod.rtp.RtpCodec;
import com.herod.rtp.RtpCodecException;
import com.herod.rtp.RtpMime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RouteCall{
    @JsonProperty("Id")
    long id;
    @JsonProperty("prefix")
    String inExtension;//prefijo
    @JsonProperty("net")
    String outsrv;//interfaz de salida
    @JsonProperty("postfix")
    String outExtension;//postfijo
    String transport;
    List<RtpCodec> codecs = new ArrayList<RtpCodec>();
    public RouteCall(long i, String ifx, String osrv, String ofx, String t)
    {
        id = i;
        inExtension = ifx;
        outExtension = ofx;
        outsrv = osrv;
        transport = t;
    }
    public void addCodec(String c) throws RtpCodecException {
        RtpMime mime = RtpMime.getByName(c);
        int codec = -1;
        switch(c){
            case "PCMU":
                codec = 0;
                break;
            case "PCMA":
                codec = 8;
                break;
            case "G729":
                codec = 18;
                break;
            case "OPUS":
                codec = 96;
                break;
        }
        codecs.add(new RtpCodec(mime, codec, 8000));
    }
    public void addCodec(RtpCodec c)
    {
        codecs.add(c);
    }
    public void addAllCodecs(Map<String, RtpCodec> c){
        for(Map.Entry<String, RtpCodec> entry : c.entrySet()) {
            codecs.add((RtpCodec)entry.getValue());
        }
    }
    public String getInExtension(){
        return inExtension;
    }

    public String getOutsrv() {
        return outsrv;
    }

    public String getOutExtension(){
        return outExtension;
    }

    public String getTransport() {
        return transport;
    }

    public List<RtpCodec> getCodecs() {
        return codecs;
    }
    public Long getId()
    {
        return id;
    }
};
