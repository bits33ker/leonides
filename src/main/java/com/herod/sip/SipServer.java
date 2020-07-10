package com.herod.sip;

import com.google.common.hash.HashCode;
import com.herod.leonides.utils.LeonidesUtils;
import gov.nist.core.Host;
import gov.nist.core.HostPort;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.header.extensions.ReferredBy;
import gov.nist.javax.sip.header.extensions.ReferredByHeader;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;
import gov.nist.javax.sip.stack.HopImpl;
import gov.nist.javax.sip.stack.SIPClientTransactionImpl;
import com.herod.sip.call.CallEndPoint;
import org.apache.commons.lang3.SerializationUtils;

import javax.sdp.SdpException;
import javax.sip.*;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.*;
import javax.sip.message.Message;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

public class SipServer {

    private static final int MAX_UNAUTHORIZED_REGISTERS = 5;
    private static final int MAX_TIME_OUTS = 5;
    private static Random tag = new Random();

    //region Constants
    public static String RESPONSE_ATTRIBUTE_NAME = "response";
    public static final String BRANCH_MAGIC_COOKIE = "z9hG4bK";
    //endregion

    //region Attributes
    private InetAddress SERVER_HOST;// = "127.0.0.1"
    private int SERVER_PORT = 5060;//x default udp
    private int MAX_FORWARDS=70;
    private int RELAY_TIME=100;
    String transport;

    private String USER_AGENT = "Leonides";
    private boolean DELAY_OFFER=false;
    private String IDENTIFIER="localhost";
    private String SIPSERVER_TYPE="Back2Back";//SoftSwitch
    private int REGISTER_TIMEOUT=30000;
    private int INVITE_TIMEOUT=1000;
    private boolean AUDIO_ENABLED = true;
    private boolean VIDEO_ENABLED = false;
    private boolean RTCP_ENABLED = false;

    private final Logger logger = Logger.getLogger(com.herod.sip.SipServer.class.getName());
    //private final SipClientConnectionInfo sipClientConnectionInfo;
    private final MessageFactory messageFactory;
    private final HeaderFactory headerFactory;
    private final AddressFactory addressFactory;
    //private final SipClientConnectionInfo originalSipClientConnectionInfo;
    private SipStack sipStack;
    private ListeningPoint listeningEndPoint;
    private SipProvider provider;
    private Properties properties;
    private SipListener sipListener;
    //private Map<String, SipSessionContext> sipSessionHashMap = Collections.synchronizedMap(new HashMap<String, SipSessionContext>());
    private boolean running = true;
    private long sequence = 1;
    //endregion

