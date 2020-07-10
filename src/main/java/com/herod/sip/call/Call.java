/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package com.herod.sip.call;

//import org.mobicents.servlet.sip.example.rtp.RTCPStack;

import com.herod.rtp.RtpCall;
import com.herod.rtp.interfaces.IRtpCall;
import gov.nist.javax.sip.message.SIPRequest;
import com.herod.sip.interfaces.ICall;
import org.apache.log4j.Logger;

import javax.sdp.SdpException;
import javax.sip.PeerUnavailableException;
import javax.sip.address.Address;
import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Vector;

public class Call implements ICall
{

	public static int Inbound=0;
	public static int OutBound=1;

	IRtpCall rtpCall;

	protected static Logger logger = Logger.getLogger(Call.class);
	//private Contact contact = null;
	int maxForwards;

	private boolean delayOffer = false;
	private final Object lockStatus = new Object();
	private CallStatus status;
	private Call referredBy;
	private Call referTo;

	private CallStats callStats = new CallStats();

	private Vector<CallEndPoint> addressParties = new Vector<CallEndPoint>();//0:inbound, 1:outbound, 2: conference
	//protected HashMap<CallMedia, RtpListener> mediaListenerHashMap = new HashMap<CallMedia, RtpListener>();

	public Call(SIPRequest request) throws PeerUnavailableException, SdpException, ParseException, IOException, ExpiredException {
		status = CallStatus.idle;
		//setContact(request.getContactHeader());
		addressParties.add(new CallEndPoint(request, true));
		maxForwards = request.getMaxForwards().getMaxForwards();
		if (maxForwards <= 0) {
			throw new ExpiredException("ERROR. Message expired");
		}
		rtpCall = (IRtpCall) new RtpCall(request);
	}

	public IRtpCall getRtpCall()
	{
		return rtpCall;
	}

	@Override
	public boolean isDelayOffer() {
		return delayOffer;
	}

	@Override
	public CallEndPoint getInbound(){
		if(this.getAddressParties().size()>0)
			return this.getAddressParties().get(Call.Inbound);
		return null;
	}

	@Override
	public CallEndPoint getOutbound(){
		if(this.getAddressParties().size()> Call.OutBound)
			return this.getAddressParties().get(Call.OutBound);
		return null;
	}

	public CallEndPoint getPartyAddress(String callId){
		Iterator<CallEndPoint> itPartyAddress = this.getAddressParties().iterator();
		while(itPartyAddress.hasNext()) {
			CallEndPoint callEndPoint = itPartyAddress.next();
			if (callId.equals(callEndPoint.getCallId())) {
				return callEndPoint;
			}
		}
		return null;
	}

	public Call getReferredBy() {
		return referredBy;
	}

	public void setReferredBy(Call referredBy) {
		this.referredBy = referredBy;
	}

	public Call getReferTo() {
		return referTo;
	}

	public void setReferTo(Call referTo) {
		this.referTo = referTo;
	}

	public CallStats getCallStats() {
		return callStats;
	}

    public CallEndPoint add(SIPRequest req, boolean inbound) throws IOException, ParseException, SdpException, PeerUnavailableException {

        CallEndPoint party = new CallEndPoint(req, inbound);
        this.addressParties.add(party);
		//callStats.add(party.getPartyStats());

         return party;
    }
/*
	public CallEndPoint add(CallEndPoint orig, AddressImpl fromAddr, String fromTag, AddressImpl toAddr) throws IOException, ParseException, SdpException, SipException {
		From from = CallEndPoint.cloneFrom(fromAddr, fromTag);
		To to = CallEndPoint.cloneTo(toAddr, null);

		CallEndPoint party = new CallEndPoint(from, to, orig.isInbound(), orig.getCallId());
		party.setRequest(orig.getRequest());
		this.addressParties.add(party);
		//callStats.add(party.getPartyStats());

		delayOffer = orig.isDelayOffer();

		return party;
	}*/

	public Vector<CallEndPoint> getAddressParties() {
		return addressParties;
	}


	@Override
	public CallType getType() {
		return CallType.common;
	}

	@Override
	public CallStatus getStatus() {
		synchronized (lockStatus) {
			return this.status;
		}
	}

	public void setStatus(CallStatus status) {
		synchronized (lockStatus) {
			this.status = status;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getInbound() == null) ? 0 : getInbound().hashCode());
		result = prime * result + ((getOutbound() == null) ? 0 : getOutbound().hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Call other = (Call) obj;
		if (this.getInbound() == null) {
			if (other.getInbound() != null)
				return false;
		} else if (!this.getInbound().equals(other.getInbound()))
			return false;
		if (this.getOutbound() == null) {
			if (other.getOutbound() != null)
				return false;
		} else if (!this.getOutbound().equals(other.getOutbound()))
			return false;
		return true;
	}

	public Address getTagAddress(String tag){
		for(CallEndPoint party : addressParties){
			if(party.getFrom().getTag().equals(tag))
				return party.getFrom().getAddress();
			if(party.getTo().getTag().equals(tag))
				return party.getTo().getAddress();
		}
		return null;
	}
}