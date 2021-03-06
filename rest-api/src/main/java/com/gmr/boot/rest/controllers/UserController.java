package com.gmr.boot.rest.controllers;

import com.gmr.boot.rest.RestConstants;
import com.gmr.boot.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(value = RestConstants.API_PREFIX + "/user")
public class UserController {

    @Autowired
    @Qualifier("userService")
    private UserService userService;

}
