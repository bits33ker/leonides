package com.herod.sip.interfaces;

import javax.sip.ResponseEvent;

/**
 * Created by eugenio.voss on 7/6/2017.
 */
public interface ISipResponseEvents {
    void doRinging(ResponseEvent responseEvent);
    void doSessionProgress(ResponseEvent responseEvent);
    void doSuccess(ResponseEvent responseEvent);
    void do4XX(ResponseEvent responseEvent, boolean bAck);
    void doTry(ResponseEvent responseEvent);
}
