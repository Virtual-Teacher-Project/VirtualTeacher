package com.alpha53.virtualteacher.controllers.mvc;

import com.alpha53.virtualteacher.exceptions.AuthorizationException;
import com.alpha53.virtualteacher.exceptions.EntityDuplicateException;
import com.alpha53.virtualteacher.exceptions.EntityNotFoundException;
import com.alpha53.virtualteacher.exceptions.StorageException;
import com.alpha53.virtualteacher.models.FilterOptionsUsers;
import com.alpha53.virtualteacher.models.User;
import com.alpha53.virtualteacher.models.dtos.EmailForm;
import com.alpha53.virtualteacher.models.dtos.FilterUserDto;
import com.alpha53.virtualteacher.models.dtos.UserDto;
import com.alpha53.virtualteacher.services.contracts.CourseService;
import com.alpha53.virtualteacher.services.contracts.UserService;
import com.alpha53.virtualteacher.utilities.helpers.AuthenticationHelper;
import com.alpha53.virtualteacher.utilities.mappers.dtoMappers.UserMapperHelper;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/users")
public class UserMvcController {

    private final UserService userService;
    private final AuthenticationHelper authenticationHelper;
    private final CourseService courseService;

    public UserMvcController(UserService userService, AuthenticationHelper authenticationHelper, CourseService courseService, UserMapperHelper userMapperHelper) {
        this.userService = userService;
        this.authenticationHelper = authenticationHelper;
        this.courseService = courseService;
    }

