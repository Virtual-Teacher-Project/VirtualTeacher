package com.alpha53.virtualteacher.controllers.mvc;


import com.alpha53.virtualteacher.exceptions.*;
import com.alpha53.virtualteacher.models.Course;
import com.alpha53.virtualteacher.models.Lecture;
import com.alpha53.virtualteacher.models.RatingDto;
import com.alpha53.virtualteacher.models.User;
import com.alpha53.virtualteacher.models.dtos.CourseDto;
import com.alpha53.virtualteacher.models.dtos.LectureDto;
import com.alpha53.virtualteacher.services.TopicServiceImpl;
import com.alpha53.virtualteacher.services.contracts.CourseService;
import com.alpha53.virtualteacher.services.contracts.LectureService;
import com.alpha53.virtualteacher.utilities.helpers.AuthenticationHelper;
import com.alpha53.virtualteacher.utilities.mappers.dtoMappers.CourseDtoMapper;
import com.alpha53.virtualteacher.utilities.mappers.dtoMappers.LectureDtoMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@Controller
@RequestMapping("/courses")
public class CourseMvcController {
    private final CourseService courseService;
    private final AuthenticationHelper authenticationHelper;
    private final TopicServiceImpl topicService;
    private final CourseDtoMapper courseDtoMapper;
    private final LectureDtoMapper lectureDtoMapper;
    private final LectureService lectureService;

    public CourseMvcController(CourseService courseService, AuthenticationHelper authenticationHelper, TopicServiceImpl topicService, CourseDtoMapper courseDtoMapper, LectureDtoMapper lectureDtoMapper, LectureService lectureService) {
        this.courseService = courseService;
        this.authenticationHelper = authenticationHelper;
        this.topicService = topicService;
        this.courseDtoMapper = courseDtoMapper;
        this.lectureDtoMapper = lectureDtoMapper;
        this.lectureService = lectureService;
    }

    @ModelAttribute("isAuthenticated")
    private boolean populateIsAuthenticated(HttpSession session) {
        return session.getAttribute("currentUser") != null;
    }

