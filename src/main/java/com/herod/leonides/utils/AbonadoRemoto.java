package com.herod.leonides.utils;

import gov.nist.javax.sip.address.AddressImpl;

import javax.sip.address.Address;

/**
 * Created by eugenio.voss on 8/3/2017.
 */
public class AbonadoRemoto {
    private Address extAddress;//direccion del interno
    private Address pbxAddress;//direccion de la PBX a la que se encuentra registrado
    public AbonadoRemoto(Address e, Address p)
    {
        extAddress = e;
        pbxAddress = p;
    }

    public Address getExtAddress() {
        return extAddress;
    }

    public Address getPbxAddress() {
        return pbxAddress;
    }
    public String getUser(){
        return ((AddressImpl)extAddress).getUserAtHostPort().split("@")[0];
    }
}
