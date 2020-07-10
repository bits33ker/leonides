package com.herod.sip.call;

import com.herod.sip.SipServer;
import gov.nist.javax.sip.address.AddressImpl;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.message.SIPRequest;
import gov.nist.javax.sip.message.SIPResponse;

import javax.sdp.*;
import javax.sip.PeerUnavailableException;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

/**
 * Created by eugenio.voss on 6/12/2016.
 * Contiene toda la info necesaria de una sesion de llamada.
 *
 * Una llamada tiene 2 partes: la sesion inbound (hacia la PBX), y la outbound (desde la< PBX).
 * A su vez, cada sesion se encuentra dividida en transacciones (INVITE, OPTIONS, BYE) que comienzan
 * con un REQUEST.
 *
 * UAC1----------------------------->PBX----------------------------->UAC2
 *     AddressParty1(Sesion Inbound)    AddressParty2(Sesion Outbound)
 *     Transaccion INVITE               Transaccion INVITE
 *     Transaccion OPTIONS              Transaccion OPTIONS
 *     Transaccion BYE                  Transaccion BYE
 */
public class CallEndPoint {
    private final String HEADER_100REL = "100rel";

    /*datos propios de la conexion
    callId
    Via
    From
    To
    Contact
    Seq
    Pasan al siguiente PartyAdress:
    Max-forwards: se decrementa
    User-Agent
    Supported y Requires?
     */

    SipServer server;
    private SIPRequest request = null;
    private FromHeader from = null;
    private ToHeader to = null;
    private Via via = null;
    private Contact contact = null;
    boolean delayOffer = false;

    private boolean inbound = false;

    String callId;

    private Vector<SIPHeader> supported;
    private Vector<SIPHeader> require;

    public CallEndPoint(From fromAddr, To toAddr, boolean inbound, String callId){
        from = fromAddr;
        to = toAddr;
        this.inbound = inbound;
        this.callId = callId;
    }

