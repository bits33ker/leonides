package com.herod.sip.interfaces;

import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import java.text.ParseException;

/**
 * Created by eugenio.voss on 7/6/2017.
 */
public interface ISipRequestEvents {
    void doRegister(RequestEvent requestEvent) throws SipException, InvalidArgumentException, ParseException;
    void doSubscribe(RequestEvent requestEvent) throws SipException, InvalidArgumentException, ParseException;
    void doInvite(RequestEvent requestEvent) throws SipException, InvalidArgumentException, ParseException;
    void doRefer(RequestEvent requestEvent) throws SipException, InvalidArgumentException, ParseException;
    void doBye(RequestEvent requestEvent) throws SipException, InvalidArgumentException, ParseException;
    void doPrack(RequestEvent requestEvent) throws SipException, InvalidArgumentException, ParseException;
    void doCancel(RequestEvent requestEvent) throws SipException, InvalidArgumentException, ParseException;
    void doAck(RequestEvent requestEvent);
}
