package com.herod.leonides.service;

import com.herod.leonides.LeonidesServer;
import com.herod.leonides.call.RouteCall;
import com.herod.leonides.udp.ClusterManager;
import com.herod.rtp.RtpCodec;
import com.herod.rtp.RtpCodecException;
import com.herod.leonides.call.RouteManager;
import com.herod.sip.SipInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class LeonidesRestServiceImpl implements LeonidesRestService {
    private final static Logger logger = Logger.getLogger(LeonidesRestServiceImpl.class.getName());

    @Autowired
    LeonidesServer leonidesServer;
    @Autowired
    RouteManager routeManager;
    @Autowired
    ClusterManager clusterManager;

    @Override
    public List<SipInterface> getSipInterfaces() {
        logger.fine("getSipInterfaces");
        //List<SipInterface> list = new ArrayList<SipInterface>();
        //list.add(leonidesServer.getSipInterface());
        return leonidesServer.getSipInterfaces();
    }

    @Override
    public List<RtpCodec> getCodecs() {
        logger.fine("getCodecs");
        return routeManager.getCodecs();
    }

    @Override
    public void addCodec(RtpCodec codec) throws RtpCodecException {
        logger.fine("addCodec");
        routeManager.addCodec(codec.getMime().toString() + Integer.toString(codec.getFreq()), codec.getMime().getName());
    }

    @Override
    public boolean findCodec(RtpCodec codec) {
        List<RtpCodec> codecs = routeManager.getCodecs();
        for(RtpCodec c : codecs)
        {
            if(codec.getFreq()==c.getFreq() && codec.getMedia()==c.getMedia())
                return true;
        }
        return false;
    }

    @Override
    public Optional<RtpCodec> findCodecByMedia(long media) {
        List<RtpCodec> codecs = routeManager.getCodecs();
        for(RtpCodec c : codecs)
        {
            if(c.getMedia()==(int)media)
                return Optional.of(c);
        }
        return Optional.empty();
    }

    @Override
    public void deleteCodec(RtpCodec codec) throws RtpCodecException {
        logger.fine("deleteCodec");
        routeManager.getCodecs().remove(codec);
    }

    @Override
    public List<RouteCall> getRoutes() {
        logger.fine("getRoutes");
        return routeManager.listRoutes();
    }

    @Override
    public Optional<RouteCall> findById(Long id) {
        //return (Optional<Sitio>) sitioDao.findById(id);
        Optional<RouteCall> opt = Optional.empty();
        List<RouteCall>list = getRoutes();
        for (RouteCall r: list) {
            if(r.getId()==id)
                return Optional.of(r);
        }
        return opt;
    }

    @Override
    public void updateRoute(Long id, RouteCall route) {
        //routeManager.addRoute(route.toString());
    }

    @Override
    public void addRoute(String r) throws RtpCodecException, ParseException {
        routeManager.addRoute(r.toString());
    }

    @Override
    public List<String> getUsers() {
        logger.fine("getUsers");
        return routeManager.listAbonados();
    }

    @Override
    public List<String> getWhiteList() {
        logger.fine("getWhiteList");
        return routeManager.getWhiteList();
    }

    @Override
    public List<String> getBlackList() {
        logger.fine("getBlackList");
        return routeManager.getBlackList();
    }
}
