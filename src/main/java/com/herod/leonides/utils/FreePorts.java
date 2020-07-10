package com.herod.leonides.utils;

import org.apache.log4j.Logger;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Random;

/**
 * Created by eugenio.voss on 10/4/2017.
 * Chequea los puertos libres para Rtp
 */
public class FreePorts {
    private static Logger logger = Logger.getLogger(FreePorts.class);

    private int freeMediaPort=34000;
    final int freeMediaPortMin=34000;
    final int freeMediaPortMax=36000;
    private Random portNumberGenerator = new Random();

    public synchronized DatagramSocket getRandomPortNumber() {
        DatagramSocket resultSocket = null;
        //we'll first try to bind to a random port. if this fails we'll try
        //again (bindRetries times in all) until we find a free local port.
        int fp = portNumberGenerator.nextInt(freeMediaPortMax - freeMediaPortMin) + freeMediaPortMin;
        for (int i = 0; i < 5; i++) {
            try {
                resultSocket = new DatagramSocket(fp);
                //we succeeded - break so that we don't try to bind again
                break;
            }
            catch (SocketException exc) {
                if (exc.getMessage().indexOf("Address already in use") == -1) {
                    logger.fatal("An exception occurred while trying to create"
                            + "a local host discovery socket.", exc);
                    return null;
                }
                //port seems to be taken. try another one.
                logger.debug("Port " + fp + " seems in use.");
                fp = portNumberGenerator.nextInt(freeMediaPortMax - freeMediaPortMin) + freeMediaPortMin;
                logger.debug("Retrying bind on port " + fp);
            }
        }
        return resultSocket;
    }

    public synchronized int getFreeMediaPort() {
        DatagramSocket resultSocket = null;
        int fp = -1;
        for (int i = 0; i < 5; i++) {
            try {
                //chequeo puerto.
                fp = freeMediaPort++;
                if(freeMediaPort>=freeMediaPortMax)
                    freeMediaPort = freeMediaPortMin;
                resultSocket = new DatagramSocket(fp);
                break;
            }
            catch (SocketException exc) {
                if (exc.getMessage().indexOf("Address already in use") == -1) {
                    logger.fatal("An exception occurred while trying to create"
                            + "a local host discovery socket.", exc);
                    return -1;
                }
                //port seems to be taken. try another one.
                logger.debug("Puerto " + fp + " en uso");
                fp = -1;
            }
        }
        if(resultSocket!=null)resultSocket.close();
        return fp;
    }
}
