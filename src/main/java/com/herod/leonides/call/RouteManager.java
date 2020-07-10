package com.herod.leonides.call;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.herod.rtp.RtpCodecException;
import com.herod.rtp.RtpCodec;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.Route;
import gov.nist.javax.sip.header.RouteList;
import gov.nist.javax.sip.header.To;
import com.herod.leonides.udp.ClusterManager;
import com.herod.leonides.utils.AbonadoRemoto;
import com.herod.rtp.RtpMime;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sip.SipException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.ToHeader;
import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by eugenio.voss on 26/1/2017.
 * Clase que controla el ruteo a los distintos servers a los que se conecta Karen.
 * Controla tambien users, WhiteList y BlackList.
 * Crear RouteManagerTest para probar esto.
 */
public class RouteManager {
    /*Rutas en Mitrol
    1: 0.0.0.0          ->localhost
    2: 172.30.41.230    ->DNS
    3: 192.168.41.227   ->desarrollo
    4: 192.168.41.245   ->MiteKowal
    5: 192.168.41.161   ->Kamailio
    6: 192.168.41.230   ->produccion

https://stackoverflow.com/questions/22856279/call-external-javascript-functions-from-java-code
 Use ScriptEngine.eval(java.io.Reader) to read the script

ScriptEngineManager manager = new ScriptEngineManager();
ScriptEngine engine = manager.getEngineByName("JavaScript");
// read script file
engine.eval(Files.newBufferedReader(Paths.get("C:/Scripts/Jsfunctions.js"), StandardCharsets.UTF_8));

Invocable inv = (Invocable) engine;
// call function from script file
inv.invokeFunction("yourFunction", "param");

    */
    public static final int LOCAL_PORT = 5060;

    String routeListCfg = "routelist.properties";

    //private List<RtpCodec> codecs = new ArrayList<RtpCodec>();
    private Map<String, RtpCodec> codecs = new HashMap<>();

    HashMap<RouteCall, Route> routes = new HashMap<>();

    HashMap<String, AddressImpl> users = new HashMap<>();//usuarios registrados

    @Autowired
    ClusterManager clusterManager;//users remotos

    RouteList whiteList = new RouteList();
    RouteList blackList = new RouteList();

    public RouteManager(){
        clusterManager = null;
    }
/*
    private void initCodecs(String codecCfg) throws IOException {
        InputStream inputStream = null;
        inputStream = this.getClass().getResourceAsStream("/" + codecCfg);

        if(inputStream==null)
            inputStream = new FileInputStream(codecCfg);//whiteProperties

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line="";

        while((line = reader.readLine())!=null)
        {
                String[] params = line.split(",");
                codecs.add(new RtpCodec(RtpMime.getByName(params[0]), Integer.parseInt(params[1]), Integer.parseInt(params[2])));
        };
        reader.close();
        inputStream.close();
    }*/

    private void initCodecs(JsonArray jsonArray) throws IOException, RtpCodecException {
        if (jsonArray == null)
            return;
        Iterator<JsonElement> iterator = jsonArray.iterator();
        while(iterator.hasNext())
        {
            JsonObject codec = iterator.next().getAsJsonObject();
            codecs.put(codec.get("id").getAsString(), new RtpCodec(RtpMime.getByName(codec.get("name").getAsString()), codec.get("media").getAsInt(), codec.get("freq").getAsInt()));
        };
    }

    public void addCodec(String id, String c) throws RtpCodecException {
        RtpMime mime = RtpMime.getByName(c);
        int codec = -1;
        switch(c){
            case "PCMU":
                codec = 0;
                break;
            case "PCMA":
                codec = 8;
                break;
            case "G729":
                codec = 18;
                break;
            case "OPUS":
                codec = 96;
                break;
        }
        codecs.put(id, new RtpCodec(mime, codec, 8000));
    }
    public void init(JsonObject jsonObject) throws IOException, RtpCodecException {
        initCodecs(jsonObject.get("codecs").getAsJsonArray());

        addWhiteList(jsonObject.get("whitelist").getAsJsonArray());
        addBlackList(jsonObject.get("blacklist").getAsJsonArray());
        addRouteList(jsonObject.get("routes").getAsJsonArray(), jsonObject.get("SipServers").getAsJsonArray());
    }

