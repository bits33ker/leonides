package com.herod.sip.interfaces;

import gov.nist.javax.sip.address.AddressImpl;
import com.herod.sip.call.Call;

import javax.sdp.SdpException;
import javax.sip.InvalidArgumentException;
import javax.sip.SipException;
import javax.sip.address.Address;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.ParseException;

/**
 * Created by eugenio.voss on 15/6/2017.
 */
public interface ISoftPhone {
    //estas 2 solo para telefonos, no para Karen.
    //Call sendCall( Address toAddr, boolean delayOffer, int expirationTime) throws SipException, ParseException, InvalidArgumentException, SdpException, IOException;
    //Call sendCall(Address toAddr, byte[] sdp, boolean delayOffer, int expirationTime) throws SipException, ParseException, InvalidArgumentException, SdpException, IOException;

    void sendRegister(Address toAddr, int expirationTime) throws ParseException, InvalidArgumentException, UnknownHostException, SipException;
    void sendBye(Call call) throws SipException, UnknownHostException, ParseException, InvalidArgumentException;
    void sendCancel(Call call) throws SipException, UnknownHostException, ParseException, InvalidArgumentException;
    void sendRefer(Call call, AddressImpl to) throws ParseException, SipException, InvalidArgumentException, IOException, SdpException;
}
