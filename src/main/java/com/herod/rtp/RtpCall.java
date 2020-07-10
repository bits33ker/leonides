package com.herod.rtp;

import com.herod.rtp.interfaces.IRtpCall;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;

import javax.sdp.SdpException;
import java.io.IOException;
import java.util.Optional;

public class RtpCall implements IRtpCall{
    // indica como es la llamada a nivel rtp. puertos, codecs, ...

    //configuracion
    private boolean rtcpEnabled;
    private boolean videoEnabled;

    String sessionDescription;

    public RtpCall(SIPRequest request) {
        String contentType = request.getContentTypeHeader().getMediaRange().toString();
        if (contentType == null || !contentType.trim().equals(IRtpCall.mediaContentType) || request.getContentLength().getContentLength() <= 0)
            return;
        sessionDescription = new String(
                (byte[]) request.getContent());
    }
    public String toString(){return sessionDescription;}
    public Optional<String> getSessionDescription()
    {
        if(sessionDescription==null)
            return Optional.empty();
        return Optional.of(sessionDescription);
    }

    @Override
    public void setResponseMedia(SIPMessage msg){
        String contentType = msg.getContentTypeHeader().toString();
        if (contentType == null || !contentType.contains(IRtpCall.mediaContentType) || msg.getContentLength().getContentLength() <= 0)
            return;
        sessionDescription = new String(
                (byte[]) msg.getContent());
    }

    @Override
    public void setCallerMedia() throws SdpException, IOException {

    }
}
