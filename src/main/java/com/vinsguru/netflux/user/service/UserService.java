package com.vinsguru.netflux.user.service;

import com.vinsguru.netflux.user.dto.LoginRequest;
import com.vinsguru.netflux.user.dto.LoginResponse;
import com.vinsguru.netflux.user.dto.RegisterRequest;
import com.vinsguru.netflux.user.exceptions.UsernameAlreadyExistsException;
import com.vinsguru.netflux.user.mapper.UserMapper;
import com.vinsguru.netflux.user.repository.UserRepository;
import com.vinsguru.netflux.user.security.UserPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }
        try {
            userRepository.saveAndFlush(UserMapper.toEntity(request, passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException ex) {
            // concurrent registration with the same username
            throw new UsernameAlreadyExistsException(request.username());
        }
    }

    public LoginResponse login(LoginRequest request) {
        // throws BadCredentialsException (an AuthenticationException) for an unknown user or a wrong password
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        var principal = (UserPrincipal) authentication.getPrincipal();
        return new LoginResponse(
                tokenService.generateToken(principal.id()),
                principal.name()
        );
    }
}
