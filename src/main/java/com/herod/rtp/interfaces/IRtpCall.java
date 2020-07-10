package com.herod.rtp.interfaces;

import com.herod.leonides.utils.FreePorts;
import gov.nist.javax.sip.message.SIPMessage;

import javax.sdp.SdpException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Optional;

public interface IRtpCall {
    public static FreePorts freePorts = new FreePorts();
    public static String mediaContentType = "application/sdp";

    //void startRtp() throws IOException, RtpException, SdpException;
    //void stopRtp();
    void setResponseMedia(SIPMessage msg);// throws ParseException, SdpException, IOException;
    void setCallerMedia() throws SdpException, IOException;
    public Optional<String> getSessionDescription();

}
