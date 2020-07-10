package com.herod.sip.call;

import gov.nist.javax.sdp.fields.AttributeField;
import gov.nist.javax.sip.address.AddressImpl;
import com.herod.sip.interfaces.IEndPoint;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sip.address.Address;
import java.util.Vector;

/**
 * Created by eugenio.voss on 3/3/2017.
 */
public class RemoteEndPoint implements IEndPoint{
    //configuracion
    private boolean rtcpEnabled;
    private boolean videoEnabled;

    //media
    private MediaDescription audioDescription = null;
    private MediaDescription videoDescription = null;

    //puertos
    private int audioPort = -1;
    private int rtcpPort = -1;
    private int videoPort = -1;
    //parametros sip
    private int sipPort = 5060;
    private AddressImpl address;
    private String user;
    private String tag;


    public RemoteEndPoint(boolean rtcp, boolean video){
        rtcpEnabled = rtcp;
        videoEnabled = video;
    }
    //getters
    public int getAudioPort() {
        return audioPort;
    }

    public int getRtcpPort() {
        return rtcpPort;
    }

    public int getVideoPort() {
        return videoPort;
    }

    public MediaDescription getAudioDescription() {
        return audioDescription;
    }

    public MediaDescription getVideoDescription() throws SdpException {
        return videoDescription;
    }

    public boolean isRtcpEnabled() {
        return rtcpEnabled;
    }

    public boolean isVideoEnabled() {
        return videoEnabled;
    }

    @Override
    public int getSipPort() {
        return sipPort;
    }

    @Override
    public Address getAddress() {
        return address;
    }

    @Override
    public String getUser() {
        return user;
    }

    @Override
    public String getTag() {
        return tag;
    }

    //setters

    public void setAudioPort(int audioPort) throws SdpException {
        this.audioPort = audioPort;
    }

    public void setRtcpPort(int rtcpPort) throws SdpException {
        if(!isRtcpEnabled())
            return;
        this.rtcpPort = rtcpPort;
    }

    public void setVideoPort(int videoPort) throws SdpException {
        if(!isVideoEnabled())
            return;
        this.videoPort = videoPort;
    }

    public int setAudioDescription(MediaDescription audioDescription) throws SdpException {
        this.audioDescription = audioDescription;
        audioPort = this.audioDescription.getMedia().getMediaPort();
        if (audioDescription.getAttribute("rtcp") != null && isRtcpEnabled()) {
            rtcpPort = Integer.parseInt(this.audioDescription.getAttribute("rtcp").split(" ")[0]);
        }
        else
            rtcpEnabled = false;
        if(!isRtcpEnabled())
            this.audioDescription.removeAttribute("rtcp");

        Object[] stream = (Object[] )this.audioDescription.getAttributes(true).stream().filter(x -> {
            try {
                return ((AttributeField) x).getName().equals("rtpmap");
            } catch (SdpParseException e) {
                e.printStackTrace();
            }
            return false;
        }).toArray();
        if(stream.length>0 && stream.length<=2) {
            for(Object attr : stream)
            {
                int c = -1;
                c = Integer.parseInt(((AttributeField)attr).getValue().split(" ")[0]);
                if(c>=0 && c!=101)
                    return c;
            }
        }
        Vector<String> vector = this.audioDescription.getMedia().getMediaFormats(true);
        if(vector.size()>0 && vector.size()<=3) {
                int i = vector.size()-1;
                while (i>=0) {
                    String str= vector.get(i);
                    int c = Integer.parseInt(str);
                    if(c>=0 && c!=101)
                        return c;
                    i--;
                }
            }
        return -1;
    }

    public void setVideoDescription(MediaDescription videoDescription) throws SdpException {
        if(!isVideoEnabled())
            return;
        this.videoDescription = videoDescription;
        if(videoDescription!=null)
            videoPort = this.videoDescription.getMedia().getMediaPort();
    }

    @Override
    public void setSipPort(int port) {
        sipPort = port;
    }

    @Override
    public void setAddress(Address address) {
        this.address = (AddressImpl)address;
    }

    @Override
    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public void setTag(String tag) {
        this.tag = tag;
    }

}
