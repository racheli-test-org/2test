package com.hillel.items_exchange.controller;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.*;
import com.hillel.items_exchange.dao.TypeWithValidateFields;
import com.hillel.items_exchange.dao.TypeWithValidatedGetter;
import com.hillel.items_exchange.dao.TypeWithOutValidation;
import com.hillel.items_exchange.dao.RoleRepository;
import com.hillel.items_exchange.dto.UserRegistrationDto;
import com.hillel.items_exchange.exception.ResourceNotFoundException;
import com.hillel.items_exchange.exception.RoleNotFoundException;
import com.hillel.items_exchange.model.Role;
import com.hillel.items_exchange.security.jwt.JwtTokenProvider;
import com.hillel.items_exchange.service.UserService;
import com.hillel.items_exchange.validator.UserLoginDtoValidator;
import com.hillel.items_exchange.validator.UserRegistrationDtoValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class TypicalClass {

    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRegistrationDtoValidator userRegistrationDtoValidator;
    private final UserLoginDtoValidator userLoginDtoValidator;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public TypicalClass(RoleRepository roleRepository, BCryptPasswordEncoder passwordEncoder, UserRegistrationDtoValidator userRegistrationDtoValidator, UserLoginDtoValidator userLoginDtoValidator, UserService userService, AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRegistrationDtoValidator = userRegistrationDtoValidator;
        this.userLoginDtoValidator = userLoginDtoValidator;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;

    }

    @PostMapping("/register")
    public ResponseEntity<?> FullyValidated(@NotNull UserRegistrationDto userRegistrationDto,
                                         @NotNull BindingResult bindingResult) {
        userRegistrationDtoValidator.validate(userRegistrationDto, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ResourceNotFoundException(bindingResult.toString());
        }

        Role role = roleRepository.findById(1L).orElseThrow(RoleNotFoundException::new);
        userService.registerNewUser(userRegistrationDto, passwordEncoder, role);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/users/{username}")
                .buildAndExpand(userRegistrationDto.getUsername()).toUri();

        return ResponseEntity.created(location).body("user registered");
    }

    @PostMapping("/register")
    public ResponseEntity<?> PartiallyValidated(@NotNull UserRegistrationDto userRegistrationDto,
                                          BindingResult bindingResult) {
        userRegistrationDtoValidator.validate(userRegistrationDto, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ResourceNotFoundException(bindingResult.toString());
        }

        Role role = roleRepository.findById(1L).orElseThrow(RoleNotFoundException::new);
        userService.registerNewUser(userRegistrationDto, passwordEncoder, role);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/users/{username}")
                .buildAndExpand(userRegistrationDto.getUsername()).toUri();

        return ResponseEntity.created(location).body("user registered");
    }

    @PostMapping("/register")
    public ResponseEntity<?> NotAtAllValidated(UserRegistrationDto userRegistrationDto,
                                                BindingResult bindingResult) {
        userRegistrationDtoValidator.validate(userRegistrationDto, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ResourceNotFoundException(bindingResult.toString());
        }

        Role role = roleRepository.findById(1L).orElseThrow(RoleNotFoundException::new);
        userService.registerNewUser(userRegistrationDto, passwordEncoder, role);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/users/{username}")
                .buildAndExpand(userRegistrationDto.getUsername()).toUri();

        return ResponseEntity.created(location).body("user registered");
    }

    @PostMapping("/register")
    public ResponseEntity<?> ValidatedWithRemoteClassValidatedField(@NotNull TypeWithValidateFields userRegistrationDto) {
        userRegistrationDtoValidator.validate(userRegistrationDto, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ResourceNotFoundException(bindingResult.toString());
        }

        Role role = roleRepository.findById(1L).orElseThrow(RoleNotFoundException::new);
        userService.registerNewUser(userRegistrationDto, passwordEncoder, role);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/users/{username}")
                .buildAndExpand(userRegistrationDto.getUsername()).toUri();

        return ResponseEntity.created(location).body("user registered");
    }

    @PostMapping("/register")
    public ResponseEntity<?> ValidatedWithRemoteClassValidatedSetters(@NotNull  TypeWithValidatedGetter userRegistrationDto) {
        userRegistrationDtoValidator.validate(userRegistrationDto, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ResourceNotFoundException(bindingResult.toString());
        }

        Role role = roleRepository.findById(1L).orElseThrow(RoleNotFoundException::new);
        userService.registerNewUser(userRegistrationDto, passwordEncoder, role);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/users/{username}")
                .buildAndExpand(userRegistrationDto.getUsername()).toUri();

        return ResponseEntity.created(location).body("user registered");
    }

    @PostMapping("/register")
    public ResponseEntity<?> NotValidatedUsingRemotetype(@NotNull  TypeWithOutValidation userRegistrationDto) {
        userRegistrationDtoValidator.validate(userRegistrationDto, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ResourceNotFoundException(bindingResult.toString());
        }

        Role role = roleRepository.findById(1L).orElseThrow(RoleNotFoundException::new);
        userService.registerNewUser(userRegistrationDto, passwordEncoder, role);

        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath().path("/users/{username}")
                .buildAndExpand(userRegistrationDto.getUsername()).toUri();

        return ResponseEntity.created(location).body("user registered");
    }
}