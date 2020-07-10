package com.herod.leonides.udp;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import com.herod.leonides.server.UdpServer;
import com.herod.leonides.utils.*;
import com.herod.leonides.call.RouteManager;
import com.herod.utils.ThreadBuilder;
import com.herod.utils.entities.ParameterBag;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sip.address.Address;
import javax.sip.address.SipURI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;

public class ClusterManager implements UdpPacketConsumer, UdpServer.UdpListener, DisposableBean {

    private static Logger logger = Logger.getLogger(ClusterManager.class);

    HashMap<String, AbonadoRemoto> remotos = new HashMap<>();//usuarios remotos registrados via cluster

    Vector<UdpClientConfig> mite1xs = new Vector<>();//lista de mite1x a registrar los abonados
    int port;//puerto a escuchara para abonados de otros mites que quieran registarse con Karina.
    int expires;//tiempo en el que hay que registrar el abonado
    UdpServer udpServer;

    @Autowired
    RouteManager routeManager;

    private ScheduledExecutorService service;

    public ClusterManager(String clusters, int expires, int port) throws Exception {
        super();
        //this.calendar = calendar;
        this.expires = expires;
        this.port = port;
        if (clusters == null)
            return;
        try {
            String[] cl = clusters.split(",");
            for (String c : cl) {
                String[] hostPort = c.split(":");
                UdpClientConfig cfg = new UdpClientConfig(hostPort[0], hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : port, expires, "");
                mite1xs.add(cfg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mite1xs.size() == 0)
            return;
        udpServer = new UdpServer(port);
        udpServer.addUdpServerListener(this); // end Listener
        udpServer.start();

        service = ThreadBuilder.buildNewSingleScheduledExecutorService("ClusterManager-CheckerService");
        service.scheduleAtFixedRate(new ScheduledUpdateRequester(this), expires, expires, TimeUnit.SECONDS);
    }

    public ClusterManager(JsonObject jsonObject) throws Exception {
        super();
        //this.calendar = calendar;
        this.expires = jsonObject.get("expires").getAsInt();
        this.port = jsonObject.get("port").getAsInt();
        JsonArray pbxs = (JsonArray) jsonObject.get("pbx");
        if (pbxs == null)
            return;
        try {
            Iterator<JsonElement> iterator = pbxs.iterator();
            while (iterator.hasNext()) {
                String[] hostPort = iterator.next().getAsString().split(":");
                UdpClientConfig cfg = new UdpClientConfig(hostPort[0], hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : port, expires, "");
                mite1xs.add(cfg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mite1xs.size() == 0)
            return;
        udpServer = new UdpServer(port);
        udpServer.addUdpServerListener(this); // end Listener
        udpServer.start();

        service = ThreadBuilder.buildNewSingleScheduledExecutorService("ClusterManager-CheckerService");
        service.scheduleAtFixedRate(new ScheduledUpdateRequester(this), expires, expires, TimeUnit.SECONDS);
    }

    @Override
    public void packetReceived( UdpServer.Event evt ) {     // Packet received
        try {
            //proceso comando 2000.
            logger.info(evt);
            //TODO. quedan en el Mite1x los comandos 3000 y 3100 que son Subscribes. Funcionan??
            String[]campos = evt.getPacketAsString().split("&");
            String msg="CLUSTER RECV= ";

            String ext="";
            String ip="";
            int port=0;
            int expires=0;
            for(String c:campos){
                String[]kv = c.split("=");
                if(kv.length>1){
                    msg += kv[0].toUpperCase() + ": " + kv[1].toUpperCase() + "\t ";
                    if(kv[0].toUpperCase().equals("EXT"))
                        ext = kv[1];
                    if(kv[0].toUpperCase().equals("IP"))
                        ip = kv[1];
                    if(kv[0].toUpperCase().equals("PORT"))
                        port = Integer.parseInt(kv[1]);
                    if(kv[0].toUpperCase().equals("EXPIRES"))
                        expires = Integer.parseInt(kv[1]);
                }
            }
            AddressImpl address = new AddressImpl();
            SipURI uri = new SipUri();
            uri.setUser(ext);
            uri.setHost(ip);
            uri.setPort(port);
            address.setURI(uri);

            AddressImpl pbx = new AddressImpl();
            SipURI puri = new SipUri();
            puri.setUser(ext);//se lo agrego para usar este address para redireccionar llamadas
            puri.setHost(evt.getPacket().getAddress().getHostAddress());
            //TODO. Hacer que el Mite1x envie el puerto de la pbx.
            puri.setPort(5060);
            pbx.setURI(puri);
            if(expires!=0)
                addAbonadoRemoto(address, pbx);
            else
                remAbonadoRemoto(address);
            logger.info( msg );
        } catch( Exception ex ) {
            ex.printStackTrace(); // Please don't use printStackTrace in production code
        }   // end ctach
    }

    @Override
    public void consume(String requestPayload, String responsePayload) {
        logger.debug("ClusterManager :" + requestPayload + " " + responsePayload);
    }

    @Override
    public void onError(UdpClientSendException t) {
        logger.error(t);
    }

    private boolean oneDayFromLastDayStart(Instant lastDayStart, Instant now) {
        if (ChronoUnit.DAYS.between(lastDayStart, now) > 0) {
            return true;
        }
        return false;
    }

    private Instant resolveDayStart(Instant now) {
        return now.atZone(ZoneId.systemDefault())
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .toInstant();
    }

    private String resolveCommand(ParameterBag parameterBag) {
        Optional<String> command = ofNullable(parameterBag.getString("cmd"));
        if (command.isPresent()) {
            return command.get();
        }
        command = ofNullable(parameterBag.getString("CMD"));

        return command.orElseThrow(() -> new IllegalStateException("Deberia tener el parametro 'CMD' o 'cmd'."));
    }

    public String buildExtensionUpdateCommand(String ext, String nombre, int expires) throws Exception {
        String[] userhost = ext.split("@");
        String[] hostport = userhost[1].split(":");
        int port = hostport.length > 1 ? Integer.parseInt(hostport[1]) : 5060;
        return "cmd=2000&ext=" + userhost[0] +
                "&nombre=" + nombre +
                "&expires=" + Integer.toString(expires) +
                "&ip=" + hostport[0] +
                "&port=" + Integer.toString(port);
    }

    @Override
    public void destroy() throws Exception {
        logger.info("Destroy");
        if (service != null) {
            service.shutdown();
            service = null;
        }
    }

    public void addAbonadoRemoto(Address in_user, Address pbx){
        AbonadoRemoto abonadoRemoto = new AbonadoRemoto(in_user, pbx);
        remotos.put(((AddressImpl)in_user).getUserAtHostPort().substring(0, ((AddressImpl)in_user).getUserAtHostPort().indexOf("@")), abonadoRemoto);
    }
    public void remAbonadoRemoto(Address in_user){
        String key = ((AddressImpl)in_user).getUserAtHostPort().substring(0, ((AddressImpl)in_user).getUserAtHostPort().indexOf("@"));
        AbonadoRemoto out_user = remotos.get(key);
        if(out_user!=null) {
            remotos.remove(key);
        }
    }
    public int getAbonadosRemotos()
    {
        return remotos.size();
    }

    //region abonados
    public AbonadoRemoto getAbonadoRemoto(Address in_user){
        AbonadoRemoto out_user = remotos.get(((AddressImpl)in_user).getUserAtHostPort().substring(0, ((AddressImpl)in_user).getUserAtHostPort().indexOf("@")));
        if(out_user!=null) {
            return out_user;
        }
        return null;
    }
    public boolean isAbonadoRemoto(Address in_user){
        AbonadoRemoto out_user = remotos.get(((AddressImpl)in_user).getUserAtHostPort().substring(0, ((AddressImpl)in_user).getUserAtHostPort().indexOf("@")));
        if(out_user!=null) {
            return true;
        }
        return false;
    }

    public synchronized List<String> listAbonadosRemotos(){
        List<String> a = new ArrayList<>();
        for (Map.Entry<String, AbonadoRemoto> e : remotos.entrySet()) {
            a.add(((AddressImpl)((AbonadoRemoto)e.getValue()).getExtAddress()).getUserAtHostPort());
        }
        return a;
    }

    private class ScheduledUpdateRequester implements Runnable {

        private UdpPacketConsumer consumer;

        public ScheduledUpdateRequester(UdpPacketConsumer consumer) {
            super();
            this.consumer = consumer;
        }

        @Override
        public void run() {
            logger.debug("Run!");
            for (UdpClientConfig cfg : mite1xs) {
                try {
                    UdpClient udpClient = new UdpClient(cfg);
                    for (String abonado : routeManager.listAbonados()) {
                        try {
                            udpClient.send(buildExtensionUpdateCommand(abonado, "", 60), consumer);
                        } catch (Exception e) {
                            logger.warn(e);
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error registrando en Mite1x " + cfg.getAddress(), e);
                }
            }
        }

        @Override
        protected void finalize() throws Throwable {
            logger.info("finalize");
        }
    }

    public void sendAbonado(String abonado, boolean registrado){
        for (UdpClientConfig cfg : mite1xs) {
            try {
                UdpClient udpClient = new UdpClient(cfg);
                    try {
                        udpClient.send(buildExtensionUpdateCommand(abonado, "", registrado?expires:0), this);
                    } catch (Exception e) {
                        logger.warn(e);
                    }
            } catch (Exception e) {
                logger.error("Error registrando en Mite1x " + cfg.getAddress(), e);
            }
        }
    }
    public synchronized void setRouteManager(RouteManager routeManager){
        this.routeManager = routeManager;
    }
}