    public CallEndPoint(SIPRequest request, boolean inbound)
    {
        this.request = request;
        try {
            via = request.getTopmostVia();
            /*
            if(via!=null){
                AddressImpl addr = new AddressImpl();
                addr.setAddressType(1);
                SipURI uri =new SipUri();
                addr.setAddess(uri);
                uri.setHost(request.getViaHost());
                uri.setPort(request.getViaPort());
                uri.setUser(((From)request.getFrom()).getUserAtHostPort().split("@")[0]);
                from = new From();
                from.setAddress(addr);
                if(((From)request.getFrom()).getTag()!=null)
                    from.setTag(((From)request.getFrom()).getTag());
            }
            else*/
            from = cloneFromHeader((From) request.getFrom());
            to = this.cloneToHeader((To)request.getTo());
            contact= request.getContactHeader();

            String to = this.request.getTo().getAddress().toString();
            this.inbound = inbound;
            callId = request.getCallId().getCallId();
            //si es inbound el puerto que viene en el REQUEST es el remote, sino cuando creamos el REQUEST hay que ponerle el local
            ListIterator<SIPHeader> itsup= request.getHeaders("Supported");
            supported = new Vector<SIPHeader>();
            require = new Vector<SIPHeader>();
            if(itsup!=null)
            {
                while(itsup.hasNext())
                {
                    SIPHeader header = itsup.next();
                    if(header.getValue().equals(HEADER_100REL))
                        supported.add(header);//el unico que soporto por ahora
                }
            }
            ListIterator<SIPHeader> itreq= request.getHeaders("Require");
            if(itreq!=null)
            {
                while(itreq.hasNext())
                {
                    SIPHeader header = itreq.next();
                    require.add(header);
                    if(header.getValue().equals(HEADER_100REL))
                        require.add(header);
                }
            }
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }
    public SipServer getServer()
    {
        return server;
    }
    public void setServer(SipServer s)
    {
        server = s;
    }
    public Contact getContact(){return contact;}
    public Via getVia(){return via;}

    private ToHeader cloneToHeader(To toHeader) throws ParseException {
        AddressImpl address = new AddressImpl();
        address.setAddressType(1);
        SipURI uri =new SipUri();
        address.setAddess(uri);
        uri.setHost(toHeader.getHostPort().getHost().getAddress());
        uri.setPort(toHeader.getHostPort().getPort());
        uri.setUser(toHeader.getUserAtHostPort().split("@")[0]);

        to = new To();
        to.setAddress(address);
        if(toHeader.getTag()!=null)
            to.setTag(toHeader.getTag());
        return to;
    }

    private FromHeader cloneFromHeader(From fromHeader) throws ParseException {
        AddressImpl address = new AddressImpl();
        address.setAddressType(1);
        SipURI uri =new SipUri();
        address.setAddess(uri);
        uri.setHost(fromHeader.getUserAtHostPort().split("@")[1]);
        uri.setPort(fromHeader.getHostPort().getPort());
        uri.setUser(fromHeader.getUserAtHostPort().split("@")[0]);

        from = new From();
        from.setAddress(address);
        if(fromHeader.getTag()!=null)
            from.setTag(fromHeader.getTag());
        return from;
    }

    public static From cloneFrom(Address fromAddr, String fromTag, SipUri uri) throws ParseException, SipException {
        From from = new From();
        if(from!=null)
        {
            AddressImpl addr = new AddressImpl();
            addr.setURI(uri);
            addr.setUser(((SipUri)fromAddr.getURI()).getUser());
            from.setAddress(addr);
            if(fromTag==null)
                fromTag = SipServer.createTag();
            from.setTag(fromTag);
        }
        else
            throw new SipException("ERROR setting From");
        return from;
    }

    public static To cloneTo(Address toAddr, String toTag) throws ParseException, SipException {
        To to = new To();
        if(to!=null)
        {
            AddressImpl addr = (AddressImpl)toAddr.clone();
            to.setAddress(addr);
            if(toTag!=null)
                to.setTag(toTag);
        }
        else
            throw new SipException("Error creando CallEndPoint");
        return to;
    }

    public FromHeader getFrom() {
        return from;
    }

    public ToHeader getTo() {
        return to;
    }

    public String getCallId(){
        return callId;
    }
    /*
    public String getSupported()
    {
        if(supported.size()==0)
            return null;
        String res = "Supported:";
        for (SIPHeader header:supported) {
            res += header.getValue() + ",";
        }
        return res.substring(0, res.length()-1) + "\r\n";
    }*/

    public List<SIPHeader> getSupported()
    {
        return supported.subList(0, supported.size());
    }
    public void setSupported(List<SIPHeader> supp)
    {
        ListIterator<SIPHeader> itsup= supp.listIterator();
        if(supported==null)supported = new Vector<SIPHeader>();
        if(itsup!=null)
        {
            while(itsup.hasNext())
            {
                SIPHeader header = itsup.next();
                if(header.getValue().equals(HEADER_100REL))
                    supported.add(header);//el unico que soporto por ahora
            }
        }
    }
    public void setRequire(ListIterator<SIPHeader> itreq)
    {
        if(require==null)require = new Vector<SIPHeader>();
        if(itreq!=null)
        {
            while(itreq.hasNext())
            {
                SIPHeader header = itreq.next();
                if(header.getValue().equals(HEADER_100REL))
                    require.add(header);//el unico que soporto por ahora
            }
        }
    }
    public boolean require100rel()
    {
        if(require==null)
            return false;
        ListIterator<SIPHeader> itreq = require.listIterator();
        if(itreq!=null)
        {
            while(itreq.hasNext())
            {
                SIPHeader header = itreq.next();
                if(header.getValue().equals(HEADER_100REL))
                    return true;//el unico que soporto por ahora
            }
        }
        return false;
    }
    public boolean isInbound() {
        return inbound;
    }

    public SIPRequest getRequest() {
        return request;
    }

    public void setRequest(SIPRequest req){
        request = req;
        callId = req.getCallId().getCallId();
    }

    public boolean isDelayOffer() {
        return delayOffer;
    }

    public SIPResponse createResponse(MediaDescription mediaResponseDescription, int sipCode)throws UnsupportedEncodingException, PeerUnavailableException, ParseException
    {
        SIPResponse resp1 = request.createResponse(sipCode);
        if(mediaResponseDescription!=null)
        {
            resp1.setContent(mediaResponseDescription, SipFactory.getInstance().createHeaderFactory().createContentTypeHeader("application", "sdp"));
        }
        return resp1;
    }

    public String getReplace()
    {
        return this.getCallId() + ";from-tag=" + this.getFrom().getTag() + ";to-tag=" + this.getTo().getTag();
    }
}
