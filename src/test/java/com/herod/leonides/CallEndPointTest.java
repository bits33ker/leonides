package com.herod.leonides;


import com.herod.sip.SipServer;
import com.herod.sip.call.CallEndPoint;
import gov.nist.javax.sip.SipProviderImpl;
import gov.nist.javax.sip.header.SIPHeader;
import gov.nist.javax.sip.header.ims.PAssertedIdentityHeader;
import gov.nist.javax.sip.header.ims.PPreferredIdentityHeader;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import org.junit.Before;
import org.junit.Test;

import javax.sdp.SdpFactory;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.SipProvider;
import javax.sip.address.AddressFactory;
import javax.sip.header.ExtensionHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.text.ParseException;

public class CallEndPointTest {
    //https://github.com/RestComm/jain-sip/blob/master/src/examples/parser/Parser.java
    String invite = "INVITE sip:5409@192.168.41.230 SIP/2.0\r\n" +
            "Via: SIP/2.0/UDP 192.168.41.53:10282;rport;branch=z9hG4bKddsEurK27289f00.sa23954ptb7Rm\r\n" +
            "From: \"muke\" <sip:8352@192.168.41.53>;tag=481500432\r\n" +
            "To: <sip:5409@192.168.41.230>\r\n" +
            "Call-ID: 112734658-0@192.168.41.53\r\n" +
            "CSeq: 1 INVITE\r\n" +
            "Contact: <sip:8352@192.168.41.53:10282;transport=udp>\r\n" +
            "Max-Forwards: 70\r\n" +
            "User-Agent: MitE1x v6.0.1.3\r\n" +
            "Expires: 300\r\n" +
            "Allow: INVITE,ACK,CANCEL,BYE,REGISTER,SUBSCRIBE,NOTIFY,REFER,OPTIONS,INFO\r\n" +
            "P-Early-Media: Supported\r\n" +
            "P-Mitrol-idLlamada: 200604145845354_MIT_00199\r\n" +
            "P-Mitrol-LoginID: muke\r\n" +
            "P-Mitrol-PerfilRuteo: 1\r\n" +
            "Content-Length: 282\r\n" +
            "Content-Type: application/sdp\r\n" +
            "\r\n" +
            "v=0\r\n" +
            "o=8352 2001824844 1 IN IP4 192.168.41.53\r\n" +
            "s=MitE1x Call\r\n" +
            "c=IN IP4 192.168.41.53\r\n" +
            "t=0 0\r\n" +
            "m=audio 36086 RTP/AVP 8 0 18 90\r\n" +
            "a=sendrecv\r\n" +
            "a=rtpmap:8 PCMA/8000/1\r\n" +
            "a=rtpmap:0 PCMU/8000/1\r\n" +
            "a=rtpmap:18 G729/8000/1\r\n" +
            "a=fmtp:18 annexb=no\r\n" +
            "a=rtpmap:90 telephone-event/8000\r\n" +
            "a=fmtp:90 0-15\r\n";

        String sessionProgress = "SIP/2.0 183 Session Progress\r\n" +
                "Via: SIP/2.0/UDP 192.168.41.53:10282;rport;branch=z9hG4bKddsEurK27289f00.sa23954ptb7Rm\r\n" +
                "From: \"muke\" <sip:8352@192.168.41.53>;tag=481500432\r\n" +
                "To: <sip:5409@192.168.41.230>;tag=24097280\r\n" +
                "Call-ID: 112734658-0@192.168.41.53\r\n" +
                "CSeq: 1 INVITE\r\n" +
                "Server: MitE1xv4.4.5.973\r\n" +
                "Allow: INVITE,ACK,CANCEL,BYE,REGISTER,SUBSCRIBE,NOTIFY,REFER,OPTIONS,INFO\r\n" +
                "P-Mitrol-idLlamada: 200604145845354_MIT_00199\r\n" +
                "P-Mitrol-LoginID: muke\r\n" +
                "Content-Length: 209\r\n" +
                "Content-Type: application/sdp\r\n" +
                "\r\n" +
                "v=0\r\n" +
                "o=8352 2001824844 1 IN IP4 192.168.41.230\r\n" +
                "s=MitE1x Call\r\n" +
                "c=IN IP4 192.168.41.230\r\n" +
                "t=0 0\r\n" +
                "m=audio 35972 RTP/AVP 8 90\r\n" +
                "a=sendrecv\r\n" +
                "a=rtpmap:8 PCMA/8000/1\r\n" +
                "a=rtpmap:90 telephone-event/8000\r\n" +
                "a=fmtp:90 0-15\r\n";

    SipFactory sipFactory = null;
    HeaderFactory headerFactory;
    AddressFactory addressFactory;
    MessageFactory messageFactory;

    @Before
    public void init() throws PeerUnavailableException {
        sipFactory = SipFactory.getInstance();
        sipFactory.setPathName("gov.nist");

        headerFactory = sipFactory.createHeaderFactory();
        addressFactory = sipFactory.createAddressFactory();
        messageFactory = sipFactory.createMessageFactory();

    }

    @Test
    public void testInvite() throws ParseException {

        Request sipRequest = messageFactory.createRequest(invite);
        byte[] contentBytes = sipRequest.getRawContent();
        String contentString = new String(contentBytes);
        //SdpFactory sdpFactory = SdpFactory.getInstance();
        //SessionDescription sd = sdpFactory
        //      .createSessionDescription(contentString);

        CallEndPoint c = new CallEndPoint((SIPRequest)sipRequest, true);
        System.out.println("From: " + c.getFrom().toString());
        System.out.println("To: " + c.getTo().toString());
        System.out.println("Contact: " + c.getContact().toString());
        System.out.println("Call-Id: " + c.getCallId().toString());
        System.out.println("Via: " + c.getVia().toString());
    }

    @Test
    public void sessionProgressTest() throws ParseException, SdpParseException {
        Response sipResponse = messageFactory.createResponse(sessionProgress);
        System.out.println("Parsed SIP Response is :\n" + sipResponse);
        byte[] contentBytes = sipResponse.getRawContent();
        String contentString = new String(contentBytes);
        SdpFactory sdpFactory = SdpFactory.getInstance();
        SessionDescription sd = sdpFactory.createSessionDescription(contentString);
        System.out.println("Parsed Content is :\n" + sd.toString());

    }
}
