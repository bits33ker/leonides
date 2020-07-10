package com.herod.leonides;

import com.google.gson.JsonObject;
import com.herod.leonides.config.License;
import com.herod.rtp.RtpDtmf;
import com.herod.rtp.interfaces.IRtpCall;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import com.herod.leonides.call.RouteManager;
import com.herod.sip.SipCallManager;
import com.herod.sip.SipInterface;
import com.herod.sip.SipServer;
import com.herod.sip.call.*;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sdp.SdpException;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.*;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.*;

/**
 * Created by eugenio.voss on 31/5/2017.
 */
public class LeonidesServer {
/*
    TODO:
    API:
    crear u pdate en la rest-api
    Agregar swagger para Documentacion.
    Control de Acceso por SWT. Modificar API para usar Token.
    Agregar Permisos.
    FrontEnd:
    Front en React con cliente de la api
    Softswitch:
    Copiar el sdp, hoy lo esta modificando. Mantener esta funcionalidad para B2B.
    MediaServer:
    Crear una springapp para RTP Media Server.
    Usar Kurento u otro media server.
    IVR:
    usar jvoicexml.
    https://css-tricks.com/model-based-testing-in-react-with-state-machines/
    como hacer Maquinas de Estado con React.
    BackEnd:
    Agregar TLS con certificado.
    Crear la UserManager para separar Abonados.
    Nombres en Ingles.
    Ver xq no anda con la VPN. Los Pads andan!
    Registracion con Challenge a otras centrales.
    Realizacion de llamadas con Challenge(Auth-Proxy) a otras centrales.
 */

    CallStatusContainer calls = new CallStatusContainer();

    Object syncCallStates = new Object();
    ArrayList<CallStats> callStatses = new ArrayList<CallStats>();//guarda los resultados de la comunicacion.

    private Map<SipInterface, SipCallManager> sipCallManagerMap = new HashMap<>();
    //SipCallManager sipCallManager;
    //SipInterface sipInterface;

    private License licencia = new License();

    @Autowired
    private RouteManager routeManager;

    public static final String sContentType = "Content-Type";
    //endregion

    private List<RtpDtmf> dtmfSignals;
    private RtpDtmf dtmfMap;
    private boolean allCodecs = true;

    //private Properties karenProperties = null;
    JsonObject karenJson = null;
    private Properties sipStackProperties = null;

    //region Attributes
    private final Logger logger = Logger.getLogger(LeonidesServer.class.getName());
    private boolean autoAnswer = false;
    private boolean delayOfferEnabled;

    //@Autowired
    //private LicenseManager licenseManager;

    public LeonidesServer(){

    }
/*
    public void init(Properties properties, Properties sipProperties, List<DtmfSignal> dtmfSignals) throws Exception {
        karenProperties = properties;
        sipStackProperties = sipProperties;
        this.dtmfSignals = dtmfSignals;
    }
*/
    public void init(JsonObject jsonObject, Properties sipProperties, List<RtpDtmf> dtmfSignals) throws Exception {
        karenJson = jsonObject;
        sipStackProperties = sipProperties;
        this.dtmfSignals = dtmfSignals;
    }


    public synchronized int getCalls(){
        return calls.size();
    }

    public synchronized Call getCall(Message msg){
        return calls.getCall(msg);
    }
    public synchronized Call getCall(String callId){
        return calls.getCall(callId);
    }
    public synchronized void removeCall(Call call){
        calls.removeCall(call);
    }
    public synchronized void addCall(Call call){
        calls.addCall(call);
    }
    public void addCallStats(CallStats callstats)
    {
        synchronized (syncCallStates) {
            callStatses.add(callstats);
        }
    }
    public synchronized void addSipCallManager(SipInterface sipInterface, SipCallManager scm)
    {
        sipCallManagerMap.put(sipInterface, scm);
        //this.sipCallManager = sipCallManager;
        //this.sipInterface = sipInterface;
    }
    public List<SipInterface> getSipInterfaces()
    {
        //return sipInterface;
        List<SipInterface> list = new ArrayList<>();

        for (Map.Entry k:sipCallManagerMap.entrySet()) {
            list.add((SipInterface) k.getKey());
        }

        return list;
    }
    /*
    public synchronized void setLicencia(KarenLicense lic)
    {
        licencia = lic;
    }
*/
    public synchronized License getLicencia() {
        return licencia;
    }
    public void setRouterManager(RouteManager routerManager)
    {
        this.routeManager = routerManager;
    }

    public synchronized void remAbonado(Address abonado)
    {
        routeManager.remAbonado(abonado);
    }
    public synchronized void addAbonado(Address abonado, String transport) throws SipException {
        routeManager.checkCallEnabled(abonado);
        if(routeManager.getAbonadosCount()>=getLicencia().getMaxAbonados())
            throw new SipException("Users Exceeded");
        routeManager.addAbonado(abonado, transport);
    }
    public synchronized boolean isUser(Address user){
        if(routeManager.getUser(user)!=null)
            return true;
        return false;
    }
    private <T extends Message> void log(T message) {
        String callId = ((CallIdHeader) message.getHeader(CallIdHeader.NAME)).getCallId();
        String method = message instanceof SIPRequest ? ((SIPRequest) message).getMethod() : ((SIPResponse) message).getReasonPhrase();
        String from = message instanceof SIPRequest ? ((SIPRequest) message).getFrom().getAddress().toString() : ((SIPResponse) message).getFrom().getAddress().toString();
        String to = message instanceof SIPRequest ? ((SIPRequest) message).getTo().getAddress().toString() : ((SIPResponse) message).getTo().getAddress().toString();
        String format = String.format("CallId: %s, From: %s, To: %s", callId, from, to);
        logger.info(method + " " +  format);
        logger.fine(message.toString());
    }

