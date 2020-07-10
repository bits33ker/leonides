package com.herod.leonides.udp;

import com.google.common.util.concurrent.*;
import com.herod.utils.ThreadBuilder;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Callable;

import static com.herod.leonides.udp.UdpClient.UdpClientStatus.AVAILABLE;

public class UdpClient implements DisposableBean {
    private static final long RETRY_INTERVAL = 1000;
    private static final int udp_port = 35000;
    private static final int MTU = 1280; //IPV6 MTU(Maximum Transmit Unit)
    private static Logger logger = Logger.getLogger(UdpClient.class);
    private final int socketTimeoutMs;
    private final InetAddress address;
    private final Integer remotePort;
    private DatagramSocket socket;
    private UdpClientStatus status;
    private ListeningExecutorService service;
    private String identifier;

    public UdpClient(UdpClientConfig udpClientConfig) throws IOException {
        this.socketTimeoutMs = udpClientConfig.getSocketTimeoutMs();
        this.address = InetAddress.getByName(udpClientConfig.getAddress());
        this.remotePort = udpClientConfig.getRemotePort();
        this.identifier = udpClientConfig.getIdentifier();
        service = MoreExecutors.listeningDecorator(ThreadBuilder.buildNewFixedThreadPool(2, String.format("UDPClientService-%s:%s", address, remotePort)));
    }

    public void send(String payload, UdpPacketConsumer consumer) throws InterruptedException {
        logger.debug("Send via UDP payload= " + payload);
        Callable<String> callableTask = new UdpSenderCallable(payload);
        ListenableFuture<String> future = service.submit(callableTask);

        Futures.addCallback(future, new FutureCallback<String>() {
            public void onSuccess(String data) {
                consumer.consume(payload, data);
            }

            public void onFailure(Throwable thrown) {
                UdpClientSendException exception = new UdpClientSendException(thrown, payload);
                consumer.onError(exception);
                if (!future.isDone()) {
                    future.cancel(true);
                }
            }
        });
    }

    private DatagramSocket resolveSocket() {
        if (socket == null) {
            socket = safeCreate();
            return socket;
        }

        if (!socket.isBound()) {
            socket.close();
            //NetUtils.freePorts(this);
            socket = safeCreate();
        }

        return socket;
    }

    @SuppressWarnings("resource")
    private DatagramSocket safeCreate() {
        DatagramSocket socket;
        try {
            socket = new DatagramSocket(udp_port);
            this.status = AVAILABLE;
            socket.setSoTimeout(socketTimeoutMs);
        } catch (IOException e) {
            logger.info("Fallo la creacion del socket UDP a " + address + ":" + remotePort + ". Reintentado en " + RETRY_INTERVAL + " ms.");
            socket = retryCreate();
        }
        return socket;
    }

    private DatagramSocket retryCreate() {
        DatagramSocket socket;
        try {
            Thread.sleep(RETRY_INTERVAL);
            socket = safeCreate();
        } catch (InterruptedException e1) {
            socket = null;
        }
        return socket;
    }

    public void close() {
        logger.info("close");
        if (socket != null) {
            socket.close();
        }

        //NetUtils.freePorts(this);
        if (service != null) {
            service.shutdown();
            service = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        this.close();
    }

    public UdpClientStatus getStatus() {
        return status;
    }

    public boolean isAvailable() {
        return this.status.equals(UdpClientStatus.AVAILABLE);
    }

    @Override
    public void destroy() throws Exception {
        logger.info("destroy");
        this.close();
    }

    enum UdpClientStatus {
        AVAILABLE,
        UNAVAILABLE
    }

    public class UdpSenderCallable implements Callable<String> {

        private String command;

        public UdpSenderCallable(String payload) {
            this.command = payload;
        }

        @Override
        public String call() throws Exception {
            // En teoria socket es thread safe, si falla entonces cada Callable
            // tendra q instanciar su propio socket y cerrarlo
            DatagramSocket socket = resolveSocket();
            DatagramPacket request = new DatagramPacket(command.getBytes(), command.length(), address, remotePort.intValue());
            DatagramPacket response = new DatagramPacket(new byte[MTU], MTU);
            socket.send(request);
            //socket.receive(response);
            //String stringResponse = new String(response.getData());
            //return stringResponse.trim();
            return "OK";
        }
    }
}
