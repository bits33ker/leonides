package com.herod.leonides;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.herod.rtp.RtpCodecException;
import com.herod.leonides.call.RouteManager;
import org.junit.Before;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.FileSystems;
import java.util.Collections;
import java.util.Enumeration;

public class ConfigTest {
    RouteManager routeManager;
    JsonObject jsonObject;

    @Before
    public void loadConfig() throws FileNotFoundException {
        JsonParser parser = new JsonParser();
        String p = FileSystems.getDefault().getPath("mconf/leonides.json").toAbsolutePath().toString();
        try {
            FileReader fp = new FileReader(p);
            //logger.info("load leonides.Json " + p);
            jsonObject = (JsonObject) parser.parse(fp);
        }catch(FileNotFoundException fnf){
            p = FileSystems.getDefault().getPath("../mconf/leonides.json").toAbsolutePath().toString();
            //logger.info("load Karen.Json" + p);
            jsonObject = (JsonObject) parser.parse(new FileReader(p));
        }
    }
    @Test
    public void configRouteManager() throws IOException, RtpCodecException {
        if (routeManager == null) {
            routeManager = new RouteManager();
            //routeManager.init(karenProperties);
            if (jsonObject == null) {
                loadConfig();
            }
            //logger.info("init " + RouteManager.class.getName());
            routeManager.init(jsonObject);
        }
    }
    @Test
    public void checkIPv4Interfaces() throws SocketException {
        InetAddress host = null;
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets)) {
            if(netint.isLoopback() || !netint.isUp())
                continue;
            if(netint.isVirtual())
                continue;

            Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
            while(inetAddresses.hasMoreElements()) {
                host = inetAddresses.nextElement();
                if(!(host instanceof Inet4Address))
                    continue;
                System.out.println(host.getHostName() + " " + host.getHostAddress());
            }
        }
    }
}