    public SipServer selectSipServer(Address addr) throws SipException {

        String outsrv = routeManager.getOutSrv((AddressImpl)addr);
        for (Map.Entry k:sipCallManagerMap.entrySet()) {
            SipInterface s = (SipInterface) k.getKey();
            if((s.getIp().equals(outsrv)))
                return ((SipCallManager)k.getValue()).getServer();
        }
        throw new SipException("selectSipServer exception");
     }
/*
     public SipServer getSipServer()
     {
         return sipCallManager.getServer();
     }
*/
    public static String getTransport(ToHeader to)
    {
        String transport = to.getParameter("transport");
        if(transport==null)
            transport = ((SipURI)to.getAddress().getURI()).getParameter("transport");
        if(transport==null)
            transport = "UDP";
        return transport;
    }

    //public synchronized Properties getKarenProperties() {
    //    return karenProperties;
    //}
    public synchronized JsonObject getKarenJson(){
        return karenJson;
    }

    //public synchronized void setKarenProperties(Properties prop) {
    //    karenProperties = prop;
    //}
    public synchronized void setKarenJson(JsonObject json){
        karenJson = json;
    }
    
    public synchronized Properties getSipStackProperties(){
    	return sipStackProperties;
    }
/*
    public synchronized void setKarenProperties(Map<String, String> prop) {
    	sipStackProperties = new Properties();
    	sipStackProperties.putAll(prop);
    }

    public synchronized void setSipStackProperties(Map<String, String> prop) {
        karenProperties = new Properties();
        karenProperties.putAll(prop);
    }
*/
    //RE-INVITE
    public void sendCall(FromHeader from,
                         ToHeader to,
                         byte[] sdpbytes,
                         String callId)
            throws SipException, ParseException, InvalidArgumentException, SdpException, IOException {
        int expires = 60;
        boolean delayOffer = false;

        SipServer outServer = this.selectSipServer(to.getAddress());

        //chequeo si esta registrado a mi .getURI().toString()
        SIPRequest request = (SIPRequest) outServer.buildRequest(Request.INVITE, null, callId, from, to);

        if(!delayOffer) {
            ContentTypeHeader contentType = new ContentType();
            contentType.setContentType("application");
            contentType.setContentSubType("sdp");
            //contentType.setContentType(Call.mediaContentType + "\r\n");
            request.setContent(sdpbytes, contentType);
        }

        //Agregamos también el header del Contact.
        //addContactHeader(request, from);
        // Al se un register, agregamos el tiempo de expiración de la registración.
        outServer.addExpiration(request, expires);
        outServer.sendRequest(request);
        logger.fine("SEND REINVITE " + request);
    }

