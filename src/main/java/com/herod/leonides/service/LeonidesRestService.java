package com.herod.leonides.service;

import com.herod.leonides.call.RouteCall;
import com.herod.rtp.RtpCodec;
import com.herod.rtp.RtpCodecException;
import com.herod.sip.SipInterface;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;

public interface LeonidesRestService {
    List<SipInterface> getSipInterfaces();
    List<RtpCodec> getCodecs();
    void addCodec(RtpCodec codec) throws RtpCodecException;
    boolean findCodec(RtpCodec codec);
    Optional<RtpCodec> findCodecByMedia(long media);
    void deleteCodec(RtpCodec codec) throws RtpCodecException;
    List<RouteCall>getRoutes();
    Optional<RouteCall> findById(Long id);
    void updateRoute(Long id, RouteCall route);
    void addRoute(String r) throws RtpCodecException, ParseException;
    List<String> getUsers();
    List<String> getWhiteList();
    List<String> getBlackList();
}