    //region Constructor
    public SipServer(SipListener sipListener, Properties props,
                     InetAddress host, int port, String transport) throws SipException, SocketException, UnknownHostException, InvalidArgumentException, TooManyListenersException {

        this.sipListener = sipListener;
        //this.originalSipClientConnectionInfo = SerializationUtils.clone(sipClientConnectionInfo);
        //this.sipClientConnectionInfo = sipClientConnectionInfo;
        this.messageFactory = SipFactory.getInstance().createMessageFactory();
        this.headerFactory = SipFactory.getInstance().createHeaderFactory();
        this.addressFactory = SipFactory.getInstance().createAddressFactory();

        SERVER_HOST = host;
        SERVER_PORT = port;
        this.transport = transport;

        //Stack Properties
        if(props==null) {
            properties = new Properties();
            //properties.putAll(props);
            properties.setProperty("javax.sip.STACK_NAME", "Jain-Sip-RI-Stack");
            //properties.setProperty("javax.sip.IP_ADDRESS", NetUtils.getIPv4InetAddress().getHostAddress());
            properties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", "gov.nist.javax.sip.stack.NioMessageProcessorFactory");

            //DEBUG
            properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "16");
            //properties.setProperty("gov.nist.javax.sip.SERVER_LOG","textclient.txt");
            //properties.setProperty("gov.nist.javax.sip.DEBUG_LOG","textclientdebug.log");
            properties.setProperty("gov.nist.javax.sip.DELIVER_UNSOLICITED_NOTIFY", "true");
            properties.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", "64");
            properties.setProperty("gov.nist.javax.sip.REENTRANT_LISTENER", "true");
            properties.setProperty("gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE", "65536");
            //DEBUG
        }
        else {
            properties = SerializationUtils.clone(props);
            /*SERVER_HOST = properties.getProperty(LeonidesUtils.SERVER_HOST, InetAddress.getLocalHost().getHostAddress());
            UDP_PORT = Integer.parseInt(properties.getProperty(LeonidesUtils.UDP_PORT, "5060"));
            TCP_PORT = Integer.parseInt(properties.getProperty(LeonidesUtils.TCP_PORT, "5061"));
            TLS_PORT = Integer.parseInt(properties.getProperty(LeonidesUtils.TLS_PORT, "5062"));
            TRANSPORT_UDP = Boolean.parseBoolean(properties.getProperty(LeonidesUtils.TRANSPORT_UDP, "true"));
            if(TRANSPORT_UDP) SERVER_PORT = UDP_PORT;
            TRANSPORT_TCP = Boolean.parseBoolean(properties.getProperty(LeonidesUtils.TRANSPORT_TCP, "false"));
            if(TRANSPORT_TCP) SERVER_PORT = TCP_PORT;
            TRANSPORT_TLS = Boolean.parseBoolean(properties.getProperty(LeonidesUtils.TRANSPORT_TLS, "false"));
            if(TRANSPORT_TLS) SERVER_PORT = TLS_PORT;*/
            MAX_FORWARDS = Integer.parseInt(properties.getProperty(LeonidesUtils.MAX_FORWARDS, "70"));
            RELAY_TIME = Integer.parseInt(properties.getProperty(LeonidesUtils.RELAY_TIME, "100"));
            USER_AGENT = properties.getProperty(LeonidesUtils.USER_AGENT, "Karen");
            DELAY_OFFER = Boolean.parseBoolean(properties.getProperty(LeonidesUtils.DELAY_OFFER, "false"));
            IDENTIFIER = properties.getProperty(LeonidesUtils.IDENTIFIER, "localhost");
            REGISTER_TIMEOUT = Integer.parseInt(properties.getProperty(LeonidesUtils.REGISTER_TIMEOUT, "30000"));
            INVITE_TIMEOUT = Integer.parseInt(properties.getProperty(LeonidesUtils.INVITE_TIMEOUT, "1000"));
            AUDIO_ENABLED = Boolean.parseBoolean(properties.getProperty(LeonidesUtils.AUDIO_ENABLED, "true"));
            VIDEO_ENABLED = Boolean.parseBoolean(properties.getProperty(LeonidesUtils.VIDEO_ENABLED, "false"));
            RTCP_ENABLED = Boolean.parseBoolean(properties.getProperty(LeonidesUtils.RTCP_ENABLED, "false"));
            SIPSERVER_TYPE = properties.getProperty(LeonidesUtils.SIPSERVER_TYPE, "Back2Back");
        }

