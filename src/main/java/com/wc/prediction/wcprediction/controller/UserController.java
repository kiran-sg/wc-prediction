package com.wc.prediction.wcprediction.controller;

import com.wc.prediction.wcprediction.dto.WcUserDto;
import com.wc.prediction.wcprediction.entity.WcUser;
import com.wc.prediction.wcprediction.response.UserResponse;
import com.wc.prediction.wcprediction.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public List<WcUser> getAllUsers() {
        return userService.getAllUsers();
    }

    @PostMapping
    public WcUser createUser(@RequestBody WcUser user) {
        return userService.saveUser(user);
    }

    @PostMapping(value = "/validate", consumes = "application/json")
    public ResponseEntity<UserResponse> validateUser(@RequestBody WcUserDto user, HttpSession session) {
        UserResponse response = new UserResponse();
        if (user.getUserId() == null) {
            response.setMessage("User id not provided");
            return ResponseEntity.badRequest().body(response);
        }
        response = userService.validateUser(user.getUserId());
        if (response.isValidUser()) {
            session.setAttribute("userId", response.getUser().getUserId());
        }
        return ResponseEntity.ok(response);
    }
}
