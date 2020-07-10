package com.herod.leonides.udp;

public interface UdpPacketConsumer {
	void consume(String requestPayload, String responsePayload);
	
	void onError(UdpClientSendException t);

}