    ToHeader route(Address toAddr) throws ParseException, RouteException {
        //chequeo si esta registrado a mi .getURI().toString()
        ToHeader to = routeManager.route(toAddr);
        if(to==null)
            throw new RouteException("ToHeader ERROR: " + to.toString());

        return to;
    }
    /*

    String setIpInterface(Address addr) throws SipException {
        for (Map.Entry k: sipCallManagerMap.entrySet() ) {
            SipInterface sipInterface = (SipInterface)k.getKey();
            String transport = ((SipURI)addr.getURI()).getTransportParam();
            if(transport==null) {
                if(transport==null)
                    transport = "UDP";
            }
            if(sipInterface.getIp().equals(((SipURI)addr.getURI()).getHost())
                    && transport.equals(sipInterface.getProtocol()))
                return sipInterface.getNet();
        }
        throw new SipException("No Interface");
    }
*/
    public synchronized SIPMessage sendInvite(
            Call call,
            SIPMessage request
    ) throws ParseException, SdpException, IOException, SipException, InvalidArgumentException, RouteException {
        //int expires = -1;

        CallEndPoint firstParty = call.getInbound();

        //chequeo si esta registrado a mi .getURI().toString()
        ToHeader to = route(firstParty.getTo().getAddress());

        if(request.getTo().getTag()!=null)
            to.setTag(request.getTo().getTag());

        SipServer outServer = null;
        if(!routeManager.isAbonado(to.getAddress()))
            outServer = this.selectSipServer(to.getAddress());
        else
            outServer = firstParty.getServer();

        routeManager.checkCallEnabled(request.getFrom().getAddress());
        SipUri uri = new SipUri();
        uri.setHost(outServer.getHost());
        uri.setPort(outServer.getServerPort());
        From from = CallEndPoint.cloneFrom(request.getFrom().getAddress(), null, uri);

        //obtengo session description de la inbound
        Optional<String> sessionDescription = call.getRtpCall().getSessionDescription();

        SIPRequest invite = (SIPRequest) outServer.buildRequest(Request.INVITE, request, outServer.updateCallId(((CallIdHeader) request.getHeader(CallIdHeader.NAME)).getCallId()), from, to);
        if(!sessionDescription.get().isEmpty()) {
            ContentType contentType = request.getContentTypeHeader();
            invite.setContent(sessionDescription.get(), contentType);
        }

        CallEndPoint dest = call.add((SIPRequest)invite, false);
        dest.setSupported(firstParty.getSupported());
        dest.setServer(outServer);
        //this.addCall(call);
        // This session will be used to send BYE
        request.setHeaders(firstParty.getSupported());
        logger.fine("SEND IVITE\n" + invite);
        outServer.sendRequest(invite);
        return invite;
    }
    public synchronized void processCancel(RequestEvent requestEvent) throws InvalidArgumentException, SipException, SdpException, ParseException, UnknownHostException {
        String callId = ((CallIdHeader) requestEvent.getRequest().getHeader(CallIdHeader.NAME)).getCallId();
        Call call = getCall(requestEvent.getRequest());
        if(call!=null){
            CallEndPoint firstParty = call.getInbound();
            CallEndPoint secondParty = call.getOutbound();

            if (firstParty.getCallId().equals(callId)) {
                SipServer outServer = secondParty.getServer();
                outServer.sendCancel(secondParty);
                removeCall(call);
                addCallStats(call.getCallStats());
            }
            if (secondParty.getCallId().equals(callId)) {
                SipServer outServer = firstParty.getServer();
                outServer.sendCancel(firstParty);
                removeCall(call);
                addCallStats(call.getCallStats());
            }
            //logger.info(call.getCallStats().toString());
        }
    }
    public synchronized void processBye(RequestEvent requestEvent) throws ParseException, SipException, UnknownHostException, InvalidArgumentException {
        Call call = getCall(requestEvent.getRequest());
        String callId = ((CallIdHeader) requestEvent.getRequest().getHeader(CallIdHeader.NAME)).getCallId();

        CallEndPoint firstParty = call.getInbound();
        CallEndPoint secondParty = call.getOutbound();

        if (firstParty.getCallId().equals(callId)) {
            AddressImpl address = new AddressImpl();
            address.setAddressType(1);
            SipURI uri =new SipUri();
            address.setAddess(uri);
            uri.setHost(((To) secondParty.getTo()).getHostPort().getHost().getIpAddress());
            uri.setPort(((To) secondParty.getTo()).getHostPort().getPort());
            uri.setUser(((To)secondParty.getTo()).getUserAtHostPort().substring(0, ((To)secondParty.getTo()).getUserAtHostPort().toString().indexOf('@')));

            ToHeader toHeader = CallEndPoint.cloneTo(address, secondParty.getTo().getTag());

            SipServer outServer = secondParty.getServer();
            Request request = outServer.buildRequest( Request.BYE, (SIPRequest)requestEvent.getRequest(), secondParty.getCallId(), secondParty.getRequest().getFrom(), toHeader);
            //request.removeLast(sVia);
            log(request);
            outServer.sendRequest(request);
            return;
        }
        if (secondParty.getCallId().equals(callId)) {

            AddressImpl address = new AddressImpl();
            address.setAddressType(1);
            SipURI uri =new SipUri();
            address.setAddess(uri);
            uri.setHost(((From) firstParty.getFrom()).getHostPort().getHost().getIpAddress());
            uri.setPort(((From) firstParty.getFrom()).getHostPort().getPort());
            uri.setUser(((From) firstParty.getFrom()).getUserAtHostPort().substring(0, ((From) firstParty.getFrom()).getUserAtHostPort().indexOf('@')));

            ToHeader toHeader = CallEndPoint.cloneTo(address, firstParty.getFrom().getTag());
            FromHeader from = new From();
            AddressImpl toAddr = (AddressImpl) firstParty.getTo().getAddress().clone();
            from.setAddress(toAddr);

            if(firstParty.getTo().getTag()!=null)
                from.setTag(firstParty.getTo().getTag());

            SipServer outServer = firstParty.getServer();

            Request request = outServer.buildRequest( Request.BYE, (SIPRequest)requestEvent.getRequest(), firstParty.getCallId(), from, toHeader);
            log(request);
            outServer.sendRequest(request);
            return;
        }
    }
    /*
    public synchronized void processPrack(RequestEvent requestEvent) throws ParseException, SdpException, IOException, SipException, InvalidArgumentException, RtpException {
        SIPRequest request = (SIPRequest) requestEvent.getRequest();
        Call call = getCall(request);
        CallEndPoint firstParty = call.getInbound();
        CallEndPoint secondParty = call.getOutbound();

        String callId = ((CallIdHeader) request.getHeader(CallIdHeader.NAME)).getCallId();

        ContentType contentType = request.getContentTypeHeader();
        if (contentType != null && contentType.compareMediaRange(Call.mediaContentType)==0 && request.getContentLength().getContentLength() > 0) {
            call.setResponseMedia(request);
        }


        if (firstParty.getCallId().equals(callId)) {
            SipServer outServer = this.getSipServer();
            outServer.sendPRACK(secondParty);

            call.setStatus(CallStatus.answered);
            //call.startRtp();
            return;
        }
        if (secondParty.getCallId().equals(callId)) {
            SipServer outServer = this.getSipServer();
            outServer.sendPRACK(firstParty);

            call.setStatus(CallStatus.answered);
            //call.startRtp();
            return;
        }
    }
    public void createBlindRefer(RequestEvent requestEvent, ReferredBy referredBy) throws ParseException, SipException, InvalidArgumentException, IOException, SdpException {
        String callId = ((CallIdHeader) requestEvent.getRequest().getHeader(CallIdHeader.NAME)).getCallId();
        Call call = getCall(requestEvent.getRequest());

        CallEndPoint firstParty = call.getInbound();
        CallEndPoint secondParty = call.getOutbound();
        ReferTo referTo = (ReferTo) requestEvent.getRequest().getHeader(ReferTo.NAME);
        if (firstParty.getRequest().getCallId().getCallId().equals(callId)) {

            Contact contact = ((SIPRequest) requestEvent.getRequest()).getContactHeader();

            //TODO. Debo pasar al nuevo Call los datos del ContentType del primer Call. Probar. No se escucha en un sentido. Si en el otro.
            //firstParty.createSessionDescription();
            //call.stopRtp();
            Call referCall = this.referCall(secondParty, (AddressImpl) secondParty.getTo().getAddress(), secondParty.getTo().getTag(),//(AddressImpl)((SIPRequest) requestEvent.getRequest()).getTo().getAddress().clone(),
                    (AddressImpl) referTo.getAddress());
            if (referCall == null) {
                logger.info("Refer " + requestEvent.getRequest().getMethod() + " from " + contact + " to " + referTo.getAddress() + " NOT FOUND");
                throw new SipException("ERROR. ReferCall no found");
            }
            referCall.setReferredBy(call);
            call.setReferTo(referCall);

            return;
        }
        if (secondParty.getRequest().getCallId().getCallId().equals(callId)) {
            //FIXME. Cuando transfiere el secondparty no funca. Salta por excepcion cuando quiere abrir puerto.
            //if(routeManager.isAbonado(referTo.getAddress()))
            //{
            //es abonado mio.
            //le envio un invite.

            Contact contact = ((SIPRequest) requestEvent.getRequest()).getContactHeader();
            call.stopRtp();
            Call referCall = this.referCall(firstParty,
                    (AddressImpl) secondParty.getFrom().getAddress(),//(AddressImpl)((SIPRequest) requestEvent.getRequest()).getFrom().getAddress().clone()
                    firstParty.getFrom().getTag(),
                    (AddressImpl) referTo.getAddress());
            if (referCall == null) {
                logger.info("Refer " + requestEvent.getRequest().getMethod() + " from " + contact + " to " + referTo.getAddress() + " NOT FOUND");
                throw new SipException("ERROR. refer Call no found");
            }
            referCall.setReferredBy(call);
            call.setReferTo(referCall);
            //    return;
            //}
            return;
        }
    }

    public synchronized void createInteractRefer(RequestEvent requestEvent) throws ParseException, SipException, InvalidArgumentException, IOException, SdpException {
        String callId = ((CallIdHeader) requestEvent.getRequest().getHeader(CallIdHeader.NAME)).getCallId();
        Call call = getCall(requestEvent.getRequest());

        CallEndPoint firstParty = call.getInbound();
        CallEndPoint secondParty = call.getOutbound();
        ReferTo referTo = (ReferTo) requestEvent.getRequest().getHeader(ReferTo.NAME);
        ReferredBy referredBy = (ReferredBy) requestEvent.getRequest().getHeader(ReferredBy.NAME);

        String callIdReferTo = ((SipURI)referTo.getAddress().getURI()).getHeader(ReferTo.REPLACES);
        Call callReferTo = getCall(callIdReferTo);
        String[]paramReferTo = null;
        String tagReferTo = "";
        String tagReferFrom = "";
        if(callIdReferTo!=null) {
            //callIdReferTo = callIdReferTo.substring(0, callIdReferTo.indexOf("%3B"));
            paramReferTo = callIdReferTo.split("%3B");
            callIdReferTo = paramReferTo[0];
            //tagReferFrom = paramReferTo[1].split("%3D")[1];
            tagReferTo = paramReferTo[2].split("%3D")[1];
        }

        //Transferencia con Interaccion
        if (firstParty.getRequest().getCallId().getCallId().equals(callId)) {

            Contact contact = ((SIPRequest) requestEvent.getRequest()).getContactHeader();

            SipServer outServer = this.getSipServer();
            Call referCall = this.referCall(outServer, secondParty, (AddressImpl) secondParty.getTo().getAddress(), secondParty.getTo().getTag(), callReferTo, tagReferTo);
            if (referCall == null) {
                logger.info("Refer " + requestEvent.getRequest().getMethod() + " from " + contact + " to " + referTo.getAddress() + " NOT FOUND");
                throw new SipException("ERROR. Refer Call not found");
            }
            referCall.setReferredBy(call);
            call.setReferTo(referCall);

            return;
        }
        if (secondParty.getRequest().getCallId().getCallId().equals(callId)) {

            Contact contact = ((SIPRequest) requestEvent.getRequest()).getContactHeader();
            Call referCall = this.referCall(firstParty,
                    (AddressImpl) secondParty.getFrom().getAddress(),//(AddressImpl)((SIPRequest) requestEvent.getRequest()).getFrom().getAddress().clone()
                    firstParty.getFrom().getTag(),
                    (AddressImpl) referTo.getAddress());
            if (referCall == null) {
                logger.info("Refer " + requestEvent.getRequest().getMethod() + " from " + contact + " to " + referTo.getAddress() + " NOT FOUND");
                throw new SipException("ERROR. Refer Call not found");
            }
            referCall.setReferredBy(call);
            call.setReferTo(referCall);
            return;
        }
    }

    //referCall para Transferencia ciega
    private Call referCall(
            CallEndPoint party,
            AddressImpl fromAddr,
            String fromTag,
            AddressImpl toAddr
    ) throws ParseException, SdpException, IOException, SipException, InvalidArgumentException, RouteException {
        int expires = -1;

        //chequeo si esta registrado a mi .getURI().toString()
        ToHeader to = route(toAddr);

        SipServer outServer = this.getSipServer();

        routeManager.checkCallEnabled(fromAddr);
        FromHeader from = new From();
        if(from!=null)
        {
            SipUri uri= new SipUri();
            uri.setUser(((SipUri) fromAddr.getURI()).getUser());
            uri.setHost(outServer.getHost());
            uri.setPort(fromAddr.getPort());
            AddressImpl addr = new AddressImpl();//(AddressImpl)fromAddr.clone();
            addr.setURI(uri);
            from.setAddress(addr);
            if(fromTag!=null)
                from.setTag(fromTag);
        }
        else
            throw new SipException("Error creando INVITE");

        Call call = new Call(outServer.isRtcpEnabled(), outServer.isVideoEnabled());
        CallEndPoint orig = call.add(outServer, party, fromAddr, fromTag, toAddr);

        SIPRequest request = (SIPRequest) outServer.buildRequest( Request.INVITE, (SIPRequest)party.getRequest(), server.updateCallId(((CallIdHeader) party.getRequest().getHeader(CallIdHeader.NAME)).getCallId()), from, to);
        request.setCallId(outServer.createCallId());//cambio el call ID.

        ContentType contentType = new ContentType();
        if (contentType != null) {
            contentType.setContentType("application");
            contentType.setContentSubType("sdp");
            request.setContent(party.createSessionDescription(routeManager.getCodecs(null)), contentType);
        }

        CallEndPoint dest = call.add(server, (SIPRequest)request, false);
        dest.setSupported(party.getSupported());
        addCall(call);
        // This session will be used to send BYE
        request.setHeaders(party.getSupported());
        server.sendRequest(request);
        call.setStatus(CallStatus.ringing);
        return call;
    }

    //referCall para Transferencia con Interaccion
    private Call referCall(
            SipServer server,
            CallEndPoint party,
            AddressImpl fromAddr,
            String fromTag,
            Call callReferTo,
            String tagReferTo
    ) throws ParseException, SdpException, IOException, SipException, InvalidArgumentException {
        int expires = -1;

        //el tag del ReferTo es a quien hay que colgar. Hay que conectar a los otros 2 entre ellos.
        CallEndPoint inRefer = callReferTo.getInbound();
        AddressImpl toAddr = null;
        if(inRefer.getFrom().getTag().equals(tagReferTo))
            toAddr = (AddressImpl) inRefer.getTo().getAddress();
        if(inRefer.getTo().getTag().equals(tagReferTo))
            toAddr = (AddressImpl) inRefer.getFrom().getAddress();
        Call call = new Call(server.isRtcpEnabled(), server.isVideoEnabled());
        CallEndPoint orig = call.add(server, party, fromAddr, fromTag, toAddr);

        FromHeader from = new From();
        if(from!=null)
        {
            SipUri uri= new SipUri();
            uri.setUser(((SipUri) fromAddr.getURI()).getUser());
            uri.setHost(server.getHost());
            uri.setPort(fromAddr.getPort());
            AddressImpl addr = new AddressImpl();//(AddressImpl)fromAddr.clone();
            addr.setURI(uri);
            from.setAddress(addr);
            if(fromTag!=null)
                from.setTag(fromTag);
        }
        else
            throw new SipException("Error creando INVITE");

        if(callReferTo.getOutbound().getFrom().getTag().equals(tagReferTo + "@" + server.getHost()))
            toAddr = (AddressImpl) callReferTo.getOutbound().getTo().getAddress();
        if(callReferTo.getOutbound().getTo().getTag().equals(tagReferTo + "@" + server.getHost()))
            toAddr = (AddressImpl) callReferTo.getOutbound().getFrom().getAddress();

        CallEndPoint dest = call.add(server, party, (AddressImpl)from.getAddress(), fromTag, (AddressImpl)toAddr);
        call.setStatus(CallStatus.answered);
        return call;
    }
*/
    //Responses
    public void processTry(ResponseEvent responseEvent) throws Exception {
        Response resp = responseEvent.getResponse();
        //logger.info("Recieve Trying ");
        log(resp);

        Call call = getCall(resp);
        if(call==null) {
            logger.severe("No call for " + resp);
            throw new Exception("TRY ERROR. No call found");
        }
/*
        CallEndPoint firstParty = call.getInbound();
        CallEndPoint secondParty = call.getOutbound();

        String callId = ((CallIdHeader) resp.getHeader(CallIdHeader.NAME)).getCallId();
        if (firstParty.getCallId().equals(callId)) {
            SipServer outServer = this.getSipServer();

            String toTag = ((SIPResponse)resp).getTo().getTag();
                if(toTag!=null)firstParty.getTo().setTag(toTag);

                if(call.getReferredBy()==null) {
                    SIPResponse resp2 = (SIPResponse) outServer.buildResponse(Response.TRYING, secondParty.getRequest());
                    String tag = SipServer.createTag();
                    resp2.getTo().setTag(tag);
                    secondParty.getTo().setTag(tag);
                    outServer.sendResponse(resp2);
                }
                return;
        }
            if (secondParty.getCallId().equals(callId)) {
                SipServer outServer = this.getSipServer();

                String toTag = ((SIPResponse)resp).getTo().getTag();
                if(toTag!=null)secondParty.getTo().setTag(toTag);
            }

 */
    }
    public void processRing(ResponseEvent responseEvent) throws Exception {
        Response resp = responseEvent.getResponse();
        //logger.fine("RECIEVED " + resp);

        Call call = getCall(resp);
        if(call==null) {
            throw new Exception("RING ERROR. No call found");
        }

        CallEndPoint firstParty = call.getInbound();
        CallEndPoint secondParty = call.getOutbound();

            String callId = ((CallIdHeader) resp.getHeader(CallIdHeader.NAME)).getCallId();
            if(!call.isDelayOffer()) {
                if (firstParty.getCallId().equals(callId)) {
                    SipServer outServer = secondParty.getServer();

                    firstParty.setRequire(resp.getHeaders(SIPHeader.REQUIRE));
                    if(call.getReferredBy()==null) {
                        SIPResponse resp2 = (SIPResponse) outServer.buildResponse(Response.RINGING, secondParty.getRequest());
                        //String tag = SipServer.createTag();
                        resp2.getTo().setTag(secondParty.getTo().getTag());
                        logger.fine("SEND RING\n" + resp2);
                        outServer.sendResponse(resp2);
                    }
                    call.setStatus(CallStatus.ringing);
                    if (firstParty.require100rel()) {
                        //outServer.sendPRACK(firstParty);
                    }
                    return;
                }
                if (secondParty.getCallId().equals(callId)) {
                    //por aca no debiera entrar
                    SipServer outServer = firstParty.getServer();

                    secondParty.setRequire(resp.getHeaders(SIPHeader.REQUIRE));
                    if(call.getReferredBy()==null) {
                        SIPResponse resp2 = (SIPResponse) outServer.buildResponse(Response.RINGING, firstParty.getRequest());
                        resp2.getTo().setTag(firstParty.getTo().getTag());

                        //resp2.removeFirst(sVia);
                        logger.fine("SEND RING\n" + resp2);
                        outServer.sendResponse(resp2);
                    }
                    call.setStatus(CallStatus.ringing);
                    if(secondParty.require100rel())
                    {
                        //outServer.sendPRACK(secondParty);
                    }
                    return;
                }
            }
            else{//DelayOffer
                if (firstParty.getCallId().equals(callId)) {
                    SipServer outServer = secondParty.getServer();

                    SIPResponse resp2 = (SIPResponse) outServer.buildResponse(Response.RINGING, secondParty.getRequest());
                    ContentType contentType = ((SIPResponse) resp).getContentTypeHeader();
                    if (contentType != null && contentType.compareMediaRange(IRtpCall.mediaContentType)==0 && ((SIPResponse) resp).getContentLength().getContentLength() > 0) {
                        resp2.setContent(((SIPResponse) resp).getContent(), contentType);
                    }

                    resp2.getTo().setTag(SipServer.createTag());
                    logger.fine("SEND RING\n" + resp2);
                    outServer.sendResponse(resp2);
                    call.setStatus(CallStatus.ringing);
                    return;
                }
                if (secondParty.getCallId().equals(callId)) {
                    //por aca no debiera entrar
                    SipServer outServer = firstParty.getServer();

                    SIPResponse resp2 = (SIPResponse) outServer.buildResponse(Response.RINGING, firstParty.getRequest());
                    resp2.getTo().setTag(SipServer.createTag());
                    //resp2.removeFirst(sVia);
                    ContentType contentType = ((SIPResponse) resp).getContentTypeHeader();
                    if (contentType != null && contentType.compareMediaRange(IRtpCall.mediaContentType)==0 && ((SIPResponse) resp).getContentLength().getContentLength() > 0) {
                        resp2.setContent(((SIPResponse) resp).getContent(), contentType);
                    }

                    logger.fine("SEND RING\n" + resp2);
                    outServer.sendResponse(resp2);
                    call.setStatus(CallStatus.ringing);
                    return;
                }
            }

    }

