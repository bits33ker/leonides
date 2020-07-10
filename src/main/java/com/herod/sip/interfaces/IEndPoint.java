package com.herod.sip.interfaces;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sip.address.Address;
import java.net.UnknownHostException;

/**
 * Created by eugenio.voss on 3/3/2017.
 */
public interface IEndPoint {
    //getters
    int getAudioPort();
    int getRtcpPort();
    int getVideoPort();
    MediaDescription getAudioDescription();
    MediaDescription getVideoDescription() throws SdpException;
    //parametros sip
    int getSipPort();
    Address getAddress();
    String getUser();
    String getTag();
    //setters
    void setAudioPort(int port) throws SdpException;
    void setRtcpPort(int port) throws SdpException, UnknownHostException;
    void setVideoPort(int port) throws SdpException;
    int setAudioDescription(MediaDescription media) throws SdpException, UnknownHostException;
    void setVideoDescription(MediaDescription media) throws SdpException;
    //parametros sip
    void setSipPort(int port);
    void setAddress(Address address);
    void setUser(String user);
    void setTag(String tag);
}