        initSipStack();
    }

    public SipServer(SipListener sipListener, Properties props, int port) throws SipException, SocketException, UnknownHostException, InvalidArgumentException, TooManyListenersException {

        this.sipListener = sipListener;
        //this.originalSipClientConnectionInfo = SerializationUtils.clone(sipClientConnectionInfo);
        //this.sipClientConnectionInfo = sipClientConnectionInfo;
        this.messageFactory = SipFactory.getInstance().createMessageFactory();
        this.headerFactory = SipFactory.getInstance().createHeaderFactory();
        this.addressFactory = SipFactory.getInstance().createAddressFactory();

        SERVER_PORT = port;

        //Stack Properties
        if(props==null) {
            properties = new Properties();
            //properties.putAll(props);
            properties.setProperty("javax.sip.STACK_NAME", "Jain-Sip-RI-Stack");
            //properties.setProperty("javax.sip.IP_ADDRESS", NetUtils.getIPv4InetAddress().getHostAddress());
            //String outbandProxy = String.format("%s:%s/%s", sipClientConnectionInfo.getServerHost(), String.valueOf(sipClientConnectionInfo.getServerPort()), sipClientConnectionInfo.getSipTransportChannel().toString());
            //properties.setProperty("javax.sip.OUTBOUND_PROXY", outbandProxy);
            properties.setProperty("gov.nist.javax.sip.MESSAGE_PROCESSOR_FACTORY", "gov.nist.javax.sip.stack.NioMessageProcessorFactory");

            //DEBUG
            properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "16");
            //properties.setProperty("gov.nist.javax.sip.SERVER_LOG","textclient.txt");
            //properties.setProperty("gov.nist.javax.sip.DEBUG_LOG","textclientdebug.log");
            properties.setProperty("gov.nist.javax.sip.DELIVER_UNSOLICITED_NOTIFY", "true");
            properties.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", "64");
            properties.setProperty("gov.nist.javax.sip.REENTRANT_LISTENER", "true");
            properties.setProperty("gov.nist.javax.sip.RECEIVE_UDP_BUFFER_SIZE", "65536");
            //DEBUG
        }
        else {
            properties = SerializationUtils.clone(props);
            /*
            SERVER_HOST = properties.getProperty(LeonidesUtils.SERVER_HOST, InetAddress.getLocalHost().getHostAddress());
            UDP_PORT = port;//Integer.parseInt(properties.getProperty(LeonidesUtils.UDP_PORT, "5060"));
            TCP_PORT = port + 1;//Integer.parseInt(properties.getProperty(LeonidesUtils.TCP_PORT, "5061"));
            TLS_PORT = port + 2;//Integer.parseInt(properties.getProperty(LeonidesUtils.TLS_PORT, "5062"));
            TRANSPORT_UDP = Boolean.parseBoolean(properties.getProperty(LeonidesUtils.TRANSPORT_UDP, "true"));
            if(TRANSPORT_UDP) SERVER_PORT = UDP_PORT;
            TRANSPORT_TCP = Boolean.parseBoolean(properties.getProperty(LeonidesUtils.TRANSPORT_TCP, "false"));
            if(TRANSPORT_TCP) SERVER_PORT = TCP_PORT;
            TRANSPORT_TLS = Boolean.parseBoolean(properties.getProperty(LeonidesUtils.TRANSPORT_TLS, "false"));
            if(TRANSPORT_TLS) SERVER_PORT = TLS_PORT;
            */
            MAX_FORWARDS = Integer.parseInt(properties.getProperty(LeonidesUtils.MAX_FORWARDS, "70"));
            RELAY_TIME = Integer.parseInt(properties.getProperty(LeonidesUtils.RELAY_TIME, "100"));
            USER_AGENT = properties.getProperty(LeonidesUtils.USER_AGENT, "Karen");
            DELAY_OFFER = Boolean.parseBoolean(properties.getProperty(LeonidesUtils.DELAY_OFFER, "false"));
            IDENTIFIER = properties.getProperty(LeonidesUtils.IDENTIFIER, "localhost");
            REGISTER_TIMEOUT = Integer.parseInt(properties.getProperty(LeonidesUtils.REGISTER_TIMEOUT, "30000"));
            INVITE_TIMEOUT = Integer.parseInt(properties.getProperty(LeonidesUtils.INVITE_TIMEOUT, "1000"));
            AUDIO_ENABLED = Boolean.parseBoolean(properties.getProperty(LeonidesUtils.AUDIO_ENABLED, "true"));
            VIDEO_ENABLED = Boolean.parseBoolean(properties.getProperty(LeonidesUtils.VIDEO_ENABLED, "false"));
            RTCP_ENABLED = Boolean.parseBoolean(properties.getProperty(LeonidesUtils.RTCP_ENABLED, "false"));
        }

        initSipStack();
    }

    private void initSipStack() throws SipException, InvalidArgumentException, TooManyListenersException {

        sipStack = new SipStackImpl(properties);

        try {
            //Luego de los cambios en el MitE1x ya no es necesario escuchar en el puerto 5060. Por lo que pedimos un puerto libre.
            //NetUtils.allocatePort(this, this.getServerPort(), false);

            logger.info("createListeningPoint " + SERVER_HOST.getHostAddress() + ":" + this.getServerPort());
            listeningEndPoint = sipStack.createListeningPoint(SERVER_HOST.getHostAddress(), this.getServerPort(), this.getTransport());
            //udpListeningEndPoint = sipStack.createListeningPoint(this.localHost, this.sipPort, SipTransportChannel.UDP.toString());
            provider = sipStack.createSipProvider(listeningEndPoint);

            provider.setAutomaticDialogSupportEnabled(false);

            provider.addSipListener(sipListener);

        }catch(TransportNotSupportedException e) {
            logger.severe("TransportNotSupportedException: " + SERVER_HOST.getHostAddress() + "\n" + e);
            throw e;
        }catch (ObjectInUseException e) {
            logger.severe("ObjectInUseException: " + SERVER_HOST.getHostAddress() + "\n" + e);
            throw e;
        } catch (SipException e) {
                logger.severe("ERROR creating Sip listening Endpoint at Host:" + SERVER_HOST.getHostAddress() + "\n" + e);
            throw e;
        } catch (Exception e) {
            //logger.fatal(e, "No se pudo establecer conexión con el Host:" + this.sipClientConnectionInfo.getServerHost() + ", port:" + this.sipClientConnectionInfo.getServerPort() + ". Desde el host local " + this.SERVER_HOST + " puerto " + this.SERVER_PORT + " Verifique que pueda establecer contacto con dicho host desde este equipo.");
            logger.severe( e.getMessage());
        }
    }

    //endregion

    //region Getters

    public String getHost() {
        return SERVER_HOST.getHostAddress();
    }

    public InetAddress getInetAddress(){return SERVER_HOST;}

    /*public int getUdpPort() {
        return UDP_PORT;
    }

    public int getTcpPort() {
        return TCP_PORT;
    }

    public int getTlsPort() {
        return TLS_PORT;
    }
*/
    public int getServerPort(){ return SERVER_PORT;}

    public String getTransport() throws SipException {
        //if(TRANSPORT_UDP) return "UDP";
        //if(TRANSPORT_TCP) return "TCP";
        //if(!TRANSPORT_TLS) throw new SipException("TRANSPORT ERROR");
        //return "TLS";
        return transport;
    }

    public String getUserAgent() {
        return USER_AGENT;
    }

     public int getInviteTimeout(){
         return INVITE_TIMEOUT;
     }

     public int getRegisterTimeout(){
         return REGISTER_TIMEOUT;
     }

    public boolean isAudioEnabled() {
        return AUDIO_ENABLED;
    }

    public boolean isVideoEnabled() {
        return VIDEO_ENABLED;
    }

    public boolean isRtcpEnabled() {
        return RTCP_ENABLED;
    }

    //endregion

    //region Private Methods

    private void checkMaxRegistrationAttempsError(ResponseEvent responseEvent) {
        int code = responseEvent.getResponse().getStatusCode();
    }

    private SipUser getSipUserFromUri(URI uri) {
        String userName = SipUser.getSipUserName(uri);
        String domain = SipUser.getSipDomain(uri);
        return new SipUser(userName, domain);
    }

 /*
    private Address getAddress(SipUser user) throws ParseException {
        Address address = addressFactory.createAddress(user.getDomain() != null ? formatAddress(user.getUserName(), user.getDomain()) : formatAddress(user.getUserName(), sipClientConnectionInfo.getServerHost() + ":" + sipClientConnectionInfo.getServerPort()));
        String displayName = user.getDisplayName();
        if (displayName != null) {
            address.setDisplayName(displayName);
        }
        return address;
    }
    */

    private void addContactHeader(Message sipMessage, SipUser sipUser) throws ParseException {
        String domain = SERVER_HOST.getHostAddress() + ":" + this.getServerPort();
        String formatAddress = formatAddress(sipUser.getUserName(), domain);
        Address address = addressFactory.createAddress(formatAddress);
        String displayName = sipUser.getDisplayName();
        if (displayName != null) {
            address.setDisplayName(displayName);
        }
        addContactHeader(sipMessage, address);

    }
    public void addExpiration(SIPMessage message, int expirationTime) throws InvalidArgumentException {
        message.addHeader(headerFactory.createExpiresHeader(expirationTime));
    }

    private void addContactHeader(Message sipMessage, Address address) throws ParseException {
        sipMessage.addHeader(headerFactory.createContactHeader(address));
    }
    public void addReferTo(Message sipMessage, Address to, String replace) throws ParseException {
        ReferToHeader referToHeader = headerFactory.createReferToHeader(to);
        referToHeader.setParameter(ReferTo.REPLACES, replace);
        sipMessage.addHeader(referToHeader);
    }

    public void addReferTo(Message sipMessage, Address to) throws ParseException {
        ReferToHeader referToHeader = headerFactory.createReferToHeader(to);
        //referToHeader.setParameter(ReferTo.REPLACES, callid);
        sipMessage.addHeader(referToHeader);
    }

    public void addReferredBy(Message sipMessage, Address from) throws ParseException {
        ReferredByHeader referrredByHeader = (ReferredByHeader)headerFactory.createHeader(ReferredBy.NAME, from.toString());
        //referToHeader.setParameter(ReferTo.REPLACES, callid);
        sipMessage.addHeader(referrredByHeader);
    }

    public HeaderFactory getHeaderFactory(){
        return headerFactory;
    }

     public void sendRequest(Request request) throws SipException {

        //StateLess
        //provider.sendRequest(request);
        //StatesFully
        //Generamos una transacción para el envío.
        ClientTransaction clientTransaction = provider.getNewClientTransaction(request);
        clientTransaction.setRetransmitTimer(RELAY_TIME);
        //Enviamos el mensaje.
        clientTransaction.sendRequest();
        log("SEND", request);
    }

    protected void sendRequest(Request request, SipURI uri) throws SipException, IOException {

        //StateLess
        //provider.sendRequest(request);
        //StatesFully
        //Generamos una transacción para el envío.
        ClientTransaction clientTransaction = provider.getNewClientTransaction(request);
        clientTransaction.setRetransmitTimer(RELAY_TIME);
        //Enviamos el mensaje.
        ((SIPClientTransactionImpl)clientTransaction).setNextHop(new HopImpl(uri.getHost(), uri.getPort(), "UDP"));
        HostPort hp = new HostPort();
        hp.setHost(new Host(uri.getHost()));
        hp.setPort(uri.getPort());
        ((SIPClientTransactionImpl) clientTransaction).setEncapsulatedChannel(((SIPClientTransactionImpl) clientTransaction).getMessageProcessor().createMessageChannel(hp));
        clientTransaction.sendRequest();
        log("SEND", request);
    }

    public void sendResponse(Response response) throws SipException, InvalidArgumentException {
        provider.sendResponse(response);
        log("SEND", response);
    }

    private String formatAddress(String user, String domain) {
        String format = "sip:%s@%s";
        return String.format(format, user, domain);
    }

    private ToHeader getToHeader(SipUser to) throws ParseException {
        Address toAddress = to.getDomain() != null ? addressFactory.createAddress(formatAddress(to.getUserName(), to.getDomain())) : addressFactory.createAddress(formatAddress(to.getUserName(), SERVER_HOST.getHostAddress() + ":" + this.getServerPort()));
        String displayName = to.getDisplayName();
        if (displayName != null) {
            toAddress.setDisplayName(displayName);
        }
        ToHeader toHeader = headerFactory.createToHeader(toAddress, to.getUserName());
        toHeader.setTag(to.getTag());

        return toHeader;
    }

    private FromHeader getFromHeader(SipUser from) throws ParseException {
        Address fromAddress = from.getDomain() != null ? addressFactory.createAddress(formatAddress(from.getUserName(), from.getDomain())) : addressFactory.createAddress(formatAddress(from.getUserName(), SERVER_HOST.getHostAddress() + ":" + this.getServerPort()));
        String displayName = from.getDisplayName();
        if (displayName != null) {
            fromAddress.setDisplayName(displayName);
        }
        FromHeader fromHeader = headerFactory.createFromHeader(fromAddress, from.getUserName());
        fromHeader.setTag(from.getTag());
        return fromHeader;
    }
    public Response buildResponse(int responseCode, Request request) throws ParseException {
        SIPResponse response = (SIPResponse) messageFactory.createResponse(responseCode, request);
        if(responseCode == Response.OK)
        {
            ContactHeader contactHeader = headerFactory.createContactHeader(response.getTo().getAddress());
            response.addHeader(contactHeader);
        }
        createHeaders(response, null);
        return response;
    }

    public static String createBranch() {
        // https://code.google.com/p/sipservlets/issues/detail?id=269
        return BRANCH_MAGIC_COOKIE + UUID.randomUUID().toString();
    }
    public String createCallId() throws UnknownHostException {
        // https://code.google.com/p/sipservlets/issues/detail?id=269
        return UUID.randomUUID().toString() + "@" + this.getHost() + ":" + Integer.toString(this.getServerPort());
    }
    
    public String updateCallId(String callId)
    {
        if(callId.indexOf('@')>0)
            callId = callId.substring(0, callId.indexOf('@'));
        callId = callId + "@" + SERVER_HOST.getHostAddress() + ":" + Integer.toString(this.getServerPort());
        return callId;
    }

    public static String createTag() {
        /* RFC3261: https://tools.ietf.org/html/rfc3261
    If a request contained a To tag in the request, the To header field
   in the response MUST equal that of the request.  However, if the To
   header field in the request did not contain a tag, the URI in the To
   header field in the response MUST equal the URI in the To header
   field; additionally, the UAS MUST add a tag to the To header field in
   the response (with the exception of the 100 (Trying) response, in
   which a tag MAY be present).  This serves to identify the UAS that is
   responding, possibly resulting in a component of a dialog ID.  The
   same tag MUST be used for all responses to that request, both final
   and provisional (again excepting the 100 (Trying)).  Procedures for
   the generation of tags are defined in Section 19.3.
         */
        return HashCode.fromInt(tag.nextInt()).toString();
    }

    protected Request buildCancel(SIPMessage sipRequest, FromHeader from, ToHeader to) throws ParseException, InvalidArgumentException, UnknownHostException, SipException {

        //int sipPort = this.sipClientConnectionInfo.getServerPort();
        String callId =this.updateCallId(((CallIdHeader) sipRequest.getHeader(CallIdHeader.NAME)).getCallId());
        CallIdHeader callIdHeader = headerFactory.createCallIdHeader(callId);
        //Si la session sip es manejada por nosotros, podemos saber el número de secuencia pidiendolo al contexto de la sesión.
        //SipSessionContext sipSessionContext = sipSessionHashMap.get(getSipSessionOwnerId(sipSession));
        //if (sipSessionContext != null) {
        //   sequence = sipSessionContext.addSentSequence(callId, type);
        //}

        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(sipRequest.getCSeq().getSeqNumber(), Request.CANCEL);

        MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(MAX_FORWARDS);

        //ViaList viaHeaders = sipRequest.getViaHeaders();
        Vector<ViaHeader> viaHeaders = new Vector<>();
        //ViaHeader viaHeader = headerFactory.createViaHeader(SERVER_HOST, SERVER_PORT, transportParam, null);
        if(viaHeaders!=null) {
            //Via via = new Via();
            //via.setHost(sipClientConnectionInfo.getLocalHost());
            //via.setPort(sipClientConnectionInfo.getLocalPort());
            //via.setBranch(((ViaHeader) viaHeaders.getFirst()).getBranch());
            ViaHeader via = headerFactory.createViaHeader(SERVER_HOST.getHostAddress(), this.getServerPort(), this.getTransport(), null);
            via.setBranch(sipRequest.getTopmostVia().getBranch());
            viaHeaders.add(0, via);
        }
        List<String> userAgents = new ArrayList<String>(Arrays.asList(USER_AGENT));
        UserAgentHeader userAgentHeader = headerFactory.createUserAgentHeader(userAgents);

        URI uri = to.getAddress().getURI();

        Request request = messageFactory.createRequest(uri,
                Request.CANCEL,
                callIdHeader,
                cSeqHeader,
                from,
                to,
                viaHeaders,
                maxForwardsHeader
        );

        request.addHeader(userAgentHeader);

        Contact contact = sipRequest.getContactHeader();
        if(contact!=null) {
            ContactHeader contactHeader = headerFactory.createContactHeader(sipRequest.getContactHeader().getAddress());
            request.addHeader(contactHeader);
        }

        return request;
    }

    private void createHeaders(SIPMessage message, String userAgent) throws ParseException {
        //UserAgentHeader userAgentHeader = headerFactory.createUserAgentHeader(userAgents);
        if(userAgent!=null) {
            List<String> userAgents = new ArrayList<String>();
            userAgents.add(userAgent);
            UserAgentHeader userAgentHeader = headerFactory.createUserAgentHeader(userAgents);
            message.addHeader(userAgentHeader);
        }

        List<String> server = new ArrayList<String>(Arrays.asList(LeonidesUtils.SERVER_VERSION));
        ServerHeader serverHeader = headerFactory.createServerHeader(server);
        message.addHeader(serverHeader);

        AllowHeader allowHeader = headerFactory.createAllowHeader(LeonidesUtils.SERVER_ALLOW);
        message.addHeader(allowHeader);
    }
    public Request buildRequest(String type, SIPMessage sipRequest, String callId, URI proxy, FromHeader from, ToHeader to) throws ParseException, InvalidArgumentException, UnknownHostException, SipException {
        //callId = this.updateCallId(callId);
        CallIdHeader callIdHeader = headerFactory.createCallIdHeader(callId);

        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(sequence++, type);

        MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(MAX_FORWARDS);

        Vector<ViaHeader> viaHeaders = new Vector<>();
        if(viaHeaders!=null) {
            ViaHeader via = headerFactory.createViaHeader(SERVER_HOST.getHostAddress(), this.getServerPort(), this.getTransport(), null);
            via.setBranch(createBranch());
            viaHeaders.add(0, via);
        }
        //List<String> userAgents = new ArrayList<String>(Arrays.asList(USER_AGENT));
        //UserAgentHeader userAgentHeader = headerFactory.createUserAgentHeader(userAgents);

        SIPRequest request = (SIPRequest) messageFactory.createRequest(proxy,
                type,
                callIdHeader,
                cSeqHeader,
                from,
                to,
                viaHeaders,
                maxForwardsHeader
        );

        createHeaders(request, (sipRequest!=null && (UserAgentHeader)sipRequest.getHeader(UserAgent.NAME)!=null)?(String)((UserAgentHeader)sipRequest.getHeader(UserAgent.NAME)).getProduct().next():null);

        if(sipRequest!=null)
        {
            Contact contact = sipRequest.getContactHeader();
            if(contact!=null) {
                ContactHeader contactHeader = headerFactory.createContactHeader(sipRequest.getContactHeader().getAddress());
                request.addHeader(contactHeader);
            }
        }
        else
        {
            ContactHeader contactHeader = headerFactory.createContactHeader(from.getAddress());
            request.addHeader(contactHeader);
        }

        return request;
    }

    public Request buildRequest(String type, SIPMessage sipRequest, String callId, FromHeader from, ToHeader to) throws ParseException, InvalidArgumentException, UnknownHostException, SipException {
        //callId = this.updateCallId(callId);
        CallIdHeader callIdHeader = headerFactory.createCallIdHeader(callId);

        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(sequence++, type);

        MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(MAX_FORWARDS);

        Vector<ViaHeader> viaHeaders = new Vector<>();
        if(viaHeaders!=null) {
            ViaHeader via = headerFactory.createViaHeader(SERVER_HOST.getHostAddress(), this.getServerPort(), this.getTransport(), null);
            via.setBranch(createBranch());
            viaHeaders.add(0, via);
        }
        //List<String> userAgents = new ArrayList<String>(Arrays.asList(USER_AGENT));
        //UserAgentHeader userAgentHeader = headerFactory.createUserAgentHeader(userAgents);

        URI uri = to.getAddress().getURI();

        SIPRequest request = (SIPRequest) messageFactory.createRequest(uri,
                type,
                callIdHeader,
                cSeqHeader,
                from,
                to,
                viaHeaders,
                maxForwardsHeader
        );

        createHeaders(request, (sipRequest!=null && (UserAgentHeader)sipRequest.getHeader(UserAgent.NAME)!=null)?(String)((UserAgentHeader)sipRequest.getHeader(UserAgent.NAME)).getProduct().next():null);

        if(sipRequest!=null)
        {
            Contact contact = sipRequest.getContactHeader();
            if(contact!=null) {
                ContactHeader contactHeader = headerFactory.createContactHeader(sipRequest.getContactHeader().getAddress());
                request.addHeader(contactHeader);
            }
        }
        else
        {
            ContactHeader contactHeader = headerFactory.createContactHeader(from.getAddress());
            request.addHeader(contactHeader);
        }

        return request;
    }

    protected Request buildRequest(String type, SIPMessage message) throws ParseException, InvalidArgumentException, UnknownHostException, SipException {

        FromHeader from = message.getFromHeader();
        ToHeader to = message.getToHeader();
        String callId = this.updateCallId(((CallIdHeader) message.getHeader(CallIdHeader.NAME)).getCallId());
        CallIdHeader callIdHeader = headerFactory.createCallIdHeader(callId);

        CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(sequence++, type);

        MaxForwardsHeader maxForwardsHeader = headerFactory.createMaxForwardsHeader(MAX_FORWARDS);

        //ViaList viaHeaders = sipRequest.getViaHeaders();
        Vector<ViaHeader> viaHeaders = new Vector<>();
        //ViaHeader viaHeader = headerFactory.createViaHeader(SERVER_HOST, SERVER_PORT, transportParam, null);
        if(viaHeaders!=null) {
            String transportParam = this.getTransport();
            ViaHeader via = headerFactory.createViaHeader(SERVER_HOST.getHostAddress(), this.getServerPort(), transportParam, null);
            via.setBranch(createBranch());
            viaHeaders.add(0, via);
        }

        URI uri = to.getAddress().getURI();

        Request request = messageFactory.createRequest(uri,
                type,
                callIdHeader,
                cSeqHeader,
                from,
                to,
                viaHeaders,
                maxForwardsHeader
        );

        createHeaders((SIPMessage) request,message.getHeader(UserAgent.NAME)==null?null:
                (String)((UserAgentHeader)message.getHeader(UserAgent.NAME)).getProduct().next());

        if(type.equals(Request.PRACK))
        {
            RSeqHeader rseq = headerFactory.createRSeqHeader((int)message.getCSeq().getSeqNumber());
            request.addHeader(rseq);
        }

        if(message.getContactHeader()!=null) {
            ContactHeader contactHeader = headerFactory.createContactHeader(message.getContactHeader().getAddress());
            request.addHeader(contactHeader);
        }

        /*
        RouteList routes = message.getRouteHeaders();
        if(routes==null)
            routes = new RouteList();
        SipUri localUri = new SipUri();
        localUri.setHost(RouteManager.KOWAL_HOST);
        localUri.setPort(RouteManager.KOWAL_PORT);
        AddressImpl local = new AddressImpl();
        local.setAddess(localUri);
        routes.add(0, new Route(local));
        request.addHeader(routes);
        */

        return request;
    }

    private RouteList addRoute(SIPMessage message, Route route){
        RouteList routes = message.getRouteHeaders();
        if(routes==null)
            routes = new RouteList();
        SipUri localUri = (SipUri)route.getAddress().getURI().clone();
        AddressImpl local = new AddressImpl();
        local.setAddess(localUri);
        routes.add(0, new Route(local));
        return routes;
    }

    private SipURI getSipUri(SipUser to) throws ParseException {
        String localHost = this.SERVER_HOST.getHostAddress() + ":" + this.getServerPort();
        return to.getDomain() != null ? addressFactory.createSipURI(to.getUserName(), to.getDomain()) : addressFactory.createSipURI(to.getUserName(), localHost);
    }

    private <T extends Message> void log(String direction, T message) {
        log(direction, message, null);
    }

    private <T extends Message> void log(String direction, T message, String action) {
        String callId = ((CallIdHeader) message.getHeader(CallIdHeader.NAME)).getCallId();
        String method = message instanceof SIPRequest ? ((SIPRequest) message).getMethod() : ((SIPResponse) message).getReasonPhrase();
        String from = message instanceof SIPRequest ? ((SIPRequest) message).getFrom().getAddress().toString() : ((SIPResponse) message).getFrom().getAddress().toString();
        String to = message instanceof SIPRequest ? ((SIPRequest) message).getTo().getAddress().toString() : ((SIPResponse) message).getTo().getAddress().toString();
        String format = String.format("CallId: %s, From: %s, To: %s", callId, from, to);
        logger.info(direction +"\t " + method + "\t " + format);
        logger.fine(message.toString());
    }
    //endregion

    //region Public Methods

    public void reinitSipStack() throws InvalidArgumentException, SipException, TooManyListenersException {
        //Se vió que hay casos en que el sip stack de JainSip deja de enviarnos los eventos SIP q recibe, para solucionar tal inconveniente se expone este método, el cual reinicia el sip stack.
        synchronized (properties) {
            disposeSipStack();
            initSipStack();
        }

    }

    public void close() {

        disposeSipStack();

        running = false;
    }

    private void disposeSipStack() {
        if (provider != null) {
            provider.removeSipListener(sipListener);
            try {
                provider.removeListeningPoint(listeningEndPoint);
                if (sipStack != null) {
                    sipStack.deleteListeningPoint(listeningEndPoint);
                    sipStack.deleteSipProvider(provider);
                }
                logger.info("managment Finished");
            } catch (ObjectInUseException e) {
                logger.severe("Could not remove listening endpoint");
            }
            sipStack = null;
            provider = null;
            listeningEndPoint = null;
        }
        //liberamos el puerto q habíamos pedido.
        //NetUtils.freePorts(this);
    }

    private String getRegistrationUri(FromHeader fromHeader) {
        String scheme = fromHeader.getAddress().getURI().getScheme();
        String userName = ((SipURI) fromHeader.getAddress().getURI()).getUser();
        return String.format("%s:%s", scheme, userName);
    }

    public void sendACK(ResponseEvent responseEvent) throws ParseException, InvalidArgumentException, SipException, UnknownHostException {

        SIPRequest request = (SIPRequest) buildRequest( Request.ACK, (SIPMessage)responseEvent.getResponse());
        long cSeq = ((SIPResponse)responseEvent.getResponse()).getCSeq().getSeqNumber();
        request.getCSeq().setSeqNumber(cSeq);

        Dialog dialog = responseEvent.getClientTransaction() != null ? responseEvent.getClientTransaction().getDialog() : responseEvent.getDialog();
        if(dialog==null) {
            //sendRequest(request);
            return;
        }
        dialog.sendAck(request);
    }

    public void sendACK(ResponseEvent responseEvent, FromHeader from, ToHeader to) throws ParseException, InvalidArgumentException, SipException, UnknownHostException {

        SIPRequest request = (SIPRequest) buildRequest( Request.ACK, (SIPMessage)responseEvent.getResponse(), ((SIPMessage) responseEvent.getResponse()).getCallId().getCallId(), from, to);
        long cSeq = ((SIPResponse)responseEvent.getResponse()).getCSeq().getSeqNumber();
        request.getCSeq().setSeqNumber(cSeq);

        Dialog dialog = responseEvent.getClientTransaction() != null ? responseEvent.getClientTransaction().getDialog() : responseEvent.getDialog();
        if(dialog==null) {
            sendRequest(request);
            return;
        }
        dialog.sendAck(request);
    }
    public void sendTrying(CallEndPoint party) throws ParseException, SipException, InvalidArgumentException {

        String tag = this.createTag();
        party.getTo().setTag(tag);
        SIPResponse resp = (SIPResponse) this.buildResponse(Response.TRYING, party.getRequest());
        resp.setToTag(tag);
        this.sendResponse(resp);
    }


    public void sendCancel(CallEndPoint callEndPoint) throws ParseException, InvalidArgumentException, SipException, SdpException, UnknownHostException {
        try {
            Request request = buildCancel(callEndPoint.getRequest(), callEndPoint.getFrom(), callEndPoint.getTo());
            sendRequest(request);
        }catch(TransactionUnavailableException e)
        {
            logger.severe("ERROR sendCancel. " + e.getMessage());
        }
    }

    public void sendResponse(int responseCode, RequestEvent requestEvent, byte [] sessionBytes) throws ParseException, SipException, InvalidArgumentException {
        Response response = messageFactory.createResponse(responseCode, requestEvent.getRequest());
        if (sessionBytes != null) {
            response.setContent(sessionBytes, headerFactory.createContentTypeHeader("application", "sdp"));
            FromHeader fromHeader = (FromHeader) response.getHeader(FromHeader.NAME);
            //Agregamos también el header del Contact ya que sino no es un mensaje válido para contestar un invite.
            addContactHeader(response, fromHeader.getAddress());
        }

        sendResponse(response);
    }

    @Override
    public String toString() {
        return String.format("ServerHost:%s, ServerPort:%d, Running: %s, registrationInterval: %s",
                SERVER_HOST.getHostAddress(),
                this.getServerPort(),
                running,
                REGISTER_TIMEOUT);
    }

    public boolean getIsDelayOfferEnabled() {
        return DELAY_OFFER;
    }

    //endregion

}
