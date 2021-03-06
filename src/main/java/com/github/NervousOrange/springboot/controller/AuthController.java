package com.github.NervousOrange.springboot.controller;

import com.github.NervousOrange.springboot.entity.AuthResult;
import com.github.NervousOrange.springboot.entity.User;
import com.github.NervousOrange.springboot.service.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.Map;

@RestController
public class AuthController {
    private UserService userService;
    private AuthenticationManager authenticationManager;

    @Inject
    public AuthController(UserService userService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/auth")
    @ResponseBody
    public AuthResult auth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user =  userService.getUserByUsername(authentication == null ? null : authentication.getName());
        if (user == null) {
            return AuthResult.notLoggedAuthResult();
        } else {
            return AuthResult.loggedAuthResult(user);
        }
    }

    @PostMapping("/auth/login")
    @ResponseBody
    public Object login(@RequestBody Map<String, String> usernameAndPassword) {
        String username = usernameAndPassword.get("username");
        String password = usernameAndPassword.get("password");
        UserDetails userDetails;
        try {
            userDetails = this.userService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e){
            return AuthResult.failedResult("用户不存在");
        }
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userDetails, password, userDetails.getAuthorities());
        try {
            authenticationManager.authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(token);
            return AuthResult.successfulResult("登录成功", userService.getUserByUsername(username));
        } catch (BadCredentialsException e) {
            return AuthResult.failedResult("密码不正确");
        }
    }

    @PostMapping("/auth/register")
    @ResponseBody
    public Object register(@RequestBody Map<String, String> usernameAndPassword) {
        String username = usernameAndPassword.get("username");
        String password = usernameAndPassword.get("password");
        if (username.length() < 1 || username.length() > 15) {
            return AuthResult.failedResult("invalid username");
        }
        if (password.length() < 6 || password.length() > 16) {
            return AuthResult.failedResult("invalid password");
        }
        try {
            userService.insertNewUser(username, password);
        } catch (DuplicateKeyException e) {
            return AuthResult.failedResult("username already exist");
        }
        login(usernameAndPassword);
        return AuthResult.successfulResult("注册成功", userService.getUserByUsername(username));
    }

    @GetMapping("/auth/logout")
    @ResponseBody
    public Object logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = this.userService.getUserByUsername(authentication == null ? null : authentication.getName());
        if (user == null) {
            return AuthResult.failedResult("用户尚未登录");
        } else {
            SecurityContextHolder.clearContext();
            return AuthResult.logoutResult("注销成功");
        }
    }
}
