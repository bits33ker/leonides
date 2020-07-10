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

import gov.nist.javax.sip.header.From;
import gov.nist.javax.sip.header.To;

import javax.sip.header.CallIdHeader;
import javax.sip.message.Message;
import java.util.HashSet;
import java.util.Iterator;

public class CallStatusContainer{
	
	protected HashSet<Call> activeCalls = new HashSet<Call>();

	public void removeCall(Call call) {
		activeCalls.remove(call);
	}
	
	public Call getCall(String from, String to) {
		String extFrom = from.substring(0, from.indexOf('@'));
		String extTo = to.substring(0, to.indexOf('@'));
		Iterator<Call> it = activeCalls.iterator();
		while(it.hasNext()) {
			Call call = it.next();
			CallEndPoint inbound = call.getInbound();
			String f1 = ((From)inbound.getFrom()).getUserAtHostPort().substring(0, ((From)inbound.getFrom()).getUserAtHostPort().indexOf('@'));
			String t1 = ((To)inbound.getTo()).getUserAtHostPort().substring(0, ((To)inbound.getTo()).getUserAtHostPort().indexOf('@'));
			if(extFrom.equals(f1) && extTo.equals(t1))
				return call;
		}
		return null;
	}
	/*
	public Call getCall(Request msg) {
		String callId = ((CallIdHeader) msg.getHeader(CallIdHeader.NAME)).getCallId();
		//String callid = msg.getCallId().getCallId();
		Iterator<Call> it = activeCalls.iterator();
		while(it.hasNext()) {
			Call call = it.next();
			Iterator<CallEndPoint> itPartyAddress = call.getAddressParties().iterator();
			while(itPartyAddress.hasNext()) {
				CallEndPoint partyAddress = itPartyAddress.next();
				if (callId.equals(partyAddress.getCallId())) {
					return call;
				}
			}
		}
		return null;
	}*/

	public Call getCall(Message msg) {
		String callId = ((CallIdHeader) msg.getHeader(CallIdHeader.NAME)).getCallId();
		//String callid = msg.getCallId().getCallId();
		Iterator<Call> it = activeCalls.iterator();
		while(it.hasNext()) {
			Call call = it.next();
			Iterator<CallEndPoint> itPartyAddress = call.getAddressParties().iterator();
			while(itPartyAddress.hasNext()) {
				CallEndPoint callEndPoint = itPartyAddress.next();
				if (callId.equals(callEndPoint.getCallId())) {
					//TODO. aparte de chequear el callid debo chequear el Branch y el CSeq.
					//TODO. Debo guardar los branch para saber a que responde.
					//String branch = ((ViaHeader) msg.getHeader(ViaHeader.NAME)).getBranch();
					//Request req = callEndPoint.getRequest();
					//if(req!=null && branch.equals(((ViaHeader)req.getHeader(ViaHeader.NAME)).getBranch()))
					return call;
				}
			}
		}
		return null;
	}

	public Call getCall(String callId) {
		//String callId = ((CallIdHeader) msg.getHeader(CallIdHeader.NAME)).getCallId();
		if(callId==null)
			return null;
		if(activeCalls.size()==0)
			return null;
		if(callId=="")
		{//entrego la primera
			return activeCalls.iterator().next();
		}
		Iterator<Call> it = activeCalls.iterator();
		while(it.hasNext()) {
			Call call = it.next();
			Iterator<CallEndPoint> itPartyAddress = call.getAddressParties().iterator();
			while(itPartyAddress.hasNext()) {
				CallEndPoint callEndPoint = itPartyAddress.next();
				if (callId.equals(callEndPoint.getCallId())) {
					return call;
				}
			}
		}
		return null;
	}

	public Call addCall(Call call) {
		activeCalls.add(call);
		return call;
	}

	public CallStatus getStatus(String from, String to) {
		Call call = getCall(from,to);
		if(call != null) {
			return call.getStatus();
		} else {
			return null;
		}
	}
	public boolean isStatus(CallStatus status) {
		Iterator<Call> it = activeCalls.iterator();
		while(it.hasNext()) {
			Call call = it.next();
			if(call.getStatus()==status)
				return  true;
		}
		return false;
	}
	public void removeAll()
	{
		while(activeCalls.size()>0)
		{
			Iterator it = activeCalls.iterator();
			Call c = (Call) it.next();
			removeCall(c);
		}
	}
	public int size()
	{
		return activeCalls.size();
	}

	public CallStatusContainer getCallIds(String host)
	{
		CallStatusContainer callids = new CallStatusContainer();
		Iterator<Call> it = activeCalls.iterator();
		while(it.hasNext()) {
			Call call = it.next();
			boolean isHost=false;
			Iterator<CallEndPoint> itPartyAddress = call.getAddressParties().iterator();
			while(itPartyAddress.hasNext()) {
				CallEndPoint callEndPoint = itPartyAddress.next();
				if(callEndPoint.getCallId().contains(host))
					isHost = true;
			}
			if(isHost)
				callids.addCall(call);
		}
		return callids;
	}
}
