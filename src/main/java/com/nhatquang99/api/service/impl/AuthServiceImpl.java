package com.nhatquang99.api.service.impl;

import com.nhatquang99.api.model.Role;
import com.nhatquang99.api.model.User;
import com.nhatquang99.api.model.enums.ERole;
import com.nhatquang99.api.payload.request.AuthRequest;
import com.nhatquang99.api.payload.response.GenericResponse;
import com.nhatquang99.api.payload.response.LoginResponse;
import com.nhatquang99.api.repository.RoleRepository;
import com.nhatquang99.api.repository.UserRepository;
import com.nhatquang99.api.security.JwtTokenProvider;
import com.nhatquang99.api.security.MyUserDetails;
import com.nhatquang99.api.service.IAuthService;
import com.nhatquang99.api.service.IMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.transaction.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements IAuthService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public Object login(AuthRequest user) {
        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
            if (!userRepository.findByUsername(user.getUsername()).orElse(null).isVerifyMail())
                return new GenericResponse(HttpStatus.BAD_REQUEST,user.getUsername() + " ch??a x??c th???c!", null);
            // Set th??ng tin authentication v??o Security Context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // Tr??? v??? jwt cho ng?????i d??ng.
            MyUserDetails userDetails = (MyUserDetails) authentication.getPrincipal();
            String jwt = jwtTokenProvider.generateToken(userDetails);

            LoginResponse data = new LoginResponse(userDetails.getUsername(), jwt, userDetails.getAuthorities().stream().findFirst().get().toString());

            return new GenericResponse(HttpStatus.OK, "????ng nh???p th??nh c??ng!", data);
        } catch (RuntimeException ex) {
            return new GenericResponse(HttpStatus.FORBIDDEN, "????ng nh???p th???t b???i. T??n ????ng nh???p ho???c m???t kh???u kh??ng ch??nh x??c!", null);
        }
    }

    @Override
    public Object signup(AuthRequest userRegister) {
        if (userRepository.existsByUsername(userRegister.getUsername())) {
            return new GenericResponse(HttpStatus.BAD_REQUEST, "????ng k?? th???t b???i. " + userRegister.getUsername() + " ???? t???n t???i", null);
        }

        User user = new User();
        user.setPassword(passwordEncoder.encode(userRegister.getPassword()));
        user.setUsername(userRegister.getUsername());

        // Set role member cho user;
        Role role = roleRepository.findByName(ERole.ROLE_MEMBER).orElse(null);
        if (role == null) {
            return new GenericResponse(HttpStatus.BAD_REQUEST, ERole.ROLE_MEMBER + " kh??ng t???n t???i.", null);
        }
        user.setRole(role);
        user.setVerifyMail(true);
        userRepository.save(user);

        return new GenericResponse(HttpStatus.OK, "????ng k?? th??nh c??ng!", null);
    }
}