    public void processSessionProgress(ResponseEvent responseEvent) throws Exception {
        SIPResponse resp = (SIPResponse)responseEvent.getResponse();
        //logger.info("Recieve Session Progress" + resp);

        Call call = getCall(resp);

            CallEndPoint firstParty = call.getInbound();
            CallEndPoint secondParty = call.getOutbound();

            String callId = ((CallIdHeader) resp.getHeader(CallIdHeader.NAME)).getCallId();
            if(!call.isDelayOffer()) {
                ContentType contentType = (ContentType)responseEvent.getResponse().getHeader(LeonidesServer.sContentType);
                if (contentType != null && contentType.compareMediaRange(IRtpCall.mediaContentType)==0 && resp.getContentLength().getContentLength() > 0) {
                    call.getRtpCall().setResponseMedia((SIPMessage) responseEvent.getResponse());
                }
                if (firstParty.getCallId().equals(callId)) {
                    firstParty.setRequire(resp.getHeaders(SIPHeader.REQUIRE));
                    SipServer outServer = secondParty.getServer();

                    if(call.getReferredBy()==null) {
                        SIPResponse resp2 = (SIPResponse) outServer.buildResponse(Response.SESSION_PROGRESS, secondParty.getRequest());
                        //String tag = SipServer.createTag();
                        if(secondParty.getTo().getTag()==null)
                        {
                            if(resp.getTo().getTag()!=null)
                                secondParty.getTo().setTag(resp.getTo().getTag());
                        }
                        resp2.getTo().setTag(secondParty.getTo().getTag());
                        //resp2.setContent(firstParty.createSessionDescription(null), contentType);
                        logger.fine("SEND S.PROGRESS\n" + resp2);
                        outServer.sendResponse(resp2);
                    }
                    call.setStatus(CallStatus.progress);
                    return;
                }
                if (secondParty.getCallId().equals(callId)) {
                    //por aca no debiera entrar
                    SipServer outServer = firstParty.getServer();
                    secondParty.setRequire(resp.getHeaders(SIPHeader.REQUIRE));
                    if(call.getReferredBy()==null) {
                        SIPResponse resp2 = (SIPResponse) outServer.buildResponse(Response.SESSION_PROGRESS, firstParty.getRequest());
                        String tag = SipServer.createTag();
                        resp2.getTo().setTag(tag);
                        firstParty.getTo().setTag(tag);
                        //resp2.setContent(firstParty.createSessionDescription(null), contentType);
                        call.getRtpCall().setCallerMedia();
                        //resp2.removeFirst(sVia);
                        logger.fine("SEND S.PROGRESS\n" + resp2);
                        outServer.sendResponse(resp2);
                    }
                    call.setStatus(CallStatus.progress);
                    return;
                }
            }
            else{//DelayOffer
                if (firstParty.getCallId().equals(callId)) {
                    SipServer outServer = secondParty.getServer();
                    SIPResponse resp2 = (SIPResponse) outServer.buildResponse(Response.SESSION_PROGRESS, secondParty.getRequest());
                    ContentType contentType = ((SIPResponse) resp).getContentTypeHeader();
                    if (contentType != null && contentType.compareMediaRange(IRtpCall.mediaContentType)==0 && ((SIPResponse) resp).getContentLength().getContentLength() > 0) {
                        resp2.setContent(((SIPResponse) resp).getContent(), contentType);
                    }

                    resp2.getTo().setTag(SipServer.createTag());
                    logger.fine("SEND S.PROGRESS\n" + resp2);
                    outServer.sendResponse(resp2);
                    call.setStatus(CallStatus.progress);
                    return;
                }
                if (secondParty.getCallId().equals(callId)) {
                    //por aca no debiera entrar
                    SipServer outServer = firstParty.getServer();
                    SIPResponse resp2 = (SIPResponse) outServer.buildResponse(Response.SESSION_PROGRESS, firstParty.getRequest());
                    resp2.getTo().setTag(SipServer.createTag());
                    //resp2.removeFirst(sVia);
                    ContentType contentType = ((SIPResponse) resp).getContentTypeHeader();
                    if (contentType != null && contentType.compareMediaRange(IRtpCall.mediaContentType)==0 && ((SIPResponse) resp).getContentLength().getContentLength() > 0) {
                        resp2.setContent(((SIPResponse) resp).getContent(), contentType);
                    }

                    logger.fine("SEND S.PROGRESS\n" + resp2);
                    outServer.sendResponse(resp2);
                    call.setStatus(CallStatus.progress);
                    return;
                }
            }
    }

