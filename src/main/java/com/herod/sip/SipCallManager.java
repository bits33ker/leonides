package com.herod.sip;

import com.herod.leonides.LeonidesServer;
import com.herod.sip.call.CallEndPoint;
import com.herod.sip.call.ExpiredException;
import com.herod.sip.call.RouteException;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.header.Authorization;
import gov.nist.javax.sip.header.Contact;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import com.herod.sip.call.Call;
import com.herod.sip.interfaces.ISipRequestEvents;
import com.herod.sip.interfaces.ISipResponseEvents;

import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sdp.SdpException;
import javax.sip.*;
import javax.sip.header.*;
import javax.sip.message.Message;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.io.IOException;
import java.net.InetAddress;
import java.text.ParseException;
import java.util.Properties;

/**
 * Clase que maneja la generación de llamadas sip.
 */
public class SipCallManager implements SipListener, ISipRequestEvents, ISipResponseEvents {
    //region Constants
    //private static final String SDP_TEMPLATE = "v=0\r\no=%s 0 0 IN IP4 %s\ns=%s\r\nt=0 0\r\nm=audio %s RTP/AVP %s %s\r\na=sendrecv\r\nc=IN IP4 %s %s\r\na=rtpmap:101 telephone-event/8000\r\na=fmtp:101 0-15";
    //private static final String sVia = "Via";
    //private static final int REGISTRATION_INTERVAL = 6000;
    private SipServer server;

    private boolean running;

    @Autowired
    private LeonidesServer leonidesServer;

    private static Logger logger = Logger.getLogger(SipCallManager.class.getName());
    //region Constructors
    public SipCallManager(Properties properties, InetAddress host, int port, String transport) throws Exception {
        //users = new HashMap<>();
        init(properties, host, port, transport);
    }
    public SipCallManager() throws Exception {
    }

    public void init(Properties properties, InetAddress host, int port, String transport) throws Exception {
        server = new SipServer(this, properties, host, port, transport);
        running = true;
    }

    //lista de codecs: https://en.wikipedia.org/wiki/RTP_audio_video_profile
    //endregion

    //region Methods
    public void finish() {
        running = false;
        server.close();
    }

    public void setLeonidesServer(LeonidesServer k)
    {
        leonidesServer = k;
    }

    //endregion

    public void destroy() {
        finish();
    }

    //region Getters
    public boolean isRunning() {
        return running;
    }

    public SipServer getServer(){return server;}

    //endregion

