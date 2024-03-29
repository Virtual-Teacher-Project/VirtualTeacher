package com.alpha53.virtualteacher.services;

import com.alpha53.virtualteacher.exceptions.AuthorizationException;
import com.alpha53.virtualteacher.exceptions.EntityDuplicateException;
import com.alpha53.virtualteacher.exceptions.EntityNotFoundException;
import com.alpha53.virtualteacher.models.*;
import com.alpha53.virtualteacher.repositories.contracts.CourseDao;
import com.alpha53.virtualteacher.repositories.contracts.LectureDao;
import com.alpha53.virtualteacher.services.contracts.CourseService;
import com.alpha53.virtualteacher.services.contracts.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class CourseServiceImpl implements CourseService {
    private static final String ONLY_CREATOR_CAN_MODIFY_A_COURSE = "Only  creator or admin can modify a course.";
    private static final String ONLY_TEACHER_OR_ADMIN_CAN_CREATE_COURSE = "Only teacher or admin can create course.";
    public static final String COURSE_TRANSFER_EXCEPTION = "Courses can only be transferred by an Admin.";
    public static final String ASSIGN_COURSE_TO_USER_EXCEPTION = "Courses can only be assigned to teachers.";
    public static final String INVALID_CURRENT_TEACHER_EXCEPTION = "User with ID %d is not a Teacher.";
    private final CourseDao courseRepository;
    private final UserService userService;
    private final LectureDao lectureDao;

    @Autowired
    public CourseServiceImpl(CourseDao courseRepository, UserService userService, LectureDao lectureDao) {
        this.courseRepository = courseRepository;
        this.userService = userService;
        this.lectureDao = lectureDao;
    }

    public void create(Course course, User user) {
        //TODO fix description dto

        boolean duplicateExists = true;
        try {
            courseRepository.getByTitle(course.getTitle());

        } catch (EntityNotFoundException e) {
            duplicateExists = false;
        }

        if (duplicateExists) {
            throw new EntityDuplicateException("Course", "title", course.getTitle());
        }

        if (!user.getRole().getRoleType().equalsIgnoreCase("teacher") && !user.getRole().getRoleType().equalsIgnoreCase("admin")) {
            throw new AuthorizationException(ONLY_TEACHER_OR_ADMIN_CAN_CREATE_COURSE);
        }
        course.setCreator(user);
        courseRepository.create(course);
    }

    public void update(Course course, User user) {
        //TODO fix description dto
        boolean duplicateExists = true;
        try {
            Course existingCourse = courseRepository.getByTitle(course.getTitle());
            if (Objects.equals(existingCourse.getCourseId(), course.getCourseId())) {
                duplicateExists = false;
            }
        } catch (EntityNotFoundException e) {
            duplicateExists = false;
        }

        if (duplicateExists) {
            throw new EntityDuplicateException("Course", "title", course.getTitle());
        }
        checkModifyPermissions(course.getCourseId(), user);
        course.setCreator(user);
        courseRepository.update(course);
    }

    public void delete(int id, User user) {
        Course course = courseRepository.get(id);
        checkModifyPermissions(id, user);
        if (!course.isPublished() || courseRepository.getStudentsWhichAreEnrolledForCourse(id).isEmpty()) {

            courseRepository.delete(id);
        } else {
            throw new AuthorizationException("You can delete course only if there are no enrolled students or the course is not public");
        }

    }

    public Course getCourseById(int id) {
        Course course = courseRepository.get(id);
        if (!course.isPublished()) {
            throw new AuthorizationException("Only teacher or admin can access a course which is not public");
        } else {
            Set<Lecture> lectures = new HashSet<>(lectureDao.getAllByCourseId(id));
            course.setLectures(lectures);
            return course;
        }


    }

    @Override
    public Course getCourseByIdAuth(int id, User user) {
        Course course = courseRepository.get(id);
        if (!course.isPublished() && (user.getRole().getRoleType().equalsIgnoreCase("student") || user.getRole().getRoleType().equalsIgnoreCase("PendingTeacher"))) {
            throw new AuthorizationException("Only teacher or admin can access a course which is not public");
        } else {
            Set<Lecture> lectures = new HashSet<>(lectureDao.getAllByCourseId(id));
            course.setLectures(lectures);
            return course;
        }
    }

    public List<Course> getPublic(FilterOptions filterOptions) {
        filterOptions.setIsPublic(Optional.of(Boolean.TRUE));
        return courseRepository.get(filterOptions);
    }

    public List<Course> get(FilterOptions filterOptions, Optional<User> optionalUser) {
        if (optionalUser.isEmpty() || optionalUser.get().getRole().getRoleType().equalsIgnoreCase("student") || optionalUser.get().getRole().getRoleType().equalsIgnoreCase("PendingTeacher")){
            filterOptions.setIsPublic(Optional.of(Boolean.TRUE));
        }
        return courseRepository.get(filterOptions);
    }

    @Override
    public List<Course> getUsersEnrolledCourses(int userId) {
        return courseRepository.getOngoingCoursesByUser(userId);
    }

    @Override
    public List<Course> getUsersCompletedCourses(int userId) {
        return courseRepository.getCompletedCoursesByUser(userId);
    }

    public void transferTeacherCourses(int teacherToTransferFromId, int teacherToTransferToId, User loggedUser) {
        if (!loggedUser.getRole().getRoleType().equalsIgnoreCase("Admin")) {
            throw new AuthorizationException(COURSE_TRANSFER_EXCEPTION);
        }
        User previousTeacher = userService.get(teacherToTransferFromId);
        if (!previousTeacher.getRole().getRoleType().equalsIgnoreCase("Teacher") && !previousTeacher.getRole().getRoleType().equalsIgnoreCase("Admin")) {
            throw new UnsupportedOperationException(String.format(INVALID_CURRENT_TEACHER_EXCEPTION, previousTeacher.getUserId()));
        }
        User newTeacher = userService.get(teacherToTransferToId);
        if (!newTeacher.getRole().getRoleType().equalsIgnoreCase("Teacher") && !newTeacher.getRole().getRoleType().equalsIgnoreCase("Admin")) {
            throw new UnsupportedOperationException(ASSIGN_COURSE_TO_USER_EXCEPTION);
        }
        if (teacherToTransferToId == teacherToTransferFromId) {
            return;
        }
        courseRepository.transferTeacherCourses(teacherToTransferFromId, teacherToTransferToId);
    }

    @Override
    public void enrollUserForCourse(User user, int courseId) {
        List<Course> enrolledCourses = courseRepository.getOngoingCoursesByUser(user.getUserId());
        Course course = courseRepository.get(courseId);

        if (containsId(enrolledCourses, courseId)) {
            throw new EntityDuplicateException("Record", "id", Integer.toString(courseId));

        } else if (!user.getRole().getRoleType().equalsIgnoreCase("student")) {
            throw new AuthorizationException("Only student can enroll for course");
        } else if (course.getStartingDate().isAfter(LocalDate.now())) {
            throw new AuthorizationException("You cannot enroll before the starting date");
        } else if (!course.isPublished()) {
            throw new AuthorizationException("You cannot enroll for course which is not public");
        } else {
            courseRepository.enrollUserForCourse(user.getUserId(), courseId);
        }

    }

    @Override
    public void rateCourse(RatingDto rating, int courseId, int raterId) {
        if (courseRepository.hasUserPassedCourse(raterId, courseId)) {
            courseRepository.rateCourse(rating, courseId, raterId);
        } else {
            throw new AuthorizationException("You must pass course to leave rating");
        }

    }

    @Override
    public boolean isUserEnrolled(int userId, int courseId) {
        return courseRepository.isUserEnrolled(userId,courseId);
    }

    @Override
    public boolean hasUserPassedCourse(int userId, int courseId) {
        return courseRepository.hasUserPassedCourse(userId, courseId);
    }

    private boolean containsId(final List<Course> list, final int id) {
        return list.stream().anyMatch(o -> o.getCourseId() == id);
    }

    private void checkModifyPermissions(int courseId, User user) {
        Course course = courseRepository.get(courseId);
        if (course.getCreator().getUserId() != user.getUserId() && !user.getRole().getRoleType().equalsIgnoreCase("admin")) {
            throw new AuthorizationException(ONLY_CREATOR_CAN_MODIFY_A_COURSE);
        }
    }

    public Integer getCoursesCount(){
     return courseRepository.getPublishedCoursesCount();
    }

    @Override
    public List<Rating> getRatingsByCourseId(int courseId) {
        return courseRepository.getRatingsByCourseId(courseId);
    }

    @Override
    public List<User> getStudentsWhichAreEnrolledForCourse(int courseId) {
        return courseRepository.getStudentsWhichAreEnrolledForCourse(courseId);
    }


}