    @GetMapping("/{id}")
    public String get(@PathVariable(name = "id") int id, Model model, HttpSession session) {
        Course course;
        try {

            User user = authenticationHelper.tryGetCurrentUser(session);
            course = courseService.getCourseByIdAuth(id, user);

            if (courseService.isUserEnrolled(user.getUserId(), course.getCourseId())) {
                model.addAttribute("isEnrolled", true);
                model.addAttribute("hasPassed", false);
            } else if (courseService.hasUserPassedCourse(user.getUserId(), course.getCourseId())) {
                model.addAttribute("hasPassed", true);
                model.addAttribute("isEnrolled", false);
            } else {
                model.addAttribute("hasPassed", false);
                model.addAttribute("isEnrolled", false);
            }
            if (user.getRole().getRoleType().equalsIgnoreCase("admin") || (user.getRole().getRoleType().equalsIgnoreCase("teacher") && course.getCreator().getUserId() == user.getUserId())) {
                model.addAttribute("hasModifyPermissions", true);
            } else {
                model.addAttribute("hasModifyPermissions", false);
            }

            model.addAttribute("course", course);
            model.addAttribute("currentUserRole", session.getAttribute("currentUserRole"));
            model.addAttribute("isStudent", session.getAttribute("isStudent"));
            model.addAttribute("ratings", courseService.getRatingsByCourseId(id));
            model.addAttribute("hasStarted", LocalDate.now().isAfter(course.getStartingDate()));
            model.addAttribute("isCreator", course.getCreator().getUserId()==user.getUserId());
            model.addAttribute("rating", new RatingDto());
            model.addAttribute("hasStudents", !courseService.getStudentsWhichAreEnrolledForCourse(course.getCourseId()).isEmpty());

            return "single-course";
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (EntityDuplicateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (AuthorizationException e) {
          //  course = courseService.getCourseById(id);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("statusCode", 401);
            return "4xx";
        }
    }

    @GetMapping("/{id}/enroll")
    public String enrollForCourse(@PathVariable(name = "id") int id, HttpSession session) {
        try {
            User user = authenticationHelper.tryGetCurrentUser(session);

            courseService.enrollUserForCourse(user, id);
            return "redirect:/courses/{id}";

        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (EntityDuplicateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        } catch (AuthorizationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @GetMapping("/new")
    public String createCourse(Model model) {
        model.addAttribute("course", new CourseDto());
        model.addAttribute("topics", topicService.getAll());
        return "new-course";
    }

    @PostMapping("/new")
    public String handleNewCourse( Model model, @Valid @ModelAttribute("course") CourseDto course,
                                BindingResult bindingResult,
                                HttpSession session) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("topics", topicService.getAll());
            return "new-course";
        }
        try {
            if (course.getDescription().getDescription().isEmpty()) {
                course.setDescription(null);
            }
            Course c = courseDtoMapper.fromDto(course);
            User user = authenticationHelper.tryGetCurrentUser(session);
            courseService.create(c, user);
            return "redirect:/";
        } catch (EntityDuplicateException e) {
            bindingResult.rejectValue("title", "title_error", e.getMessage());
            return "new-course";
        }
    }

    @GetMapping("/{id}/delete")
    public String deleteCourse(@PathVariable int id,HttpSession session) {
        User user;
        try {
            user = authenticationHelper.tryGetCurrentUser(session);
        } catch (AuthorizationException e) {
            return "redirect:/auth/login";
        }
            courseService.delete(id, user);
            return "redirect:/";
    }

    //TODO EntityNotFoundException must be catch we have error pages 4xx and 5xx we can use them

    @GetMapping("/{id}/update")
    public String showEditCoursePage(@PathVariable int id, Model model, HttpSession session) {
        try {
            User user = authenticationHelper.tryGetCurrentUser(session);
            Course course = courseService.getCourseByIdAuth(id, user);
            CourseDto courseDto = courseDtoMapper.toDto(course);
            model.addAttribute("topics", topicService.getAll());
            model.addAttribute("courseId", id);
            model.addAttribute("course", courseDto);
            return "edit-course";
        } catch (AuthorizationException e) {
            return "redirect:/auth/login";
        }
    }

    @PostMapping("/{id}/update")
    public String handleEditCourse(Model model, @PathVariable int id, @Valid @ModelAttribute("course") CourseDto course,
                                  BindingResult bindingResult,
                                  HttpSession session) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("topics", topicService.getAll());
            model.addAttribute("courseId", id);
            model.addAttribute("course", course);
            return "edit-course";
        }
        try {
            if (course.getDescription().getDescription().isEmpty()) {
                course.setDescription(null);
            }
            Course c = courseDtoMapper.fromDto(course);
            c.setCourseId(id);
            User user = authenticationHelper.tryGetCurrentUser(session);
            courseService.update(c, user);
            return "redirect:/";
        } catch (EntityDuplicateException e) {
            bindingResult.rejectValue("title", "title_error", e.getMessage());
            return "edit-course";
        }
    }

    @PostMapping("/{id}/rate")
    public String handleRateCourse(@PathVariable int id, @Valid @ModelAttribute("rating") RatingDto rating,
                                   BindingResult bindingResult,
                                   HttpSession session) {
        if (bindingResult.hasErrors()) {
            return "single-course";
        }
        try {
            User user = authenticationHelper.tryGetCurrentUser(session);
            rating.setCourseId(id);
            courseService.rateCourse(rating, id, user.getUserId());
            return "redirect:/";
        } catch (EntityDuplicateException e) {
            bindingResult.rejectValue("title", "title_error", e.getMessage());
            return "single-course";
        }
    }


    //TODO maybe move to LectureMvcController
    //handle file upload
    @GetMapping("/{courseId}/lecture/new")
    public String showNewLecturePage(@PathVariable int courseId, Model model, HttpSession session) {
        try {
            User user =  authenticationHelper.tryGetCurrentUser(session);
            courseService.getCourseByIdAuth(courseId, user);
            model.addAttribute("lecture", new LectureDto());
            return "new-lecture";
        } catch (AuthorizationException e) {
            return "redirect:/auth/login";
        }
    }
    @PostMapping("/{courseId}/lecture/new")
    public String handleNewLecture(@RequestParam("file") MultipartFile file,
                                   @PathVariable int courseId,
                                   @Valid @ModelAttribute("lecture") LectureDto lecture,
                                   Model model,
                                   BindingResult bindingResult,
                                   HttpSession session) {
        model.addAttribute("file", file);
        if (bindingResult.hasErrors()) {

            return "new-lecture";
        }
        try {
            if (lecture.getDescription().getDescription().isEmpty()){
                lecture.setDescription(null);
            }
            Lecture l = lectureDtoMapper.dtoToObject(lecture);
            User user = authenticationHelper.tryGetCurrentUser(session);
            l.setCourseId(courseId);
            lectureService.create(l, user, file);
            return "redirect:/";
        } catch (EntityDuplicateException e) {
            bindingResult.rejectValue("title", "title_error", e.getMessage());
            return "new-lecture";
        }
        catch (EntityNotFoundException | UnsupportedFileTypeException | StorageException e) {
            model.addAttribute("errorMessage",e.getMessage());
            model.addAttribute("statusCode",400);
            return "new-lecture";
        }
    }
    @ModelAttribute("requestURI")
    public String requestURI(final HttpServletRequest request) {
        return request.getRequestURI();
    }
}
