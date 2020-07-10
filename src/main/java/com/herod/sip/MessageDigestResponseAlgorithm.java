package com.herod.sip;

/**
 * Created by eugenio.voss on 21/6/2017.
 */
/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

import org.apache.log4j.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The class takes standard Http Authentication details and returns a response according to the MD5 algorithm
 *
 * @author Emil Ivov
 *
 *         This class is based org.mobicents.servlet.sip.catalina.security.authentication.MessageDigestResponseAlgorithm class
 *         from sip-servlet-as7 project, re-implemented for jboss as8 (wildfly) by:
 * @author kakonyi.istvan@alerant.hu
 */

public class MessageDigestResponseAlgorithm {
    private static final Logger logger = Logger.getLogger(com.herod.sip.MessageDigestResponseAlgorithm.class.getName());

    /**
     * Calculates an http authentication response in accordance with rfc2617.
     * <p>
     *
     * @param algorithm a string indicating a pair of algorithms (MD5 (default), or MD5-sess) used to produce the digest and a
     *        checksum.
     * @param username_value username_value (see rfc2617)
     * @param realm_value A string that has been displayed to the user in order to determine the context of the username and
     *        password to use.
     * @param passwd the password to encode in the challenge response.
     * @param nonce_value A server-specified data string provided in the challenge.
     * @param cnonce_value an optional client-chosen value whose purpose is to foil chosen plaintext attacks.
     * @param method the SIP method of the request being challenged.
     * @param digest_uri_value the value of the "uri" directive on the Authorization header in the request.
     * @param entity_body the entity-body
     * @param qop_value Indicates what "quality of protection" the client has applied to the message.
     * @param nc_value the hexadecimal count of the number of requests (including the current request) that the client has sent
     *        with the nonce value in this request.
     * @return a digest response as defined in rfc2617
     * @throws NullPointerException in case of incorrectly null parameters.
     */
    public static synchronized String calculateResponse(String algorithm, String username_value, String realm_value, String passwd,
                                                        String nonce_value, String nc_value, String cnonce_value, String method, String digest_uri_value,
                                                        String entity_body, String qop_value) {
        if (logger.isDebugEnabled()) {
            logger.debug("trying to authenticate using : " + algorithm + ", " + username_value + ", " + realm_value + ", "
                    + (passwd != null && passwd.trim().length() > 0) + ", " + nonce_value + ", " + nc_value + ", "
                    + cnonce_value + ", " + method + ", " + digest_uri_value + ", " + entity_body + ", " + qop_value);
        }
        if (username_value == null || realm_value == null || passwd == null || method == null || digest_uri_value == null
                || nonce_value == null)
            throw new NullPointerException("Null parameter to MessageDigestAlgorithm.calculateResponse()");

        // The following follows closely the algorithm for generating a response
        // digest as specified by rfc2617
        String A1 = null;

        if (algorithm == null || algorithm.trim().length() == 0 || algorithm.trim().equalsIgnoreCase("MD5")) {
            A1 = username_value + ":" + realm_value + ":" + passwd;
        } else {
            if (cnonce_value == null || cnonce_value.length() == 0)
                throw new NullPointerException("cnonce_value may not be absent for MD5-Sess algorithm.");

            A1 = H(username_value + ":" + realm_value + ":" + passwd) + ":" + nonce_value + ":" + cnonce_value;
        }

        String A2 = null;
        if (qop_value == null || qop_value.trim().length() == 0 || qop_value.trim().equalsIgnoreCase("auth")) {
            A2 = method + ":" + digest_uri_value;
        } else {
            if (entity_body == null)
                entity_body = "";
            A2 = method + ":" + digest_uri_value + ":" + H(entity_body);
        }

        String request_digest = null;

        if (cnonce_value != null && qop_value != null && nc_value != null
                && (qop_value.equalsIgnoreCase("auth") || qop_value.equalsIgnoreCase("auth-int"))) {
            request_digest = KD(H(A1), nonce_value + ":" + nc_value + ":" + cnonce_value + ":" + qop_value + ":" + H(A2));
            if (logger.isDebugEnabled()) {
                logger.debug("H(A1) " + H(A1));
                logger.debug("H(A2) " + H(A2));
                logger.debug("request digest with auth " + request_digest);
            }
        } else {
            request_digest = KD(H(A1), nonce_value + ":" + H(A2));
            if (logger.isDebugEnabled()) {
                logger.debug("request digest " + request_digest);
            }
        }

        return request_digest;
    }

    public static synchronized String calculateResponse(String algorithm, String hA1, String nonce_value, String nc_value, String cnonce_value,
                                                        String method, String digest_uri_value, String entity_body, String qop_value) {

        if (logger.isDebugEnabled()) {
            logger.debug("trying to authenticate using : " + algorithm + ", " + hA1 + ", " + nonce_value + ", " + nc_value
                    + ", " + cnonce_value + ", " + method + ", " + digest_uri_value + ", " + entity_body + ", " + qop_value);
        }
        if (hA1 == null || method == null || digest_uri_value == null || nonce_value == null)
            throw new NullPointerException("Null parameter to MessageDigestAlgorithm.calculateResponse()");

        String A2 = null;
        if (qop_value == null || qop_value.trim().length() == 0 || qop_value.trim().equalsIgnoreCase("auth")) {
            A2 = method + ":" + digest_uri_value;
        } else {
            if (entity_body == null)
                entity_body = "";
            A2 = method + ":" + digest_uri_value + ":" + H(entity_body);
        }

        String request_digest = null;

        if (cnonce_value != null && qop_value != null && nc_value != null
                && (qop_value.equalsIgnoreCase("auth") || qop_value.equalsIgnoreCase("auth-int"))) {
            request_digest = KD(hA1, nonce_value + ":" + nc_value + ":" + cnonce_value + ":" + qop_value + ":" + H(A2));
            if (logger.isDebugEnabled()) {
                logger.debug("H(A1) " + hA1);
                logger.debug("H(A2) " + H(A2));
                logger.debug("request digest with auth " + request_digest);
            }
        } else {
            request_digest = KD(hA1, nonce_value + ":" + H(A2));
            if (logger.isDebugEnabled()) {
                logger.debug("request digest " + request_digest);
            }
        }

        return request_digest;
    }

    /**
     * Defined in rfc 2617 as H(data) = MD5(data);
     *
     * @param data data
     * @return MD5(data)
     */
    public static String H(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            return toHexString(digest.digest(data.getBytes()));
        } catch (NoSuchAlgorithmException ex) {
            // shouldn't happen
            logger.error("Failed to instantiate an MD5 algorithm", ex);
            return null;
        }
    }

    /**
     * Defined in rfc 2617 as KD(secret, data) = H(concat(secret, ":", data))
     *
     * @param data data
     * @param secret secret
     * @return H(concat(secret, ":", data));
     */
    private static String KD(String secret, String data) {
        return H(secret + ":" + data);
    }

    // the following code was copied from the NIST-SIP instant
    // messenger (its author is Olivier Deruelle). Thanks for making it public!
    /**
     * to hex converter
     */
    private static final char[] toHex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * Converts b[] to hex string.
     *
     * @param b the bte array to convert
     * @return a Hex representation of b.
     */
    public static String toHexString(byte[] b) {
        int pos = 0;
        char[] c = new char[b.length * 2];
        for (int i = 0; i < b.length; i++) {
            c[pos++] = toHex[(b[i] >> 4) & 0x0F];
            c[pos++] = toHex[b[i] & 0x0f];
        }
        return new String(c);
    }
}