    public void processSuccess(ResponseEvent responseEvent) throws ParseException, SdpException, IOException, SipException, InvalidArgumentException {
        SIPResponse resp = (SIPResponse) responseEvent.getResponse();

        //logger.fine(resp.toString());

        Call call = getCall(resp);
        CallEndPoint firstParty = call.getInbound();
        CallEndPoint secondParty = call.getOutbound();
        String callId = ((CallIdHeader) resp.getHeader(CallIdHeader.NAME)).getCallId();

        if(!call.isDelayOffer()) {
            logger.info("EARLY OFFER " + callId);
            ContentType contentType = (ContentType)responseEvent.getResponse().getHeader(LeonidesServer.sContentType);
            if (call.getStatus()!=CallStatus.progress && contentType != null && contentType.compareMediaRange(IRtpCall.mediaContentType)==0 && resp.getContentLength().getContentLength() > 0) {
                call.getRtpCall().setResponseMedia(resp);
            }

            if (firstParty.getCallId().equals(callId)) {
                SipServer outServer = secondParty.getServer();
                   if(((SIPResponse) responseEvent.getResponse()).getToTag()!=null) {
                        secondParty.getTo().setTag(((SIPResponse) responseEvent.getResponse()).getToTag());
                        firstParty.getTo().setTag(((SIPResponse) responseEvent.getResponse()).getToTag());
                    }

                    //firstParty.parseRemoteDescription((SIPMessage) responseEvent.getResponse());
                    if(call.getReferredBy()==null) {
                        Response resp2 = outServer.buildResponse(Response.OK, secondParty.getRequest());
                        //resp2.setContent(secondParty.createSessionDescription(null), contentType);
                        logger.fine("SEND 200 OK\n" + resp2);
                        outServer.sendResponse(resp2);
                    }
                    if(call.getStatus()!=CallStatus.answered && call.getStatus()!=CallStatus.progress) {
                        //si es un 200 OK a un INVITE y ya tenia los puertos abiertoa es un RE-INVITE
                        //call.startRtp();
                        call.setReferredBy(null);//elimino el anterior si habia
                    }
                    else {
                        if(call.getStatus()==CallStatus.answered)
                            logger.info("REINVITE 200 OK");
                    }

                    call.setStatus(CallStatus.answered);
                    return;
                }
                if (secondParty.getCallId().equals(callId)) {

                    SipServer outServer = firstParty.getServer();
                    if(((SIPResponse) responseEvent.getResponse()).getToTag()!=null) {
                        secondParty.getTo().setTag(((SIPResponse) responseEvent.getResponse()).getToTag());
                    }

                    if(call.getReferredBy()==null) {
                        SIPResponse resp2 = (SIPResponse)outServer.buildResponse(Response.OK, firstParty.getRequest());
                        if(firstParty.getTo().getTag()!=null) {
                            resp2.setToTag(firstParty.getTo().getTag());
                        }
                        Optional<String> sessionDescription = call.getRtpCall().getSessionDescription();
                        resp2.setContent(sessionDescription.get(), contentType);
                        //if(firstParty.getCodec()<0)
                        //    firstParty.setCodec(secondParty.getCodec());
                        logger.fine("SEND 200 OK\n" + resp2);
                        outServer.sendResponse(resp2);
                    }
                    if(call.getStatus()!=CallStatus.answered && call.getStatus()!=CallStatus.progress) {
                        call.getRtpCall().setCallerMedia();
                        //call.startRtp();
                    }
                    else{
                        if(call.getStatus()==CallStatus.answered) {
                            //REINVITE. debo reenviar el 200 OK al telefono original pero no cambiar de puerto.
                            //firstParty.setCodec(secondParty.getCodec());
                            logger.info("REINVITE 200 OK ");
                        }
                    }
                    call.setStatus(CallStatus.answered);
                    call.setReferredBy(null);//elimino el anterior si habia
                    return;
                }
            }
            else {//DelayOffer
                //no respondo con ACK.
                //reenvio el 200 OK y espero el PRACK
                logger.info("DELAY OFFER " + callId);
                if (firstParty.getCallId().equals(callId)) {
                    //server.sendACK(responseEvent);
                    SipServer outServer = secondParty.getServer();
                    Response resp2 = outServer.buildResponse(Response.OK, secondParty.getRequest());
                    ContentType contentType = ((SIPResponse) resp).getContentTypeHeader();
                    if (contentType != null && contentType.compareMediaRange(IRtpCall.mediaContentType)==0 && ((SIPResponse) resp).getContentLength().getContentLength() > 0) {
                        resp2.setContent(((SIPResponse) resp).getContent(), contentType);
                    }
                    //resp2.removeFirst(sVia);

                    logger.fine("SEND 200 OK\n" + resp2);
                    outServer.sendResponse(resp2);
                    return;
                }
                if (secondParty.getCallId().equals(callId)) {

                    //server.sendACK(responseEvent);
                    SipServer outServer = firstParty.getServer();
                    Response resp2 = outServer.buildResponse(Response.OK, firstParty.getRequest());
                    ContentType contentType = ((SIPResponse) resp).getContentTypeHeader();
                    if (contentType != null && contentType.compareMediaRange(IRtpCall.mediaContentType)==0 && ((SIPResponse) resp).getContentLength().getContentLength() > 0) {
                        resp2.setContent(((SIPResponse) resp).getContent(), contentType);
                    }
                    //resp2.removeFirst(sVia);

                    logger.fine("SEND 200 OK\n" + resp2);
                    outServer.sendResponse(resp2);
                    return;
                }

            }
    }
    public void process4XX(ResponseEvent responseEvent) throws SipException, InvalidArgumentException, ParseException {
        String callId = ((CallIdHeader) responseEvent.getResponse().getHeader(CallIdHeader.NAME)).getCallId();
        Call call = getCall(responseEvent.getResponse());
        removeCall(call);
        addCallStats(call.getCallStats());
        CallEndPoint firstParty = call.getInbound();
        //logger.info(call.getCallStats().toString());
        CallEndPoint secondParty = call.getOutbound();
        if(callId.equals(firstParty.getCallId())){
            SipServer outServer = secondParty.getServer();
            Response resp = outServer.buildResponse(responseEvent.getResponse().getStatusCode(), secondParty.getRequest());
            logger.fine("SEND 4XX\n" + resp);
            outServer.sendResponse(resp);
        }
        if(callId.equals(secondParty.getCallId())){
            SipServer outServer = firstParty.getServer();
            Response resp = outServer.buildResponse(responseEvent.getResponse().getStatusCode(), firstParty.getRequest());
            logger.fine("SEND 4XX\n" + resp);
            outServer.sendResponse(resp);
        }
    }
    public void printCallStates()
    {
        synchronized (syncCallStates) {
            for (CallStats stats :
                    callStatses) {
                try {
                    logger.info(stats.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public List<String> showCallStates()
    {
        List<String> cs = new ArrayList<String>();
        synchronized (syncCallStates){
            for (CallStats stats :
                    callStatses) {
                try {
                    String linea = stats.toString().replace("<", "");
                    linea = linea.replace(">", "");
                    cs.add(linea);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return cs;
    }
}
