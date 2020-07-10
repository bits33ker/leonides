package com.herod.leonides.controller;

import com.herod.leonides.call.RouteCall;
import com.herod.leonides.service.LeonidesRestService;
import com.herod.rtp.RtpCodecException;
import com.herod.rtp.RtpCodec;
import com.herod.sip.SipInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")public class LeonidesRestController {
    private final static Logger logger = Logger.getLogger(LeonidesRestController.class.getName());

    @Autowired
    LeonidesRestService leonidesRestService;

    @GetMapping("/sipinterfaces")
    @ResponseStatus(HttpStatus.OK)
    public List<SipInterface> getSipInterfaces()
    {
        return leonidesRestService.getSipInterfaces();
    }
    @GetMapping("/codecs")
    @ResponseStatus(HttpStatus.OK)
    public List<RtpCodec> getCodecs()
    {
        return leonidesRestService.getCodecs();
    }
    @PostMapping("/codecs/add")
    public ResponseEntity<Void>addCodec(@RequestBody RtpCodec codec)
    {
        try {
            leonidesRestService.addCodec(codec);
        } catch (RtpCodecException e) {
            return new ResponseEntity<Void>(HttpStatus.CONFLICT);
        }
        return new ResponseEntity<Void>(HttpStatus.CREATED);
    }
    @PostMapping("/codecs/delete")
    public ResponseEntity<Void> deleteCodec(@RequestBody RtpCodec dcodec)
    {
        if(leonidesRestService.findCodec(dcodec)) {
            try {
                leonidesRestService.deleteCodec(dcodec);
            } catch (RtpCodecException e) {
                return new ResponseEntity<Void>(HttpStatus.CONFLICT);
            }
            return new ResponseEntity<Void>(HttpStatus.OK);
        }
        return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
    }
    @DeleteMapping("/codecs/{codec}/delete/")
    public ResponseEntity<Void> deleteCodec(@PathVariable(value="codec")Long media)
    {
        Optional<RtpCodec> codec;
        codec = leonidesRestService.findCodecByMedia(media);
        if(codec.isPresent()) {
            try {
                leonidesRestService.deleteCodec(codec.get());
            } catch (RtpCodecException e) {
                return new ResponseEntity<Void>(HttpStatus.CONFLICT);
            }
            return new ResponseEntity<Void>(HttpStatus.OK);
        }
        return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
    }
    @GetMapping("/routes")
    @ResponseStatus(HttpStatus.OK)
    public List<RouteCall> getRoutes()
    {
        return leonidesRestService.getRoutes();
    }
    @PostMapping("/routes/add")
    public ResponseEntity<Void>addRoute(@RequestBody RouteCall route)
    {
        try {
            leonidesRestService.addRoute(route.toString());
        } catch (RtpCodecException e) {
            return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
        } catch (ParseException e) {
            return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<Void>(HttpStatus.CREATED);
    }
    @PutMapping("routes/{id}/update/")
    public ResponseEntity<?>updateRoute(@PathVariable(value="id") Long id, @RequestBody RouteCall newRoute)
    {
        Optional<RouteCall>routeCall = leonidesRestService.findById(id);
        if(routeCall.isPresent()) {
            leonidesRestService.updateRoute(id, newRoute);
            return new ResponseEntity<>(routeCall.get(), HttpStatus.CREATED);
        }
        return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
    }
    @GetMapping("/whitelist")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getWhiteList()
    {
        return leonidesRestService.getUsers();
    }
    @GetMapping("/blacklist")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getBlackList()
    {
        return leonidesRestService.getUsers();
    }
    @GetMapping("/users")
    @ResponseStatus(HttpStatus.OK)
    public List<String> getUsers()
    {
        return leonidesRestService.getUsers();
    }
}
