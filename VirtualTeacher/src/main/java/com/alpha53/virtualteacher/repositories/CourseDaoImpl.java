package com.alpha53.virtualteacher.repositories;

import com.alpha53.virtualteacher.exceptions.EntityNotFoundException;
import com.alpha53.virtualteacher.models.*;
import com.alpha53.virtualteacher.repositories.contracts.CourseDao;
import com.alpha53.virtualteacher.utilities.mappers.CourseMapper;
import com.alpha53.virtualteacher.utilities.mappers.RatingMapper;
import com.alpha53.virtualteacher.utilities.mappers.UserMapper;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import javax.sql.DataSource;
import java.util.*;

@Repository
public class CourseDaoImpl extends NamedParameterJdbcDaoSupport implements CourseDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final UserMapper USER_MAPPER = new UserMapper();
    private static final CourseMapper COURSE_MAPPER = new CourseMapper();
    private final RatingMapper RATING_MAPPER = new RatingMapper();

    public CourseDaoImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource dataSource) {
        this.setDataSource(dataSource);
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    }

    @Override
    public Course get(int id) {

        String sql = "SELECT description, courses.id,title,start_date,creator_id,email,first_name,last_name,picture_url, " +
                "is_verified,is_published,passing_grade,topic,topic_id, AVG(ratings.rating) AS avg_rating " +
                "FROM courses LEFT JOIN topics ON courses.topic_id = topics.id " +
                "LEFT JOIN users ON courses.creator_id = users.id " +
                "LEFT JOIN ratings ON courses.id = ratings.course_id " +
                "LEFT JOIN course_description on courses.id = course_description.course_id " +
                "WHERE courses.id=:id " +
                "GROUP BY courses.id";

        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("id", id);

        try {
            return namedParameterJdbcTemplate.queryForObject(sql, in, COURSE_MAPPER);
        } catch (IncorrectResultSizeDataAccessException e) {
            throw new EntityNotFoundException("Course", "id", String.valueOf(id));
        }

    }

    @Override
    public Course getByTitle(String title) {
        String sql = "SELECT description, courses.id,title,start_date,creator_id,email,first_name,last_name,picture_url, " +
                "is_verified,is_published,passing_grade,topic,topic_id, AVG(ratings.rating) AS avg_rating " +
                "FROM courses LEFT JOIN topics ON courses.topic_id = topics.id " +
                "LEFT JOIN users ON courses.creator_id = users.id " +
                "LEFT JOIN ratings ON courses.id = ratings.course_id " +
                "LEFT JOIN course_description on courses.id = course_description.course_id " +
                "WHERE courses.title=:title      " +
                "GROUP BY courses.id";


        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("title", title);

        try {
            return namedParameterJdbcTemplate.queryForObject(sql, in, COURSE_MAPPER);
        } catch (IncorrectResultSizeDataAccessException e) {
            throw new EntityNotFoundException();
        }
    }

    @Override
    public List<Course> get(FilterOptions filterOptions) {
        StringBuilder queryString = new StringBuilder("SELECT description, courses.id,title,start_date,creator_id,email, " +
                "first_name,last_name,picture_url, " +
                "is_verified,is_published,passing_grade,topic,topic_id, AVG(ratings.rating) AS avg_rating " +
                "FROM courses " +
                "LEFT JOIN topics ON courses.topic_id = topics.id " +
                "LEFT JOIN users ON courses.creator_id = users.id " +
                "LEFT JOIN ratings ON courses.id = ratings.course_id " +
                "LEFT JOIN course_description on courses.id = course_description.course_id ");


        List<String> filters = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (filterOptions.getTitle().isPresent() && !filterOptions.getTitle().get().isEmpty()) {
            filters.add("title like :title ");
            params.addValue("title", String.format("%%%s%%", filterOptions.getTitle().get()));
        }

        if (filterOptions.getTopic().isPresent() && !filterOptions.getTopic().get().isEmpty()) {
            filters.add("topic like :topic ");
            params.addValue("topic", String.format("%%%s%%", filterOptions.getTopic().get()));
        }
        if (filterOptions.getTeacher().isPresent() && !filterOptions.getTeacher().get().isEmpty()) {
            filters.add("email like :teacher ");
            params.addValue("teacher", String.format("%%%s%%", filterOptions.getTeacher().get()));
        }

        filterOptions.getIsPublic().ifPresent(value -> {
            if (value) {
                filters.add("is_published = 1 ");
            } else {
                filters.add("is_published = 0 ");
            }
        });

        if (!filters.isEmpty()) {
            queryString.append(" where ").append(String.join(" and ", filters));
        }
        queryString.append(" GROUP BY courses.id ");

        if (filterOptions.getRating().isPresent()) {
            queryString.append(String.format(" HAVING AVG(ratings.rating) >= %f ", filterOptions.getRating().get()));

            if (filterOptions.getRating().get() == 0) {
                queryString.append(" OR avg_rating IS NULL");
            }
//                sql += String.format(" HAVING AVG(ratings.rating) >= %f OR avg_rating IS NULL", filterOptions.getRating().get());
//            } else {
//                sql += String.format(" HAVING AVG(ratings.rating) >= %f ", filterOptions.getRating().get());
//            }
        }

        queryString.append(generateOrderBy(filterOptions));

        return namedParameterJdbcTemplate.query(queryString.toString(), params, COURSE_MAPPER);
    }

    private String generateOrderBy(FilterOptions filterOptions) {
        if (filterOptions.getSortBy().isEmpty() || filterOptions.getSortBy().get().isEmpty()) {
            return "";
        }

        String orderBy = switch (filterOptions.getSortBy().get()) {
            case "title" -> "title";
            case "rating" -> "avg_rating";
            default -> "";
        };

        orderBy = String.format(" order by %s", orderBy);

        if (filterOptions.getSortOrder().isPresent() && filterOptions.getSortOrder().get().equalsIgnoreCase("desc")) {
            orderBy = String.format("%s desc", orderBy);
        }

        return orderBy;
    }

    @Override
    public List<Course> getCompletedCoursesByUser(int userId) {
        String sql = "SELECT description, courses.id,title,start_date,creator_id,email,first_name,last_name,picture_url, " +
                "is_verified,is_published,passing_grade, topic, topic_id, AVG(ratings.rating) AS avg_rating " +
                "FROM course_user " +
                "LEFT JOIN courses ON course_user.course_id = courses.id " +
                "LEFT JOIN users ON course_user.user_id = users.id " +
                "LEFT JOIN topics ON courses.topic_id=topics.id " +
                "LEFT JOIN ratings ON courses.id = ratings.course_id " +
                "LEFT JOIN course_description on courses.id = course_description.course_id " +
                "WHERE course_user.user_id = :id AND course_user.ongoing = 0 " +
                "GROUP BY courses.id";


        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("id", userId);

        return namedParameterJdbcTemplate.query(sql, in, COURSE_MAPPER);

    }

    @Override
    public List<User> getStudentsWhichAreEnrolledForCourse(int courseId) {
        String sql = "SELECT users.id AS userId, email, password, first_name, last_name, picture_url, is_verified, " +
                "role_id, role " +
                "FROM course_user " +
                "LEFT JOIN users ON course_user.user_id = users.id " +
                "LEFT JOIN roles r ON r.id = users.role_id " +
                "WHERE course_id=:id";

        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("id", courseId);

        return namedParameterJdbcTemplate.query(sql, in, USER_MAPPER);
    }

    @Override
    public void enrollUserForCourse(int userId, int courseId) {
        String sql = "INSERT INTO course_user (course_id, user_id, ongoing)" +
                "VALUES (:course_id, :user_id, :ongoing)         ";
        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("course_id", courseId);
        in.addValue("user_id", userId);
        in.addValue("ongoing", 1);
        namedParameterJdbcTemplate.update(sql, in);
    }

    @Override
    public void completeCourse(int userId, int courseId) {
        String sql = "UPDATE course_user " +
                     "SET ongoing = 0 " +
                     "WHERE course_id = :course_id AND user_id = :user_id ";
        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("course_id", courseId);
        in.addValue("user_id", userId);

        namedParameterJdbcTemplate.update(sql, in);
    }


    @Override
    public List<Course> getOngoingCoursesByUser(int userId) {
        String sql = "SELECT description, courses.id, title, start_date, creator_id, email, first_name, last_name, picture_url,is_verified," +
                " is_published, passing_grade, topic, topic_id, AVG(ratings.rating) AS avg_rating  " +
                "FROM course_user " +
                "LEFT JOIN courses ON course_user.course_id = courses.id " +
                "LEFT JOIN topics ON courses.topic_id = topics.id " +
                "LEFT JOIN users ON courses.creator_id = users.id " +
                "  LEFT JOIN ratings ON courses.id = ratings.course_id " +
                " LEFT JOIN course_description on courses.id = course_description.course_id " +
                "WHERE course_user.user_id = :id AND course_user.ongoing = 1 " +
                "GROUP BY courses.id";
        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("id", userId);

        return namedParameterJdbcTemplate.query(sql, in, COURSE_MAPPER);

    }

    @Override
    public List<Course> getCoursesByCreator(int creatorId) {
        String sql = "SELECT description, courses.id, title, start_date, creator_id, email, first_name, last_name, picture_url,is_verified, " +
                " is_published, passing_grade, topic, topic_id , AVG(ratings.rating) AS avg_rating  " +
                "FROM courses " +
                "LEFT JOIN topics ON courses.topic_id = topics.id " +
                "LEFT JOIN users ON courses.creator_id = users.id " +
                "  LEFT JOIN ratings ON courses.id = ratings.course_id " +
                " LEFT JOIN course_description on courses.id = course_description.course_id " +
                "WHERE creator_id = :id " +
                "GROUP BY courses.id";

        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("id", creatorId);

        return namedParameterJdbcTemplate.query(sql, in, COURSE_MAPPER);

    }

    @Override
    public void create(Course course) {

        String sql = "INSERT INTO courses (title, topic_id, start_date,creator_id,is_published,passing_grade)" +
                "VALUES (:title,:topic_id,:start_date,:creator_id,:is_published,:passing_grade)         ";
        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("title", course.getTitle());
        in.addValue("topic_id", course.getTopic().getTopicId());
        in.addValue("start_date", course.getStartingDate());
        in.addValue("creator_id", course.getCreator().getUserId());
        in.addValue("is_published", course.isPublished());
        in.addValue("passing_grade", course.getPassingGrade());
        namedParameterJdbcTemplate.update(sql, in);

        if (course.getDescription() != null) {
            addDescription(course, in);
        }
    }


    @Override
    public void update(Course course) {

        String sql = "UPDATE courses SET title= :title, topic_id= :topic_id, start_date= :start_date,creator_id= :creator_id, is_published= :is_published, passing_grade= :is_published where courses.id= :course_id";

        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("title", course.getTitle());
        in.addValue("topic_id", course.getTopic().getTopicId());
        in.addValue("start_date", course.getStartingDate());
        in.addValue("creator_id", course.getCreator().getUserId());
        in.addValue("is_published", course.isPublished());
        in.addValue("passing_grade", course.getPassingGrade());
        in.addValue("course_id", course.getCourseId());

        namedParameterJdbcTemplate.update(sql, in);

        if (isDescriptionExist(course.getCourseId())) {
            if (course.getDescription() == null || course.getDescription().getDescription().isBlank()) {
                deleteDescription(in);
            } else {
                in.addValue("description", course.getDescription().getDescription());
                updateDescription(in);
            }
        } else if (course.getDescription() != null) {
            addDescription(course, in);
        }
    }

    @Override
    public void delete(int courseId) {
        String sql = "DELETE FROM courses where id= :course_id";

        MapSqlParameterSource in = new MapSqlParameterSource();


        in.addValue("course_id", courseId);


        namedParameterJdbcTemplate.update(sql, in);
    }

    @Override
    public void transferTeacherCourses(int teacherToTransferFromId, int teacherToTransferToId) {
        String sql = "UPDATE courses SET creator_id = :idNewTeacher WHERE creator_id = :idPreviousTeacher;";

        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("idPreviousTeacher", teacherToTransferFromId);
        in.addValue("idNewTeacher", teacherToTransferToId);
        namedParameterJdbcTemplate.update(sql, in);
    }


    @Override
    public void rateCourse(RatingDto rating, int courseId, int raterId) {

        String sql = "INSERT INTO ratings (course_id, rating, comment, user_id)" +
                "VALUES (:course_id, :rating, :comment, :user_id)         ";
        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("course_id", courseId);
        in.addValue("rating", rating.getRating());
        in.addValue("comment", rating.getComment());
        in.addValue("user_id", raterId);


        namedParameterJdbcTemplate.update(sql, in);
    }

    @Override
    public boolean isUserEnrolled(int userId, int courseId) {
        String sql = "SELECT COUNT(*) FROM course_user WHERE user_id =:userId AND course_id=:courseId AND ongoing=1";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("courseId", courseId);

        Integer result = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);

        return result != null && result > 0;
    }

    @Override
    public boolean hasUserPassedCourse(int userId, int courseId) {
        String sql = "SELECT COUNT(*) FROM course_user WHERE user_id =:userId AND course_id=:courseId AND ongoing=0";

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userId", userId);
        params.addValue("courseId", courseId);

        return !Objects.equals(namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class),0);
    }

    @Override
    public List<Integer> getIdOngoingCourses() {
        String sql = "SELECT course_id " +
                "FROM course_user " +
                "WHERE ongoing=1 " +
                "GROUP BY course_id";
        return namedParameterJdbcTemplate.queryForList(sql, new MapSqlParameterSource(), Integer.class);
    }

    @Override
    public void removeStudent(User user, Course course) {
        String sql = "DELETE FROM course_user WHERE course_id=:courseId AND user_id =:userId";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("courseId", course.getCourseId());
        params.addValue("userId", user.getUserId());
        namedParameterJdbcTemplate.update(sql, params);
    }

    @Override
    public Integer getPublishedCoursesCount() {
        String sql = "SELECT COUNT(*) FROM courses WHERE is_published = 1";
        return Optional.ofNullable(namedParameterJdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Integer.class)).orElse(0);
    }

    // TODO: 9.01.24 Consider moving this in a separate dao.
    @Override
    public List<Rating> getRatingsByCourseId(int courseId) {
        String sql = "SELECT rating, comment, user_id, course_id, email, first_name, last_name, picture_url " +
                " FROM ratings " +
                " LEFT JOIN users ON ratings.user_id = users.id " +
                " WHERE ratings.course_id = :id ";

        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("id", courseId);

        return namedParameterJdbcTemplate.query(sql, in, RATING_MAPPER);
    }


    private boolean isDescriptionExist(int courseId) {

        String sql = "SELECT COUNT(*) FROM course_description WHERE course_id = :course_id";

        MapSqlParameterSource param = new MapSqlParameterSource();
        param.addValue("course_id", courseId);

        return !Objects.equals(namedParameterJdbcTemplate.queryForObject(sql, param, Integer.class), 0);
    }


    // TODO: 9.01.24 consider removing MapSqlParameterSource param as a function parameter.
    private void addDescription(Course course, MapSqlParameterSource param) {

        // TODO: 9.01.24 remove this from here and add it to the update method.
        if (course.getCourseId() == null) {
            String courseIdSql = "SELECT id FROM courses WHERE title =:title";
            course.setCourseId(namedParameterJdbcTemplate.queryForObject(courseIdSql, param, Integer.class));
        }

        String descriptionSql = "INSERT INTO course_description (course_id,description) VALUES (:course_id,:description) ";

        param.addValue("course_id", course.getCourseId());
        param.addValue("description", course.getDescription().getDescription());
        namedParameterJdbcTemplate.update(descriptionSql, param);
    }


    private void deleteDescription(MapSqlParameterSource params) {
        String sql = "DELETE FROM course_description WHERE course_id =:course_id";
        namedParameterJdbcTemplate.update(sql, params);
    }


    private void updateDescription(MapSqlParameterSource params) {
        String sql = "UPDATE course_description SET description =:description WHERE course_id =:course_id";
        namedParameterJdbcTemplate.update(sql, params);
    }
}