    @GetMapping("/{id}/profile")
    public String showUserPage(HttpSession session, Model model, @PathVariable int id) {
        // TODO: 11.12.23 consider rewriting this later on.
        User loggedUser;
        User userToGet;
        try {
            loggedUser = authenticationHelper.tryGetCurrentUser(session);
            userToGet = userService.get(id);
            if (loggedUser.getUserId() != userToGet.getUserId() &&
                    !(loggedUser.getRole().getRoleType().equalsIgnoreCase("Admin") ||
                            loggedUser.getRole().getRoleType().equalsIgnoreCase("Teacher"))) {
                model.addAttribute("errorMessage", "Unauthorized access.");
                model.addAttribute("statusCode", 404);
                return "4xx";
            }
            model.addAttribute("userProfile", userToGet);
        } catch (AuthorizationException e) {
            return "redirect:/auth/login";
        } catch (EntityNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 404);
            return "4xx";
        }
        return "user";
    }

    @GetMapping("/{id}/courses/enrolled")
    public String showUserEnrolledCourses(HttpSession session, Model model, @PathVariable int id) {
        // TODO: 11.12.23 consider rewriting this later on.
        User loggedUser;
        User userToGet;
        try {
            loggedUser = authenticationHelper.tryGetCurrentUser(session);
            userToGet = userService.get(id);
            if (loggedUser.getUserId() != userToGet.getUserId() &&
                    !(loggedUser.getRole().getRoleType().equalsIgnoreCase("Admin") ||
                            loggedUser.getRole().getRoleType().equalsIgnoreCase("Teacher"))) {
                model.addAttribute("errorMessage", "Unauthorized access.");
                model.addAttribute("statusCode", 404);
                return "4xx";
            }
            model.addAttribute("userProfile", userToGet);
        } catch (AuthorizationException e) {
            return "redirect:/auth/login";
        } catch (EntityNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 404);
            return "4xx";
        }
        return "user-enrolled-courses";
    }

    @GetMapping("/{id}/courses/completed")
    public String showUserCompletedCourses(HttpSession session, Model model, @PathVariable int id) {
        // TODO: 11.12.23 consider rewriting this later on.
        User loggedUser;
        User userToGet;
        try {
            loggedUser = authenticationHelper.tryGetCurrentUser(session);
            userToGet = userService.get(id);
            if (loggedUser.getUserId() != userToGet.getUserId() &&
                    !(loggedUser.getRole().getRoleType().equalsIgnoreCase("Admin") ||
                            loggedUser.getRole().getRoleType().equalsIgnoreCase("Teacher"))) {
                model.addAttribute("errorMessage", "Unauthorized access.");
                model.addAttribute("statusCode", 404);
                return "4xx";
            }
            model.addAttribute("userProfile", userToGet);
            model.addAttribute("completedCourses", courseService.getUsersCompletedCourses(id));
            return "user-completed-courses";
        } catch (AuthorizationException e) {
            return "redirect:/auth/login";
        } catch (EntityNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 404);
            return "4xx";
        }
    }

    @GetMapping("/{id}/settings")
    public String showUserUpdatePage(HttpSession session, Model model, @PathVariable int id) {
        User loggedUser;
        User userToGet;
        try {
            loggedUser = authenticationHelper.tryGetCurrentUser(session);
            userToGet = userService.get(id);
            if (loggedUser.getUserId() != userToGet.getUserId()) {
                model.addAttribute("errorMessage", "Unauthorized access.");
                model.addAttribute("statusCode", 404);
                return "4xx";
            }
            model.addAttribute("userProfile", userToGet);
        } catch (AuthorizationException e) {
            return "redirect:/auth/login";
        } catch (EntityNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 404);
            return "4xx";
        }
        UserDto userDto = new UserDto();
        userDto.setFirstName(userToGet.getFirstName());
        userDto.setLastName(userToGet.getLastName());
        userDto.setEmail(userToGet.getEmail());
        userDto.setPassword(userToGet.getPassword());
        userDto.setRole(userToGet.getRole().getRoleType());
        model.addAttribute("userDto", userDto);
        return "user-settings";
    }

    @PostMapping("/{id}/settings")
    public String handleUpdateUser(@Valid @ModelAttribute("userDto") UserDto updateDto, BindingResult bindingResult, HttpSession session, Model model,
                                   @PathVariable int id) {
        try {
            User loggedInUser = authenticationHelper.tryGetCurrentUser(session);
            model.addAttribute("userProfile", userService.get(id));
            if (bindingResult.hasErrors()) {
                return "user-settings";
            }
            userService.update(updateDto, loggedInUser, id);
            return "redirect:/users/{id}/profile";
        } catch (AuthorizationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 401);
            return "4xx";
        } catch (EntityNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 404);
            return "4xx";
        }

    }

    @GetMapping("/{id}/delete")
    public String deleteUser(@PathVariable int id, Model model, HttpSession session) {
        User user = null;
        try {
            user = authenticationHelper.tryGetCurrentUser(session);
            userService.delete(id, user);

            if (user.getRole().getRoleType().equalsIgnoreCase("Admin") && user.getUserId() != id) {
                return "redirect:/users";
            }
            session.removeAttribute("currentUser");
            session.removeAttribute("currentUserEmail");
            return "redirect:/auth/login";
        } catch (AuthorizationException e) {
            // TODO: 13.12.23 we can just add a specific exception statement later on.
            if (e.getMessage().contains("transfer")) {
                assert user != null;
                if (user.getRole().getRoleType().equalsIgnoreCase("Admin")) {
                    model.addAttribute("errorMessage", e.getMessage());
                    return "transfer-course";
                }
            }
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 401);
            return "4xx";
        } catch (EntityNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 404);
            return "4xx";
        } catch (StorageException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 422);
            return "4xx";
        }
    }

    @PostMapping("/transfer")
    public String transferCourses(@RequestParam("userIdFrom") int userIdFrom, @RequestParam("userIdTo") int userIdTo, Model model, HttpSession session) {
        try {
            User user = authenticationHelper.tryGetCurrentUser(session);
            courseService.transferTeacherCourses(userIdFrom, userIdTo, user);
            if (user.getUserId() != userIdFrom) {
                return "redirect:/users";
            } else {
                return String.format("redirect:/users/%d/settings", user.getUserId());
            }
        } catch (AuthorizationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 401);
            return "4xx";
        } catch (EntityNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 404);
            return "4xx";
        } catch (UnsupportedOperationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 403);
            return "4xx";
        }
    }


    @PostMapping("{id}/settings/picture")
    public String uploadProfilePicture(HttpSession session, @PathVariable int id, Model model, @RequestParam("file") MultipartFile file) {
        try {
            User loggedInUser = authenticationHelper.tryGetCurrentUser(session);
            userService.uploadProfilePicture(file, loggedInUser, id);
            model.addAttribute("userProfile", userService.get(id));
            session.setAttribute("currentUser", loggedInUser);
            return "redirect:/users/{id}/profile";
        } catch (AuthorizationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 401);
            return "4xx";
        } catch (StorageException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 422);
            return "4xx";
        }
        // TODO: 12.12.23 catch entitynotfoundexception
    }

    @GetMapping("/referral")
    public String showReferralPage(Model model) {
        model.addAttribute("emailForm", new EmailForm());
        return "referral";
    }

    @PostMapping("/referral")
    public String handleReferral(@Valid @ModelAttribute("emailForm") EmailForm emailForm,
                                 BindingResult bindingResult, HttpSession session, Model model) {
        if (bindingResult.hasErrors()) {
            return "referral";
        }

        try {
            User loggedInUser = authenticationHelper.tryGetCurrentUser(session);
            userService.referFriend(loggedInUser, emailForm.getEmail());
            return "referral-confirmation";
        } catch (AuthorizationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 401);
            return "4xx";
        } catch (EntityDuplicateException e) {
            bindingResult.rejectValue("email", "email_error", e.getMessage());
            return "referral";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 422);
            return "4xx";
        }
    }

    @GetMapping
    public String showUsers(@ModelAttribute("filterOptionsUsers") FilterUserDto filterUserDto, HttpSession session, Model model) {
        try {
            User loggedUser = authenticationHelper.tryGetCurrentUser(session);
            if (!loggedUser.getRole().getRoleType().equalsIgnoreCase("Admin") && !loggedUser.getRole().getRoleType().equalsIgnoreCase("Teacher")) {
                model.addAttribute("errorMessage", "Invalid authentication.");
                model.addAttribute("statusCode", 401);
                return "4xx";
            }
            FilterOptionsUsers filterOptionsUsers = new FilterOptionsUsers(
                    filterUserDto.getEmail(),
                    filterUserDto.getFirstName(),
                    filterUserDto.getLastName(),
                    filterUserDto.getRole(),
                    filterUserDto.getSortBy(),
                    filterUserDto.getSortOrder());

            List<User> userList = userService.getAll(filterOptionsUsers);
            model.addAttribute("filterOptionsUsers", filterUserDto);
            model.addAttribute("users", userList);
            model.addAttribute("roles", userService.getRoles());
            return "users-view";

        } catch (AuthorizationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 401);
            return "4xx";
        }
    }

    @PostMapping("{id}/role/{newRole}")
    public String setUserRole(HttpSession session, Model model, @PathVariable int id, @PathVariable String newRole) {
        try {
            User loggedInUser = authenticationHelper.tryGetCurrentUser(session);
            User userToGetRole = userService.get(id);
            userService.setUserRole(loggedInUser, userToGetRole, newRole);
            return "redirect:/users";
        } catch (AuthorizationException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 401);
            return "4xx";
        } catch (EntityNotFoundException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 404);
            return "4xx";
        }
    }
}
