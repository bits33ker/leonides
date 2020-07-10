package com.herod.sip;

import com.google.gson.annotations.SerializedName;
import gov.nist.javax.sip.header.AddressParametersHeader;
import java.util.UUID;
import javax.sip.address.Address;
import javax.sip.address.URI;

public class SipUser {
    private static final String DOMAIN_DELIMITER = "@";
    @SerializedName("displayName")
    private String displayName;
    @SerializedName("userName")
    private String userName;
    @SerializedName("domain")
    private String domain;
    private String tag;
    private String displayId;

    public SipUser(String userName, String displayName, String domain, String tag) {
        this.init(userName, displayName, domain, tag);
    }

    public SipUser(String userName) {
        this.init(userName, (String)null, (String)null, UUID.randomUUID().toString());
    }

    public SipUser(String userName, String displayName, String domain) {
        this.init(userName, displayName, domain, UUID.randomUUID().toString());
    }

    public SipUser(String userName, String domain) {
        this.init(userName, (String)null, domain, UUID.randomUUID().toString());
    }

    public static SipUser buildSipUserFromString(String value, String defaultDomain) {
        String[] userAndDomain = value.split("@");
        return userAndDomain.length > 1 ? new SipUser(userAndDomain[0], userAndDomain[1]) : new SipUser(userAndDomain[0], defaultDomain);
    }

    public static SipUser buildSipUserContactHeader(AddressParametersHeader contactHeader) {
        Address address = contactHeader.getAddress();
        String name = getSipUserName(address.getURI());
        String domain = getSipDomain(address.getURI());
        String displayName = address.getDisplayName();
        String tag = contactHeader.hasParameter("tag") ? contactHeader.getParameter("tag") : UUID.randomUUID().toString();
        return new SipUser(name, displayName, domain, tag);
    }

    public static SipUser buildSipUserFromAddress(Address address) {
        String name = getSipUserName(address.getURI());
        String domain = getSipDomain(address.getURI());
        String displayName = address.getDisplayName();
        return new SipUser(name, displayName, domain);
    }

    public static boolean areSame(SipUser a, SipUser b) {
        return a.getUserName() != null && b.getUserName() != null && a.getUserName().equalsIgnoreCase(b.getUserName()) && (a.getDomain() != null && b.getDomain() != null && a.getDomain().equals(b.getDomain()) || a.getDomain() == null && b.getDomain() == null);
    }

    public static String getSipUserName(URI uri) {
        String toSplit = uri.toString();
        if (toSplit.startsWith("sip:")) {
            toSplit = toSplit.substring(4);
        }

        return toSplit.split("@")[0];
    }

    public static String getSipDomain(URI uri) {
        String toSplit = uri.toString();
        if (toSplit.startsWith("sip:")) {
            toSplit = toSplit.substring(4);
        }

        return toSplit.split("@")[1];
    }

    public static String getSipTag(URI uri) {
        String toSplit = uri.toString();
        if (toSplit.startsWith("sip:")) {
            toSplit = toSplit.substring(4);
        }

        return toSplit.split("@")[1];
    }

    private void init(String userName, String displayName, String domain, String tag) {
        this.userName = userName;
        this.displayName = displayName;
        this.domain = domain;
        this.tag = tag;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDomain() {
        return this.domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTag() {
        return this.tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDisplayId() {
        return this.displayId;
    }

    public String toString() {
        return this.domain != null ? String.format("%s%s%s", this.userName, "@", this.domain) : this.userName;
    }
}
