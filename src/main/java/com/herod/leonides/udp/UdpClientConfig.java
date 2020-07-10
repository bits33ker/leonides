package com.herod.leonides.udp;

public class UdpClientConfig {
	private String address;
	private Integer remotePort;
	private int socketTimeoutMs;
	private String identifier;
	
	public UdpClientConfig(String address, Integer remotePort, int socketTimeoutMs, String identifier) {
	    super();
	    this.address = address;
	    this.remotePort = remotePort;
	    this.socketTimeoutMs = socketTimeoutMs;
	    this.identifier = identifier;
    }
	public String getAddress() {
		return address;
	}
	public Integer getRemotePort() {
		return remotePort;
	}
	public int getSocketTimeoutMs() {
		return socketTimeoutMs;
	}

	public String getIdentifier() {
		return identifier;
	}
}