    private <T extends Message> void log(T message) {
        String callId = ((CallIdHeader) message.getHeader(CallIdHeader.NAME)).getCallId();
        String method = message instanceof SIPRequest ? ((SIPRequest) message).getMethod() : ((SIPResponse) message).getReasonPhrase();
        String from = message instanceof SIPRequest ? ((SIPRequest) message).getFrom().getAddress().toString() : ((SIPResponse) message).getFrom().getAddress().toString();
        String to = message instanceof SIPRequest ? ((SIPRequest) message).getTo().getAddress().toString() : ((SIPResponse) message).getTo().getAddress().toString();
        String format = String.format("CallId: %s, From: %s, To: %s", callId, from, to);
        logger.info(method + " " +  format);
        logger.fine(message.toString());
    }
    //region SipListener
    @Override
    public synchronized void processRequest(RequestEvent requestEvent) {
        try {
            //log(requestEvent.getRequest());

            String method = requestEvent.getRequest().getMethod();
            switch (method) {
                case Request.REGISTER:
                    doRegister(requestEvent);
                    break;
                case Request.INVITE:
                    this.doInvite(requestEvent);
                    break;
                case Request.BYE:
                    this.doBye(requestEvent);
                    break;
                case Request.CANCEL:
                    this.doCancel(requestEvent);
                    break;
                case Request.PRACK:
                    this.doPrack(requestEvent);
                    break;
                case Request.REFER:
                    this.doRefer(requestEvent);
                    break;
                case Request.ACK:
                    doAck(requestEvent);
                    break;
                case Request.SUBSCRIBE:
                    doSubscribe(requestEvent);
                    break;

                default: {
                    logger.warning(method + " no esta implementado\n");
                    Response resp = server.buildResponse(Response.METHOD_NOT_ALLOWED, requestEvent.getRequest());
                    server.sendResponse(resp);
                }
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }

    }

    @Override
    public void doSubscribe(RequestEvent requestEvent) throws ParseException, SipException, InvalidArgumentException {
        try {
            logger.fine("RECV SUBSCRIBE\n" + requestEvent.getRequest());
            Contact from = ((SIPRequest) requestEvent.getRequest()).getContactHeader();

            //resp.setAddressHeader(CONTACT_HEADER, address);
            Response resp = server.buildResponse(Response.OK, requestEvent.getRequest());
            AddressImpl address = (AddressImpl)from.getAddress();
            //users.put(address.getUserAtHostPort().substring(0, address.getUserAtHostPort().indexOf("@")), address);
            Header header = requestEvent.getRequest().getHeader("Supported");
            server.sendResponse(resp);
        }catch(Exception e)
        {
            e.printStackTrace();

            Response resp = server.buildResponse(Response.BAD_REQUEST, requestEvent.getRequest());
            server.sendResponse(resp);
        }
    }

    @Override
    public void doRegister(RequestEvent requestEvent) throws SipException, InvalidArgumentException, ParseException {
        try {
            //TODO. Falta encriptacion con MD5.
            logger.fine("RECV REGISTER\n" + requestEvent.getRequest());
            Contact from = ((SIPRequest) requestEvent.getRequest()).getContactHeader();

            int expires = from.getExpires();
            Response resp=null;
            if (expires < 0) {
                expires = requestEvent.getRequest().getExpires().getExpires();
            }
            if (expires == 0) {
                //users.remove(((AddressImpl)from.getAddress()).getUserAtHostPort().substring(0, ((AddressImpl)from.getAddress()).getUserAtHostPort().indexOf("@")));
                resp = server.buildResponse(Response.OK, requestEvent.getRequest());
                leonidesServer.remAbonado(from.getAddress());
            } else {
                if(((SIPRequest) requestEvent.getRequest()).hasHeader(Authorization.NAME)) {
                    AuthorizationHeader auth = (AuthorizationHeader)requestEvent.getRequest().getHeader(Authorization.NAME);
                    String r = MitrolAuthHeader.registerAuth((SIPRequest) requestEvent.getRequest(), auth.getRealm(),auth.getNonce(), auth.getOpaque(), "mitrol40$$$");
                    if(r.equals(auth.getResponse())) {
                    //if(true){
                        leonidesServer.addAbonado(from.getAddress(), server.getTransport());
                        resp = server.buildResponse(Response.OK, requestEvent.getRequest());
                    }
                    else{
                        resp = server.buildResponse(Response.UNAUTHORIZED, requestEvent.getRequest());
                        resp.addHeader(MitrolAuthHeader.challengeRegister());
                    }
                }else{
                    //para INVITE
                    //DigestServerAuthenticationHelper digest = new DigestServerAuthenticationHelper();
                    //digest.generateChallenge(this.getServer().getHeaderFactory(), resp, "");
                    leonidesServer.addAbonado(from.getAddress(), server.getTransport());
                    resp = server.buildResponse(Response.OK, requestEvent.getRequest());
                    //resp = server.buildResponse(Response.UNAUTHORIZED, requestEvent.getRequest());
                    //resp.addHeader(MitrolAuthHeader.challengeRegister());
                }
            }
            server.sendResponse(resp);
        }
        catch(SipException e)
        {
            Response resp = server.buildResponse(Response.FORBIDDEN, requestEvent.getRequest());
            server.sendResponse(resp);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            Response resp = server.buildResponse(Response.BAD_REQUEST, requestEvent.getRequest());
            server.sendResponse(resp);
        }
    }

    @Override
    public void doInvite(RequestEvent requestEvent) throws SipException, InvalidArgumentException, ParseException {
        //String callId = ((CallIdHeader) requestEvent.getRequest().getHeader(CallIdHeader.NAME)).getCallId();
        logger.fine("RECV INVITE\n" + requestEvent.getRequest());
        Call call = leonidesServer.getCall(requestEvent.getRequest());
        if(call==null){
            //primer invite
            try {
                if(leonidesServer.getCalls()< leonidesServer.getLicencia().getMaxCalls()) {
                    call = new Call((SIPRequest) requestEvent.getRequest());
                    call.getInbound().setServer(server);
                    server.sendTrying(call.getInbound());
                    leonidesServer.addCall(call);
                    leonidesServer.sendInvite(call, (SIPRequest) requestEvent.getRequest());
                }
                else
                {   //bloqueado por licencia
                    Response resp = server.buildResponse(Response.FORBIDDEN, requestEvent.getRequest());
                    server.sendResponse(resp);
                }
            }
            catch(SipException se)
            {
                Response resp = server.buildResponse(Response.FORBIDDEN, requestEvent.getRequest());
                server.sendResponse(resp);
                se.printStackTrace();
            }
            catch(ExpiredException e)
            {
                Response resp = server.buildResponse(Response.NOT_FOUND, requestEvent.getRequest());
                server.sendResponse(resp);
                e.printStackTrace();
            }
            catch(Exception e)
            {
                Response resp = server.buildResponse(Response.BAD_REQUEST, requestEvent.getRequest());
                server.sendResponse(resp);
                e.printStackTrace();
            }
        }
        else
        {
            try {
                //es un RE-INVITE??? o volvio a enviar el invite por falta de respuesta?
                //si la llamada esta conectada es un reinvite por cambio en la call.
                //sino debe ser un reinvite por timeout
                leonidesServer.sendInvite(call, (SIPRequest)requestEvent.getRequest());
            } catch (SdpException e) {
                Response resp = server.buildResponse(Response.BAD_REQUEST, requestEvent.getRequest());
                server.sendResponse(resp);
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                Response resp = server.buildResponse(Response.FORBIDDEN, requestEvent.getRequest());
                server.sendResponse(resp);
            } catch (RouteException e) {
                Response resp = server.buildResponse(Response.NOT_FOUND, requestEvent.getRequest());
                server.sendResponse(resp);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void doRefer(RequestEvent requestEvent) throws SipException, InvalidArgumentException, ParseException {
        try {
            throw new SipException("No puedo encontrar la llamada " + ((SIPRequest)requestEvent.getRequest()).getCallId());
        }
        catch(SipException se) {
            Response resp = server.buildResponse(Response.NOT_FOUND, requestEvent.getRequest());
            server.sendResponse(resp);
            se.printStackTrace();
        }
        catch(Exception e)
        {
            Response resp = server.buildResponse(Response.BAD_REQUEST, requestEvent.getRequest());
            server.sendResponse(resp);
            e.printStackTrace();
        }
    }

    @Override
    public void doBye(RequestEvent requestEvent) throws SipException, InvalidArgumentException, ParseException {
        String callId = ((CallIdHeader) requestEvent.getRequest().getHeader(CallIdHeader.NAME)).getCallId();
        Call call = leonidesServer.getCall(requestEvent.getRequest());
            try {
                if(call==null)
                    throw new SipException("No puedo encontrar la llamada " + ((SIPRequest)requestEvent.getRequest()).getCallId());

                leonidesServer.processBye(requestEvent);
                Response resp2 = server.buildResponse(Response.OK, requestEvent.getRequest());
                server.sendResponse(resp2);
                //call.stopRtp();
            }
            catch(Exception e)
            {
                Response resp = server.buildResponse(Response.BAD_REQUEST, requestEvent.getRequest());
                server.sendResponse(resp);
                e.printStackTrace();
            }
    }

    @Override
    public void doPrack(RequestEvent requestEvent) throws ParseException, SipException, InvalidArgumentException {
        try{
            SIPRequest request = (SIPRequest) requestEvent.getRequest();
            logger.fine("RECV PRACK\n" + requestEvent.getRequest());

            Call call = leonidesServer.getCall(request);
            if(call==null) {
                throw new Exception("ERROR. Call does not exist");
            }

            if(!call.isDelayOffer()) {
                //no se xq vino un prack
                throw new Exception("ERROR. Not Implemented");
            }

            throw new Exception("ERROR. Not Implemented");
            //leonidesServer.processPrack(requestEvent);

        }catch(Exception e)
        {
            Response resp = server.buildResponse(Response.BAD_REQUEST, requestEvent.getRequest());
            server.sendResponse(resp);
            e.printStackTrace();
        }
    }

    @Override
    public void doCancel(RequestEvent requestEvent) throws SipException, InvalidArgumentException, ParseException {
        try {
            String callId = ((CallIdHeader) requestEvent.getRequest().getHeader(CallIdHeader.NAME)).getCallId();
            Call call = leonidesServer.getCall(requestEvent.getRequest());
            if(call!=null){
                CallEndPoint firstParty = call.getInbound();
                leonidesServer.processCancel(requestEvent);

                SIPResponse resp4XX = (SIPResponse)server.buildResponse(Response.REQUEST_TERMINATED, firstParty.getRequest());
                resp4XX.getTo().setTag(firstParty.getTo().getTag());
                resp4XX.getTopmostVia().removeParameter("received");
                server.sendResponse(resp4XX);
            }

        }
        catch(Exception e)
        {
            Response resp = server.buildResponse(Response.BAD_REQUEST, requestEvent.getRequest());
            server.sendResponse(resp);
            e.printStackTrace();
        }
    }

    @Override
    public void doAck(RequestEvent requestEvent)
    {
        logger.fine("RECV ACK\n" + requestEvent.getRequest());
        CSeqHeader seq = (CSeqHeader)requestEvent.getRequest().getHeader("CSeq");
        if(seq.getMethod().equals(Request.BYE))
        {
            Call call = leonidesServer.getCall(requestEvent.getRequest());
            if(call!=null) {
                leonidesServer.removeCall(call);
                leonidesServer.addCallStats(call.getCallStats());
                //logger.info(call.getCallStats().toString());
            }

        }

    }

    @Override
    public void processResponse(ResponseEvent responseEvent) {

        log(responseEvent.getResponse());
        int statusCode = responseEvent.getResponse().getStatusCode();
        switch (statusCode) {
            case Response.TRYING:
                doTry(responseEvent);
                break;
            case Response.RINGING:
                doRinging(responseEvent);
                break;
            case Response.SESSION_PROGRESS:
                doSessionProgress(responseEvent);
                break;
            case Response.OK:
                doSuccess(responseEvent);
                break;
            case Response.NOT_FOUND:
                do4XX(responseEvent, false);
                break;
            default:
                if(statusCode>=400 && statusCode<500)
                    do4XX(responseEvent, true);
                else
                    logger.info("Status code no implemented yet!: " + Integer.toString(statusCode));
                break;
        }
    }

    @Override
    public void doRinging(ResponseEvent responseEvent){
        try{
            leonidesServer.processRing(responseEvent);
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void doSessionProgress(ResponseEvent responseEvent){
        SIPResponse resp = (SIPResponse)responseEvent.getResponse();
        logger.fine("RECV S.PROGRESS\n" + resp);

        Call call = leonidesServer.getCall(resp);
        if(call==null) {
            logger.severe("No call for " + resp);
            return;
        }
        try{
            CallEndPoint firstParty = call.getInbound();
            CallEndPoint secondParty = call.getOutbound();
            String callId = ((CallIdHeader) resp.getHeader(CallIdHeader.NAME)).getCallId();

            leonidesServer.processSessionProgress(responseEvent);
            if(!call.isDelayOffer()) {
                if (firstParty.getCallId().equals(callId)){
                    //if (firstParty.require100rel()) {
                    //    server.sendPRACK(firstParty);
                    //}
                }
                if (secondParty.getCallId().equals(callId)){
                    //if (secondParty.require100rel()) {
                    //    server.sendPRACK(secondParty);
                    //}
                }
            }

        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void doSuccess(ResponseEvent responseEvent){
        SIPResponse resp = (SIPResponse) responseEvent.getResponse();
        logger.fine("RECV 200 OK\n" + resp);

        Call call = leonidesServer.getCall(resp);
        if(call==null) {
            logger.severe("No call for " + resp);
            return;
        }
        try{
            if(((SIPResponse) responseEvent.getResponse()).getCSeq().getMethod().equals(Request.PRACK))
            {
                //logger.info("200 OK PRACK: " + resp);
                return;
            }

            if(((SIPResponse) responseEvent.getResponse()).getCSeq().getMethod().equals(Request.BYE))
            {
                //logger.info("200 OK BYE: " + resp);
                leonidesServer.removeCall(call);
                leonidesServer.addCallStats(call.getCallStats());
                //logger.info(call.getCallStats().toString());
                //if(call.getReferTo()!=null)
                //{
                //es una llamada referida
                //call.stopRtp();
                //paro los paquetes RTP a la espera de Transferir la llamada
                return;
                //}
            }
            leonidesServer.processSuccess(responseEvent);
            if(!call.isDelayOffer()) {
                CallEndPoint firstParty = call.getInbound();
                CallEndPoint secondParty = call.getOutbound();
                String callId = ((CallIdHeader) resp.getHeader(CallIdHeader.NAME)).getCallId();
                if (firstParty.getCallId().equals(callId))
                    server.sendACK(responseEvent, firstParty.getFrom(), firstParty.getTo());
                if (secondParty.getCallId().equals(callId))
                    server.sendACK(responseEvent, secondParty.getFrom(), secondParty.getTo());
            }
        }catch(Exception e)
        {
            //TODO. Exception doSuccess. Si salta por excepcion cortar la llamada. Ver como!!
            e.printStackTrace();
        }
    }

    @Override
    public void do4XX(ResponseEvent responseEvent, boolean bAck) {
        Call call = leonidesServer.getCall(responseEvent.getResponse());
        try {
            if(call==null)
                throw new Exception("ERROR. Call does not exist");
            leonidesServer.process4XX(responseEvent);
            if(bAck)
                server.sendACK(responseEvent);
        }
        catch(Exception e)
        {
            logger.severe("do4XX ERROR");
            e.printStackTrace();
        }
    }

    @Override
    public void doTry(ResponseEvent responseEvent){
        try{
            leonidesServer.processTry(responseEvent);
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void processTimeout(TimeoutEvent timeoutEvent) {
        //Si hubo un timeOut finalizamos la sessión si y lo informamos.
        //logger.warn(String.format("Time out against the sip server %s:%s from %s:%s", sipClientConnectionInfo.getServerHost(), sipClientConnectionInfo.getServerPort(), sipClientConnectionInfo.getLocalHost(), sipClientConnectionInfo.getLocalPort()));
        logger.warning(String.format("Time out against the sip server %s:%s", server.getHost(), server.getServerPort()));
        //No fijamos si el llamado se realizo como cliente.
    }


    @Override
    public void processIOException(IOExceptionEvent ioExceptionEvent) {
        logger.severe(String.format("IO exception againts sip server, %s:%s by transport :%s", ioExceptionEvent.getHost()
                , ioExceptionEvent.getPort()
                , ioExceptionEvent.getTransport()));
    }

    @Override
    public void processTransactionTerminated(TransactionTerminatedEvent transactionTerminatedEvent) {

        /*if (!transactionTerminatedEvent.isServerTransaction())
        {
            //Si fue así, utilizaremos el callId para finalizar la sessión que tuvo el timeout.
            String callId = ((CallIdHeader)transactionTerminatedEvent.getClientTransaction().buildRequest().getHeader(CallIdHeader.NAME)).getInteractionId();
            sipSessionHashMap.get(callId).finish(FinishCause.errorFinishCause("Terminated transaction by sip server"));
        }*/
        logger.fine("Transaction Terminated");
    }

    @Override
    public void processDialogTerminated(DialogTerminatedEvent dialogTerminatedEvent) {
  /*      if (!dialogTerminatedEvent.getDialog().isServer())
        {
            //Si fue así, utilizaremos el callId para finalizar la sessión que tuvo el timeout.
            String callId = dialogTerminatedEvent.getDialog().getInteractionId().getInteractionId();
            sipSessionHashMap.get(callId).finish(FinishCause.errorFinishCause("Terminated dialog by sip server"));
        }*/
        logger.fine("Dialog Terminated");
    }
    //endregion
}


