package com.herod.leonides.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.herod.leonides.LeonidesServer;
import com.herod.leonides.call.RouteManager;
import com.herod.leonides.udp.ClusterManager;
import com.herod.rtp.RtpDtmf;
import com.herod.sip.*;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by eugenio.voss on 30/1/2017.
 */
//@EnableWebMvc
//@ComponentScan("com.herod.leonides.controller")
//public class LeonidesContext extends WebMvcConfigurerAdapter implements DisposableBean{
@Configuration
@EnableWebSecurity
public class LeonidesContext extends WebSecurityConfigurerAdapter{
    private final static Logger logger = Logger.getLogger(LeonidesContext.class.getName());
    //private SipCallManager sipCallManager;//controlador del Sip Server
    private ClusterManager mite1xManager;//cluster para mite1x
    //private LicenseManager licenseManager;
    //private CtiManager ctiManager;
    private RouteManager routeManager;
    private LeonidesServer leonidesServer;
    //private Discador discador;

    Properties sipStackProperties = new Properties();//configuracion para el sipstack
    //Properties karenProperties = new Properties();//configuracion general
    JsonObject karenJson = null;

    @Autowired
    public void configureGlobalSecurity(AuthenticationManagerBuilder auth)throws Exception
    {
        //https://www.javacodemonk.com/spring-security-5-there-is-no-passwordencoder-mapped-for-the-id-b0503f3d
        auth.inMemoryAuthentication().withUser("ADMIN").password("{noop}ADMIN").roles("ADMIN");
        auth.inMemoryAuthentication().withUser("eugenio").password("{noop}voss").roles("USER");
    }

    @Override
    protected void configure(HttpSecurity http)throws Exception
    {
        //deshabilitamos acceso malicisioso. Como es para mi no hace falta.
        http.csrf().disable().authorizeRequests()
                .antMatchers("/api/codecs").permitAll()
                .antMatchers("/api/users").hasRole("USER")
                .antMatchers("/api/routes").hasRole("ADMIN")
                //.antMatchers("*/add").hasRole("ADMIN")
                //.antMatchers("*/delete").hasRole("ADMIN")
                .and().httpBasic();
    }

    private void loadConfig() throws FileNotFoundException {
        JsonParser parser = new JsonParser();
        String p = FileSystems.getDefault().getPath("mconf/leonides.json").toAbsolutePath().toString();
        try {
            FileReader fp = new FileReader(p);
            logger.info("load leonides.Json " + p);
            karenJson = (JsonObject) parser.parse(fp);
        }catch(FileNotFoundException fnf){
            p = FileSystems.getDefault().getPath("../mconf/leonides.json").toAbsolutePath().toString();
            logger.info("load Karen.Json" + p);
            karenJson = (JsonObject) parser.parse(new FileReader(p));
        }
    }


