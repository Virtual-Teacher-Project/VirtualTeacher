package com.alpha53.virtualteacher.utilities.helpers;

import com.alpha53.virtualteacher.exceptions.AuthorizationException;
import com.alpha53.virtualteacher.exceptions.EntityNotFoundException;
import com.alpha53.virtualteacher.models.User;
import com.alpha53.virtualteacher.services.contracts.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationHelper {

    public static final String PROFILE_CONFIRMATION_EXCEPTION = "Your profile has not been confirmed. Please follow the link sent to your email.";
    private static final String AUTHORIZATION_HEADER_NAME = "Authorization";
    private static final String INVALID_AUTHENTICATION_ERROR = "Invalid authentication.";
    public final UserService userService;

    @Autowired
    public AuthenticationHelper(UserService userService) {
        this.userService = userService;
    }

    public User tryGetUser(HttpHeaders headers) {
        String userInfo = headers.getFirst(AUTHORIZATION_HEADER_NAME);
        if (userInfo == null || userInfo.isBlank()) {
            throw new AuthorizationException(INVALID_AUTHENTICATION_ERROR);
        }
        String email = getEmail(userInfo);
        String password = getPassword(userInfo);
        return verifyAuthentication(email, password);
    }


    public User tryGetCurrentUser(HttpSession session) {
        String currentEmail = (String) session.getAttribute("currentUserEmail");
        try {
            User user = userService.get(currentEmail);
            throwIfNotVerified(user);
            return user;
        } catch (EntityNotFoundException e) {
            throw new AuthorizationException(INVALID_AUTHENTICATION_ERROR);
        }
    }

    public User verifyAuthentication(String email, String password) {
        try {
            User user = userService.get(email);
            if (!user.getPassword().equals(password)) {
                throw new AuthorizationException(INVALID_AUTHENTICATION_ERROR);
            }
            throwIfNotVerified(user);
            return user;
        } catch (EntityNotFoundException e) {
            throw new AuthorizationException(INVALID_AUTHENTICATION_ERROR);
        }
    }

    private static void throwIfNotVerified(User user) {
        if (!user.isVerified()) {
            throw new AuthorizationException(PROFILE_CONFIRMATION_EXCEPTION);
        }
    }

    private String getEmail(String userInfo) {
        int firstSpace = userInfo.indexOf(" ");
        if (firstSpace == -1) {
            throw new AuthorizationException(INVALID_AUTHENTICATION_ERROR);
        }
        return userInfo.substring(0, firstSpace);
    }

    private String getPassword(String userInfo) {
        int firstSpace = userInfo.indexOf(" ");
        if (firstSpace == -1) {
            throw new AuthorizationException(INVALID_AUTHENTICATION_ERROR);
        }
        return userInfo.substring(firstSpace + 1);
    }

}
