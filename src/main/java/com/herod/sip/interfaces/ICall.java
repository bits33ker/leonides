package com.herod.sip.interfaces;

import com.herod.sip.call.CallEndPoint;
import com.herod.sip.call.CallStatus;
import com.herod.sip.call.CallType;

/**
 * Created by eugenio.voss on 31/1/2017.
 * Interface de la cual derivan Call, IvrCall, ...
 *
 */
public interface ICall {
    CallType getType();
    CallStatus getStatus();
    boolean isDelayOffer();
    CallEndPoint getInbound();
    CallEndPoint getOutbound();
}
