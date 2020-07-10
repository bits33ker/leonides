package com.herod.sip;

import gov.nist.javax.sip.header.*;
import gov.nist.javax.sip.message.SIPRequest;

import javax.sip.address.SipURI;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Date;
import java.util.Random;

/**
 * Created by eugenio.voss on 11/12/2018.
 */
public class MitrolAuthHeader extends AuthenticationHeader {
    /** to hex converter */
    private static final char[] toHex = { '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static final String DEFAULT_ALGORITHM = "MD5";
    public static final String DEFAULT_SCHEME = "Digest";

    private MessageDigest messageDigest;

    public MitrolAuthHeader(String name) throws NoSuchAlgorithmException {
        super(name);
        messageDigest = MessageDigest.getInstance(DEFAULT_ALGORITHM);
    }
    @Override
    public StringBuilder encodeBody(StringBuilder buffer) {
        this.parameters.setSeparator(",");
        buffer = buffer.append(this.scheme).append(SP);
        return parameters.encode(buffer);
    }
    public String generateNonce() {
        // Get the time of day and run MD5 over it.
        Date date = new Date();
        long time = date.getTime();
        Random rand = new Random();
        long pad = rand.nextLong();
        String nonceString = (new Long(time)).toString()
                + (new Long(pad)).toString();
        byte mdbytes[] = messageDigest.digest(nonceString.getBytes());
        // Convert the mdbytes array into a hex string.
        return toHexString(mdbytes);
    }

    public static String toHexString(byte b[]) {
        int pos = 0;
        char[] c = new char[b.length * 2];
        for (int i = 0; i < b.length; i++) {
            c[pos++] = toHex[(b[i] >> 4) & 0x0F];
            c[pos++] = toHex[b[i] & 0x0f];
        }
        return new String(c);
    }

    public static String registerAuth(SIPRequest request,
                                      String realm,
                                      String nonce,
                                      String opaque,
                                      String password) throws ParseException, NoSuchAlgorithmException, UnsupportedEncodingException {
        Authorization authorizationHeader = new Authorization();
        if(realm==null) realm = "";
        if(opaque==null)opaque ="";
        authorizationHeader.setUsername(((SipURI)request.getContactHeader().getAddress().getURI()).getUser());
        authorizationHeader.setRealm(realm);
        authorizationHeader.setNonce(nonce);
        authorizationHeader.setURI(request.getRequestURI());
        authorizationHeader.setOpaque(opaque);
        MessageDigest messageDigest = MessageDigest.getInstance("md5");
        String H1 = authorizationHeader.getUsername() + ":" + realm + ":" + password;
        H1 = com.herod.sip.MitrolAuthHeader.toHexString(messageDigest.digest(H1.getBytes("UTF-8")));
        String H2 = "REGISTER:" + request.getRequestURI().toString();
        H2 = com.herod.sip.MitrolAuthHeader.toHexString(messageDigest.digest(H2.getBytes("UTF-8")));
        String responseDisgest = H1 + ":" + nonce + ":" + H2;
        String md5Password = com.herod.sip.MitrolAuthHeader.toHexString(messageDigest.digest(responseDisgest.getBytes("UTF-8")));
        authorizationHeader.setResponse(md5Password);
        request.addHeader(authorizationHeader);
        return md5Password;//response
    }
    public static com.herod.sip.MitrolAuthHeader challengeRegister() throws NoSuchAlgorithmException, ParseException {
        com.herod.sip.MitrolAuthHeader authenticationHeader = new com.herod.sip.MitrolAuthHeader(WWWAuthenticate.NAME);
        authenticationHeader.setScheme(com.herod.sip.MitrolAuthHeader.DEFAULT_SCHEME);
        authenticationHeader.setParameter(AuthenticationHeader.REALM, "");
        authenticationHeader.setNonce(authenticationHeader.generateNonce());
        authenticationHeader.setParameter(AuthenticationHeader.OPAQUE, "");
        authenticationHeader.setParameter(AuthenticationHeader.ALGORITHM, com.herod.sip.MitrolAuthHeader.DEFAULT_ALGORITHM);
        return authenticationHeader;
    }
    public static com.herod.sip.MitrolAuthHeader challengeInvite() throws NoSuchAlgorithmException, ParseException {
        com.herod.sip.MitrolAuthHeader authenticationHeader = new com.herod.sip.MitrolAuthHeader(ProxyAuthenticate.NAME);
        authenticationHeader.setScheme(com.herod.sip.MitrolAuthHeader.DEFAULT_SCHEME);
        authenticationHeader.setParameter(AuthenticationHeader.REALM, "");
        authenticationHeader.setNonce(authenticationHeader.generateNonce());
        authenticationHeader.setParameter(AuthenticationHeader.OPAQUE, "");
        authenticationHeader.setParameter(AuthenticationHeader.ALGORITHM, com.herod.sip.MitrolAuthHeader.DEFAULT_ALGORITHM);
        return authenticationHeader;
    }
    public static String inviteAuth(SIPRequest request,
                                    String realm,
                                    String nonce,
                                    String opaque,
                                    String password) throws ParseException, NoSuchAlgorithmException, UnsupportedEncodingException {
        ProxyAuthorization authorizationHeader = new ProxyAuthorization();
        if(realm==null) realm = "";
        if(opaque==null)opaque ="";
        authorizationHeader.setUsername(((SipURI)request.getContactHeader().getAddress().getURI()).getUser());
        authorizationHeader.setRealm(realm);
        authorizationHeader.setNonce(nonce);
        authorizationHeader.setURI(request.getRequestURI());
        authorizationHeader.setOpaque(opaque);
        MessageDigest messageDigest = MessageDigest.getInstance("md5");
        String H1 = authorizationHeader.getUsername() + ":" + realm + ":" + password;
        H1 = com.herod.sip.MitrolAuthHeader.toHexString(messageDigest.digest(H1.getBytes("UTF-8")));
        String H2 = "INVITE:" + request.getRequestURI().toString();
        H2 = com.herod.sip.MitrolAuthHeader.toHexString(messageDigest.digest(H2.getBytes("UTF-8")));
        String responseDisgest = H1 + ":" + nonce + ":" + H2;
        String md5Password = com.herod.sip.MitrolAuthHeader.toHexString(messageDigest.digest(responseDisgest.getBytes("UTF-8")));
        authorizationHeader.setResponse(md5Password);
        request.addHeader(authorizationHeader);
        return md5Password;//response
    }

}
