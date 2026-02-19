package com.example.ultitoolsext.services;

import com.ultikits.ultitools.annotations.Service;

@Service
public class GreetService {

    public String greet(String playerName) {
        return "Hello " + playerName + "! This message is from an external plugin using UltiTools-API.";
    }

    public String info() {
        return "UltiTools External Example v1.0.0 - verifying @Service injection works!";
    }
}
