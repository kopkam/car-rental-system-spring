package com.transport.controller;

import com.transport.dto.UserRegistrationDto;
import com.transport.entity.User;
import com.transport.service.UserService;
import com.transport.service.RecaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private RecaptchaService recaptchaService;

    @Value("${google.recaptcha.site-key}")
    private String recaptchaSiteKey;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegistrationDto());
        model.addAttribute("recaptchaSiteKey", recaptchaSiteKey);
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("user") UserRegistrationDto userDto,
                               BindingResult result,
                               HttpServletRequest request,
                               Model model) {

        String recaptchaResponse = request.getParameter("g-recaptcha-response");
        String clientIp = request.getRemoteAddr();

        // Weryfikacja reCAPTCHA
        boolean isCaptchaValid = recaptchaService.verifyRecaptcha(recaptchaResponse, clientIp);
        if (!isCaptchaValid) {
            result.rejectValue("captcha", "error.captcha", "Captcha verification failed");
        }

        if (result.hasErrors()) {
            model.addAttribute("recaptchaSiteKey", recaptchaSiteKey);
            return "register";
        }

        // Sprawdzenie, czy użytkownik istnieje
        if (userService.findByUsername(userDto.getUsername()) != null) {
            result.rejectValue("username", "error.username", "Username already exists");
            model.addAttribute("recaptchaSiteKey", recaptchaSiteKey);
            return "register";
        }

        if (userService.findByEmail(userDto.getEmail()) != null) {
            result.rejectValue("email", "error.email", "Email already exists");
            model.addAttribute("recaptchaSiteKey", recaptchaSiteKey);
            return "register";
        }

        // Rejestracja użytkownika
        User user = new User(
                userDto.getUsername(),
                userDto.getEmail(),
                userDto.getPassword(),
                userDto.getFirstName(),
                userDto.getLastName()
        );

        userService.registerUser(user);

        model.addAttribute("message", "Registration successful! Please check your email to verify your account.");
        return "login";
    }

    @GetMapping("/verify")
    public String verifyUser(@RequestParam("token") String token, Model model) {
        boolean isVerified = userService.verifyUser(token);

        if (isVerified) {
            model.addAttribute("message", "Account verified successfully! You can now login.");
            model.addAttribute("messageType", "success");
        } else {
            model.addAttribute("message", "Invalid or expired verification token. Please try registering again.");
            model.addAttribute("messageType", "error");
        }

        return "login";
    }
}