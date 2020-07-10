package com.herod.leonides.utils;

/**
 * Created by eugenio.voss on 25/1/2017.
 */
public class LeonidesUtils {
    //configuracion de SipServer
    public static final String SERVER_ALLOW = "INVITE,ACK,CANCEL,BYE,REFER,OPTIONS";
    public static final String SERVER_VERSION = "Leonides 0.0.0.3";

    public static final String USER_AGENT = "com.herod.leonides.USER_AGENT";
    public static final String SERVER_HOST = "com.herod.leonides.server.HOST";
    public static final String UDP_PORT="com.herod.leonides.server.UDP.PORT";
    public static final String TCP_PORT="com.herod.leonides.server.TCP.PORT";
    public static final String TLS_PORT="com.herod.leonides.server.TLS.PORT";
    public static final String TRANSPORT_UDP = "com.herod.leonides.server.UDP";
    public static final String TRANSPORT_TCP = "com.herod.leonides.server.TCP";
    public static final String TRANSPORT_TLS = "com.herod.leonides.server.TLS";
    public static final String MAX_FORWARDS="com.herod.leonides.MAX_FORWARDS";
    public static final String RELAY_TIME="com.herod.leonides.RELAY_TIME";
    public static final String DELAY_OFFER="com.herod.leonides.DELAY_OFFER";
    public static final String IDENTIFIER="com.herod.leonides.IDENTIFIER";
    public static final String SIPSERVER_TYPE="com.herod.leonides.SipServerType";
    public static final String REGISTER_TIMEOUT="com.herod.leonides.timeout.REGISTER";
    public static final String INVITE_TIMEOUT="com.herod.leonides.timeout.INVITE";
    public static final String AUDIO_ENABLED="com.herod.leonides.AUDIO";
    public static final String VIDEO_ENABLED="com.herod.leonides.VIDEO";
    public static final String RTCP_ENABLED="com.herod.leonides.RTCP";
    public static final String CODECS_ENABLED="com.herod.leonides.CODECS";
    public static final String TRASCODING="com.herod.leonides.trascoding";
    public static final String MITE1X_CLUSTER="com.herod.leonides.clusters";//192.168.41.245:10410,192.168.41.227:10410
    public static final String MITE1X_CLUSTER_EXPIRES="com.herod.leonides.clusters.expires";//60
    public static final String MITE1X_CLUSTER_PORT="com.herod.leonides.clusters.port";//10410

    public static final String WHITELIST="com.herod.leonides.whitelist";//192.168.41.245:10410,192.168.41.227:10410
    public static final String BLACKLIST="com.herod.leonides.blacklist";//192.168.41.245:10410,192.168.41.227:10410
    public static final String ROUTELIST="com.herod.leonides.routelist";//192.168.41.245:10410,192.168.41.227:10410
    public static final String LICENCIA_ABONADOS="com.herod.leonides.licencias.abonados";
    public static final String LICENCIA_CANALES="com.herod.leonides.licencias.canales";

    public static final String PLAYER_USER = "com.herod.sip.player.USER";
    public static final String PLAYER_HOST = "com.herod.sip.player.HOST";//=192.168.40.34
    public static final String PLAYER_PORT = "com.herod.sip.player.PORT";//=5090
    public static final String PLAYER_TRANSPORT = "com.herod.sip.player.PROTOCOL";//=UDP
    public static final String PLAYER_AUDIO = "com.herod.sip.player.AUDIO";//=wavs/test.wav
    public static final String PLAYER_CALLS = "com.herod.leonides.player.CALLS";

}
