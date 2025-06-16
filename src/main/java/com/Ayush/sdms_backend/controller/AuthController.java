package com.Ayush.sdms_backend.controller;


import com.Ayush.sdms_backend.components.JwtUtil;
import com.Ayush.sdms_backend.dto.RegisterRequest;
import com.Ayush.sdms_backend.model.User;
import com.Ayush.sdms_backend.repository.UserRepository;
import com.Ayush.sdms_backend.service.CustomUserDetailsServices;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsServices customUserDetailsServices;
    private final JwtUtil jwtUtil;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;



    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String email,@RequestParam String password ){
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            UserDetails userDetails = customUserDetailsServices.loadUserByUsername(email);
            String jwt = jwtUtil.generateToken(userDetails);

            return ResponseEntity.ok("Successfully Logged In ✅ \n jwt = " + jwt);
        } catch (Exception e) {
            e.printStackTrace(); // log to terminal
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login failed ❌: " + e.getMessage());
        }
    }


}