    void addWhiteList(JsonArray jsonArray) throws IOException {

        if (jsonArray == null)
            return;
        Iterator<JsonElement> iterator = jsonArray.iterator();
        while(iterator.hasNext())
        {
            SipUri localUri = new SipUri();
            try {
                localUri.setHost(iterator.next().getAsString());
                AddressImpl local = new AddressImpl();
                local.setAddess(localUri);
                whiteList.add(new Route(local));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        };
    }

    void addBlackList(JsonArray jsonArray) throws IOException {

        if (jsonArray == null)
            return;
        Iterator<JsonElement> iterator = jsonArray.iterator();
        while(iterator.hasNext())
        {
            SipUri localUri = new SipUri();
            try {
                localUri.setHost(iterator.next().getAsString());
                AddressImpl local = new AddressImpl();
                local.setAddess(localUri);
                blackList.add(new Route(local));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        };
    }

    void addWhiteList(String cfg) throws IOException {
        InputStream inputStream = null;
        //    String whiteProperties = System.getProperty(cfg);
        //    if (whiteProperties == null)
                inputStream = this.getClass().getResourceAsStream("/" + cfg);
        //    else
        //        inputStream = new FileInputStream(whiteProperties);
        if(inputStream==null)
            inputStream = new FileInputStream(cfg);//whiteProperties
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line="";
            while((line = reader.readLine())!=null)
            {
                SipUri localUri = new SipUri();
                try {
                    localUri.setHost(line);
                    AddressImpl local = new AddressImpl();
                    local.setAddess(localUri);
                    whiteList.add(new Route(local));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            };
        reader.close();
        inputStream.close();
    }
    void addBlackList(String cfg) throws IOException {
        InputStream inputStream = null;
        //String blackProperties = System.getProperty(cfg);
        //if (blackProperties == null)
            inputStream = this.getClass().getResourceAsStream("/" + cfg);
        //else
            //inputStream = new FileInputStream(blackProperties);
        if(inputStream==null)
            inputStream = new FileInputStream(cfg);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line="";
        while((line = reader.readLine())!=null)
        {
            SipUri localUri = new SipUri();
            try {
                localUri.setHost(line);
                AddressImpl local = new AddressImpl();
                local.setAddess(localUri);
                blackList.add(new Route(local));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        };
        reader.close();
        inputStream.close();
    }

    public synchronized void addRouteList(JsonArray routesArray, JsonArray rapsArray) throws IOException {

        if (routesArray == null)
            return;
        Iterator<JsonElement> iterator = routesArray.iterator();
        while(iterator.hasNext())
        {
            try {
                JsonObject route = iterator.next().getAsJsonObject();
                String ifx = route.get("prefix").getAsString();
                String ofx = route.get("postfix").getAsString();
                String sRap = route.get("SipServer").getAsString();
                String isrv = (route.get("presrv") == null)?"ALL":route.get("presrv").getAsString();
                String osrv = (route.get("net") == null)?"DEFAULT":route.get("net").getAsString();

                Iterator<JsonElement> itRap = rapsArray.iterator();
                while(itRap.hasNext()) {
                    JsonObject rap = itRap.next().getAsJsonObject();
                    if(rap.get("id").getAsString().equals(sRap)) {
                        SipUri localUri = new SipUri();
                        localUri.setHost(rap.get("ip").getAsString());
                        localUri.setPort(rap.get("port").isJsonNull()?5060:rap.get("port").getAsInt());
                        AddressImpl local = new AddressImpl();
                        local.setAddess(localUri);
                        //routes.put(new RouteCall(i, o, rap.get("protocol").getAsString()), local);
                        RouteCall RouteCall = new RouteCall(routes.size()+1, ifx, osrv, ofx, rap.get("protocol").isJsonNull()?"UDP":rap.get("protocol").getAsString());
                        if(rap.has("codecs") && !rap.get("codecs").isJsonNull()){
                            Iterator<JsonElement> itCodec = rap.get("codecs").getAsJsonArray().iterator();
                            while(itCodec.hasNext()) {
                                String codec = itCodec.next().getAsString();
                                if(codec.equals("ALL"))
                                    RouteCall.addAllCodecs(codecs);
                                else
                                    RouteCall.addCodec(codecs.get(codec));
                            }
                        }
                        else {
                            //ALL
                            RouteCall.addAllCodecs(codecs);
                        }
                        routes.put(RouteCall, new Route(local));
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void addRouteList(String cfg) throws IOException {
        InputStream inputStream = null;
        //    String routeProperties = System.getProperty(cfg);
        //    if (routeProperties == null)
                inputStream = this.getClass().getResourceAsStream("/" + cfg);
        //    else
                //inputStream = new FileInputStream(routeProperties);
        if(inputStream==null)
                inputStream = new FileInputStream(cfg);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line="";
            while((line = reader.readLine())!=null)
            {
                SipUri localUri = new SipUri();
                try {
                    //String []params = line.split(",");
                    //addRoute(params[0], params[1], params[2], params.length>3?Integer.parseInt(params[3]):LOCAL_PORT, params.length>4?params[4]:"UDP");
                    addRoute(line);
                    //routes.add(new Route(local));
                } catch (ParseException | RtpCodecException e) {
                    e.printStackTrace();
                }
            };
        reader.close();
        inputStream.close();
    }
    //region routes
    public void addRoute(String ruta) throws ParseException, RtpCodecException {
        String[]params = ruta.split(",");
        SipUri localUri = new SipUri();
        localUri.setHost(params[2]);
        localUri.setPort(params.length>3? Integer.parseInt(params[3]):LOCAL_PORT);
        AddressImpl local = new AddressImpl();
        local.setAddess(localUri);
        //routes.put(params[0] + ";" + params[1] + ";" + (params.length>4?params[4]:"UDP"), new Route(local));
        RouteCall RouteCall = new RouteCall(routes.size()+1, params[0], "DEFAULT", params[1], params.length>4?params[4]:"UDP");
        if(params.length>5){
            int  p=5;
            while(p<params.length)
            {
                if(params[p].contentEquals("ALL"))
                {
                    RouteCall.addAllCodecs(codecs);
                }
                else
                    RouteCall.addCodec(params[p++]);
            }
        }
        else {
            //ALL
            RouteCall.addAllCodecs(codecs);
        }
        routes.put(RouteCall, new Route(local));
    }
    /*
    public void addRoute(String inExtension, String outExtension, String host, int port, String transport) throws ParseException {
        SipUri localUri = new SipUri();
        localUri.setHost(host);
        localUri.setPort(port);
        AddressImpl local = new AddressImpl();
        local.setAddress(localUri);
        routes.put(inExtension + ";" + outExtension + ";" + transport, new Route(local));
    }*/
    /*
    * route: ingresa la direccion de llamada y devuelve la direccion de salida de acuerdo a la extension.
    * Cambia el inExtension por el outExtension.
    * Previamente debe chequear si la extension pertenece a un abonado, si esta en una WhiteList o en una BlackList.
    * */

    public synchronized ToHeader route(Address toAddr) throws ParseException {
        Address out_addr = getUser(toAddr);
        if (out_addr != null) {
            //es abonado
            AddressImpl to2 = new AddressImpl();
            SipUri uri = new SipUri();
            uri.setHost(((SipURI)out_addr.getURI()).getHost());
            uri.setPort(((SipURI)out_addr.getURI()).getPort());
            uri.setUser(((SipURI)out_addr.getURI()).getUser());
            to2.setURI(uri);
            To to = new To();
            to.setAddress(to2);
            String transport = ((SipURI)out_addr.getURI()).getTransportParam();
            if(transport==null)
                transport = "UDP";
            to.setParameter("transport", transport);

            return to;
        }

        if (clusterManager != null){
            //por hora todos UDP
            AbonadoRemoto remoto = clusterManager.getAbonadoRemoto(toAddr);
            if(remoto!=null) {
                out_addr = remoto.getPbxAddress();
                if (out_addr != null) {
                    //es abonado
                    To to = new To();
                    to.setAddress(out_addr);
                    to.setParameter("transport", "UDP");
                    return to;
                }
            }
        }
        String[] destino = ((AddressImpl)toAddr).getUserAtHostPort().split("@");

        for (Map.Entry<RouteCall, Route> e : routes.entrySet()) {
            //String []prefijos = e.getKey().split(";");
            //if(destino[0].startsWith(prefijos[0]))
            if(destino[0].startsWith(e.getKey().getInExtension()))
            {
                Route nueva_ruta = e.getValue();
                out_addr = (AddressImpl)nueva_ruta.getAddress().clone();
                //((AddressImpl)out_addr).setUser(destino[0].replaceFirst(prefijos[0], prefijos[1]));
                ((AddressImpl)out_addr).setUser(destino[0].replaceFirst(e.getKey().getInExtension(), e.getKey().getOutExtension()));

                To to = new To();
                to.setAddress(out_addr);
                //to.setParameter("net", e.getKey().getOutsrv());
                to.setParameter("transport", e.getKey().getTransport());

                return to;
            }
        }
        return null;
    }
    public synchronized String getOutSrv(AddressImpl addr)
    {
        for (Map.Entry<RouteCall, Route> e : routes.entrySet()) {
            Route nueva_ruta = e.getValue();
            AddressImpl out_addr = (AddressImpl)nueva_ruta.getAddress().clone();
            if(addr.getHost().equals(out_addr.getHost()))
            {
                RouteCall rc = (RouteCall)e.getKey();
                return rc.getOutsrv();
            }
        }
        return null;
    }
    public synchronized List<RtpCodec> getCodecs(Address toAddr)
    {
        if(toAddr==null) {
            List<RtpCodec> c= new ArrayList<RtpCodec>();
            for(Map.Entry<String, RtpCodec> entry : codecs.entrySet())
            {
                c.add(entry.getValue());
            }
            return c;
        }
        String[] destino = ((AddressImpl)toAddr).getUserAtHostPort().split("@");
        for (Map.Entry<RouteCall, Route> e : routes.entrySet()) {
            //String []prefijos = e.getKey().split(";");
            //if(destino[0].startsWith(prefijos[0]))
            if(destino[0].startsWith(e.getKey().getInExtension()))
            {
                return e.getKey().getCodecs();
            }
        }
        return null;
    }
    public synchronized List<RtpCodec>getCodecs()
    {
        List<RtpCodec> c= new ArrayList<RtpCodec>();
        for(Map.Entry<String, RtpCodec> entry : codecs.entrySet())
        {
            c.add(entry.getValue());
        }
        return c;
    }
    //region users
    public synchronized Address getUser(Address in_user){
        //NameValue nameValue= new NameValue();
        //nameValue.setName(((AddressImpl)in_user).getUserAtHostPort().substring(0, ((AddressImpl)in_user).getUserAtHostPort().indexOf("@")));
        //nameValue.setValue(transport);
        //Address out_user = users.get(nameValue);

        Address out_user = users.get(((AddressImpl)in_user).getUserAtHostPort().substring(0, ((AddressImpl)in_user).getUserAtHostPort().indexOf("@")));
        if(out_user!=null) {
            return out_user;
        }
        return null;
    }
    public synchronized boolean isAbonado(Address in_user){
        //NameValue nameValue= new NameValue();
        //nameValue.setName(((AddressImpl)in_user).getUserAtHostPort().substring(0, ((AddressImpl)in_user).getUserAtHostPort().indexOf("@")));
        //nameValue.setValue(transport);
        //Address out_user = users.get(nameValue);

        Address out_user = users.get(((AddressImpl)in_user).getUserAtHostPort().substring(0, ((AddressImpl)in_user).getUserAtHostPort().indexOf("@")));
        if(out_user!=null) {
            return true;
        }
        return false;
    }
    public synchronized void addAbonado(Address in_user, String transport){
        //NameValue nameValue= new NameValue();
        //nameValue.setName(((AddressImpl)in_user).getUserAtHostPort().substring(0, ((AddressImpl)in_user).getUserAtHostPort().indexOf("@")));
        //nameValue.setValue(transport);
        //users.put(nameValue, in_user);
        users.put(((AddressImpl)in_user).getUserAtHostPort().substring(0, ((AddressImpl)in_user).getUserAtHostPort().indexOf("@")), (AddressImpl) in_user);
        if(clusterManager!=null)
            clusterManager.sendAbonado(((AddressImpl) in_user).getUserAtHostPort(), true);
    }
    public synchronized void remAbonado(Address in_user){
        String key = ((AddressImpl)in_user).getUserAtHostPort().substring(0, ((AddressImpl)in_user).getUserAtHostPort().indexOf("@"));
        //NameValue nameValue = new NameValue();
        //nameValue.setName(key);
        //nameValue.setValue(transport);
        //Address out_user = users.get(nameValue);
        Address out_user = users.get(key);
        if(out_user!=null) {
            //users.remove(nameValue);
            users.remove(key);
            if(clusterManager!=null)
                clusterManager.sendAbonado(((AddressImpl) out_user).getUserAtHostPort(), false);
        }
    }
    public synchronized int getAbonadosCount()
    {
        return users.size();
    }
    //region cluster
    public void setClusterManager(ClusterManager clusterManager){
        this.clusterManager = clusterManager;
    }

    public List<String> listAbonados(){
        List<String> a = new ArrayList<>();
        for (Map.Entry<String, AddressImpl> e : users.entrySet()) {
            a.add(((AddressImpl)e.getValue()).getUserAtHostPort());// + ";" + e.getKey().getValue());
        }
        return a;
    }
    public List<RouteCall> listRoutes(){
        List<RouteCall> a = new ArrayList<>();
        for (Map.Entry<RouteCall, Route> e : routes.entrySet()) {
            a.add((RouteCall)e.getKey());// + ";" + e.getKey().getValue());
        }
        return a;
    }
    public synchronized void checkCallEnabled(Address in_user)throws SipException{
        //chequeo si esta habilitado para realizar llamadas
        if(isAbonado(in_user))
            return;
        //chequeo whitelist y blacklist
        //mediaMaps = rtpMaps.stream().filter(x -> !x.equals(dtmfMap)).collect(Collectors.toList());
        List<Route> black = blackList.stream().filter(x -> ((AddressImpl)x.getAddress()).getHost().equals(((AddressImpl)in_user).getHost())).collect(Collectors.toList());
        if(black!=null && black.size()>0)
            throw new SipException("FORBIDDEN. BlackList Address");

        //List<Route> white = whiteList.stream().filter(x -> x.equals(in_user)).collect(Collectors.toList());
        List<Route> white = whiteList.stream().filter(x -> ((AddressImpl)x.getAddress()).getHost().equals(((AddressImpl)in_user).getHost())).collect(Collectors.toList());
        if(white!=null && white.size()>0)
            return;

        //throw new SipException("FORBIDDEN. Unknown Address");
    }

    public String getRouteList()
    {
        return routeListCfg;
    }

    public List<String> getWhiteList()
    {
        return whiteList.getHeadersAsEncodedStrings();
    }
    public List<String> getBlackList()
    {
        return blackList.getHeadersAsEncodedStrings();
    }
}
