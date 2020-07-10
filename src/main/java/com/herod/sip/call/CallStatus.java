package com.herod.sip.call;

/**
 * Created by eugenio.voss on 22/12/2016.
 *
 * Indica el estdo de la llamada.
 * no confundir con el ultimo paquete enviado. Los paquetes se manejan a nivel de transacciones.
 */
public enum CallStatus {
    idle,
    ringing,
    progress,//Session Progress
    answered,
    hold
}