    @PostConstruct
    public void postContruct() {
            try {
                System.setProperty("java.net.preferIPv4Stack" , "true");
                if (sipStackProperties.isEmpty()) {
                String systemKarenProperties = System.getProperty("sip-stack.properties");
                InputStream inputStream = null;
                if (systemKarenProperties == null)
                    inputStream = this.getClass().getResourceAsStream("/sip-stack.properties");

                if(inputStream == null) {
                    try {
                        inputStream = new FileInputStream(FileSystems.getDefault().getPath("mconf/sip-stack.properties").toAbsolutePath().toString());
                    }catch(FileNotFoundException fnf){
                        inputStream = new FileInputStream(FileSystems.getDefault().getPath("../mconf/sip-stack.properties").toAbsolutePath().toString());
                    }
                }
                logger.info("load SipStack " + inputStream.toString());
                sipStackProperties.load(inputStream);
                inputStream.close();
                }
                if(karenJson == null)
                {
                    loadConfig();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
    }
/*
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/js/**").addResourceLocations("/js/").setCachePeriod(3600);
        //registry.addResourceHandler("/jsp/**").addResourceLocations("/jsp/").setCachePeriod(3600);
    }

 */
    @Bean
    public HttpMessageConverter jsonConverter(){
        MappingJackson2HttpMessageConverter jacksonConverter =
                new MappingJackson2HttpMessageConverter(new ObjectMapper());

        return jacksonConverter;
    }
/*
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.clear();
        converters.add(new Jaxb2RootElementHttpMessageConverter());
        converters.add(jsonConverter());
    }
*/
    @Bean
    public RouteManager getRouteManager() throws Exception {
        if (routeManager == null) {
            routeManager = new RouteManager();
            if(karenJson == null)
            {
                loadConfig();
            }
            logger.info("init " + RouteManager.class.getName());
            routeManager.init(karenJson);
            if(mite1xManager!=null) {
                routeManager.setClusterManager(mite1xManager);
                mite1xManager.setRouteManager(routeManager);
            }
        }
        return routeManager;
    }

    /*
    @Bean
    LicenseManager getLicenseManager() throws FileNotFoundException {
        if(karenJson==null)
            loadConfig();
        if(licenseManager==null){
            licenseManager = new LicenseManager((JsonObject) karenJson.get("licencia"));
            licenseManager.startLicenseClient();
        }
        return licenseManager;
    }

    @Bean
    CtiManager getCtiManager() throws Exception {
        if(karenJson==null)
            loadConfig();
        if(ctiManager==null){
            ctiManager = new CtiManager((JsonObject)karenJson.get("cti"));
            //ctiManager.startCtiClient();
        }
        return ctiManager;
    }
*/
    @Bean
    public ClusterManager getClusterManager() throws Exception {
        if (mite1xManager == null) {
            //mite1xManager = new ClusterManager(karenProperties.getProperty(LeonidesUtils.MITE1X_CLUSTER),
            //        Integer.parseInt(karenProperties.getProperty(LeonidesUtils.MITE1X_CLUSTER_EXPIRES, "60")),
            //        Integer.parseInt(karenProperties.getProperty(LeonidesUtils.MITE1X_CLUSTER_PORT, "10410")));
            if(karenJson == null)
            {
                loadConfig();
            }
            logger.info("implement " + ClusterManager.class.getName());
            mite1xManager = new ClusterManager((JsonObject) karenJson.get("clusters"));
        }
        if(routeManager!=null) {
            routeManager.setClusterManager(mite1xManager);
            mite1xManager.setRouteManager(routeManager);
        }
        return mite1xManager;
    }
/*
    @Bean
    public Discador getDiscador() throws Exception {
        String transporte = karenProperties.getProperty(LeonidesUtils.PLAYER_TRANSPORT, "UDP");
        if (discador == null) {
            //public PlayerSoftPhone(String user, Properties properties, List<RtpCodec> rtpMaps, List<DtmfSignal> dtmfSignals, SipListener listener) throws Exception {
            discador = new Discador(karenProperties.getProperty(LeonidesUtils.PLAYER_USER, "wavplayer"), Arrays.asList(new RtpCodec(RtpMime.PCMU, 0, 8000), new RtpCodec(RtpMime.G729, 18, 8000)));
            discador.init(sipStackProperties, null, discador, discador,
                    karenProperties.getProperty(LeonidesUtils.PLAYER_HOST, InetAddress.getLocalHost().getHostAddress()),
                    Integer.parseInt(karenProperties.getProperty(LeonidesUtils.PLAYER_PORT, "5090")),
                    transporte);
        }
        if(leonidesServer!=null) {
            discador.register(leonidesServer.selectSipServer(transporte).getHost(), leonidesServer.selectSipServer(transporte).getServerPort());
            String phonelist = karenProperties.getProperty(LeonidesUtils.PLAYER_CALLS, "");
            if(phonelist!="")
                discador.newCallList(phonelist);
        }
        return discador;
    }

    @Bean
    public Licencia getLicencia() throws Exception {
        if (licencia == null) {
            licencia = new Licencia();
            licencia.setMaxAbonados(Integer.parseInt(karenProperties.getProperty(LeonidesUtils.LICENCIA_ABONADOS, "10")));
            licencia.setMaxCanales(Integer.parseInt(karenProperties.getProperty(LeonidesUtils.LICENCIA_CANALES, "10")));
            if(leonidesServer!=null)
                leonidesServer.setLicencia(licencia);
        }
        return licencia;
    }
*/
    private InetAddress getInetAddress(JsonObject server) throws SocketException {
        InetAddress host = null;
        if(!(server.get("net") == null) && !server.get("net").isJsonNull()) {
            String netname = server.get("net").getAsString();
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                if(netint.getName().equals(netname)) {
                    Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                    if(inetAddresses.hasMoreElements()) {
                        do {
                            host = inetAddresses.nextElement();
                        } while (!(host instanceof Inet4Address) && inetAddresses.hasMoreElements());
                    }
                    break;
                }
            }
        }
        return host;
    }
    @Bean
    public LeonidesServer getleonidesServer() throws Exception {
        if (leonidesServer == null) {
            leonidesServer = new LeonidesServer();
            //leonidesServer.init(karenProperties, sipStackProperties, Arrays.asList(DtmfSignal.EVENT, DtmfSignal.IN_BAND));
            logger.info("init Karen " + LeonidesServer.class.getName());
            leonidesServer.init(karenJson, sipStackProperties, Arrays.asList(RtpDtmf.EVENT, RtpDtmf.IN_BAND));

            if(routeManager!=null)
                leonidesServer.setRouterManager(routeManager);
            JsonArray servers = (JsonArray) karenJson.get("servers");
            if (servers != null) {
                Iterator<JsonElement> iterator = servers.iterator();
                if (iterator.hasNext()) {
                    //tomo el primero y listo
                    JsonObject server = iterator.next().getAsJsonObject();
                    InetAddress host = null;//InetAddress.getLocalHost();
                    try {
                        if (server.get("protocol").getAsString().toUpperCase().equals("UDP")) {
                            SipCallManager sipCallManager = new SipCallManager();
                            if (sipCallManager != null) {
                                int port = 5060;
                                //if(!(server.get("ip") == null) && !server.get("ip").isJsonNull())
                                //    host = server.get("ip").getAsString();
                                host = getInetAddress(server);
                                if (!server.get("port").isJsonNull())
                                    port = server.get("port").getAsInt();
                                if (!(host == null)) {
                                    logger.info("init UDP " + host.getHostAddress() + ":" + port);
                                    sipCallManager.init(sipStackProperties, host, port, "UDP");
                                    SipInterface sipInterface = new SipInterface(server.get("id").getAsString(),
                                            server.get("protocol").getAsString(), server.get("net").getAsString(), host.getHostAddress(), server.get("port").getAsInt());
                                    leonidesServer.addSipCallManager(sipInterface, sipCallManager);
                                    sipCallManager.setLeonidesServer(leonidesServer);
                                }
                            }
                        }
                        if (server.get("protocol").getAsString().toUpperCase().equals("TCP")) {
                            SipCallManager sipCallManager = new SipCallManager();
                            if (sipCallManager != null) {
                                int port = 5061;
                                //if(!(server.get("ip") == null) && !server.get("ip").isJsonNull())
                                //    host = server.get("ip").getAsString();
                                host = getInetAddress(server);
                                if (!server.get("port").isJsonNull())
                                    port = server.get("port").getAsInt();
                                if (!(host == null)) {
                                    logger.info("init TCP " + host.getHostAddress() + ":" + port);
                                    sipCallManager.init(sipStackProperties, host, port, "TCP");
                                    SipInterface sipInterface = new SipInterface(server.get("id").getAsString(),
                                            server.get("protocol").getAsString(), server.get("net").getAsString(), host.getHostAddress(), server.get("port").getAsInt());
                                    leonidesServer.addSipCallManager(sipInterface, sipCallManager);
                                    sipCallManager.setLeonidesServer(leonidesServer);
                                }
                            }
                        }
                        if (server.get("protocol").getAsString().toUpperCase().equals("TLS")) {
                            SipCallManager sipCallManager = new SipCallManager();
                            if (sipCallManager != null) {
                                int port = 5062;
                                //if(!(server.get("ip") == null) && !server.get("ip").isJsonNull())
                                //    host = server.get("ip").getAsString();
                                host = getInetAddress(server);
                                if (!server.get("port").isJsonNull())
                                    port = server.get("port").getAsInt();
                                if (!(host == null)) {
                                    logger.info("init " + host.getHostAddress() + ":" + port);
                                    sipCallManager.init(sipStackProperties, host, port, "TLS");
                                    SipInterface sipInterface = new SipInterface(server.get("id").getAsString(),
                                            server.get("protocol").getAsString(), server.get("net").getAsString(), host.getHostAddress(), server.get("port").getAsInt());
                                    leonidesServer.addSipCallManager(sipInterface, sipCallManager);
                                    sipCallManager.setLeonidesServer(leonidesServer);
                                }
                            }
                        }
                    }catch(Exception e)
                    {
                        logger.severe("ERROR implementing SipCallManager: " + host.getHostAddress());
                    }

                }
            }
            else{
                InetAddress host = null;
                Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
                for (NetworkInterface netint : Collections.list(nets)) {
                    if(netint.isLoopback() || !netint.isUp())
                        continue;
                    if(netint.isVirtual())
                        continue;

                    Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                    while(inetAddresses.hasMoreElements()) {
                        try {
                            host = inetAddresses.nextElement();
                            if (!(host instanceof Inet4Address))
                                continue;
                            int port = 5060;
                            logger.info("init UDP " + host.getHostAddress() + ":" + port);
                            SipCallManager sipCallManager = new SipCallManager();
                            sipCallManager.init(sipStackProperties, host, port, "UDP");
                            SipInterface sipInterface = new SipInterface(netint.getDisplayName() + Integer.toString(port),
                                    "UDP", netint.getDisplayName(), host.getHostAddress(), port);
                            leonidesServer.addSipCallManager(sipInterface, sipCallManager);
                            sipCallManager.setLeonidesServer(leonidesServer);
                        }catch(Exception e)
                        {
                            logger.severe("ERROR implementing SipCallManager: " + host.getHostAddress());
                        }
                    }
                }
            }

        }
        return leonidesServer;
    }
/*
    @Bean
    public GsonHttpMessageConverter jsonConverter() {
        GsonHttpMessageConverter converter = new GsonHttpMessageConverter();
        converter.setGson(JsonMapper.getInstance().buildConverterGsonBuilder().create());
        return converter;
    }

    @Override
    public void destroy() throws Exception {
        //sipCallManager.destroy();
    }*/
}
