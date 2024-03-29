package com.alpha53.virtualteacher.repositories;

import com.alpha53.virtualteacher.exceptions.EntityNotFoundException;
import com.alpha53.virtualteacher.models.FilterOptionsUsers;
import com.alpha53.virtualteacher.models.Role;
import com.alpha53.virtualteacher.models.User;
import com.alpha53.virtualteacher.models.dtos.GradedUserDtoOut;
import com.alpha53.virtualteacher.repositories.contracts.UserDao;
import com.alpha53.virtualteacher.utilities.mappers.UserMapper;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Transactional
@Repository
public class UserDaoImpl extends NamedParameterJdbcDaoSupport implements UserDao {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public UserDaoImpl(NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource dataSource) {
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.setDataSource(dataSource);
    }

    private static final UserMapper USER_MAPPER = new UserMapper();

    @Override
    public User get(int id) {
        String query = "SELECT users.id as userId, email, password, first_name, last_name, " +
                       "picture_url, role_id , role, is_verified " +
                       "FROM users JOIN roles r on r.id = users.role_id " +
                       "WHERE users.id = :id;";

        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("id", id);
        try {
            return namedParameterJdbcTemplate.queryForObject(query, in, USER_MAPPER);
        } catch (IncorrectResultSizeDataAccessException e) {
            throw new EntityNotFoundException("User", id);
        }
    }

    @Override
    public User get(String email) {
        String query = "SELECT users.id as userId, email, password, first_name, last_name, " +
                       "picture_url, is_verified, role_id , role " +
                       "FROM users JOIN roles r on r.id = users.role_id " +
                       "WHERE users.email = :email";

        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("email", email);
        try {
            return namedParameterJdbcTemplate.queryForObject(query, in, USER_MAPPER);
        } catch (IncorrectResultSizeDataAccessException e) {
            throw new EntityNotFoundException("User", "email", email);
        }
    }

    @Override
    public List<User> getAll(FilterOptionsUsers filterOptionsUsers) {
        StringBuilder queryString = new StringBuilder("SELECT users.id as userId, email, password, first_name, " +
                                                      "last_name, " +
                                                      "picture_url, is_verified, role_id , role " +
                                                      "FROM users JOIN roles r on r.id = users.role_id");
        List<String> filterAttributes = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (filterOptionsUsers.getEmail().isPresent() && !filterOptionsUsers.getEmail().get().isEmpty()){
            filterAttributes.add(" email like :email ");
            params.addValue("email", String.format("%%%s%%", filterOptionsUsers.getEmail().get()));
        }

        if (filterOptionsUsers.getFirstName().isPresent() && !filterOptionsUsers.getFirstName().get().isEmpty()){
            filterAttributes.add(" first_name like :firstName");
            params.addValue("firstName", String.format("%%%s%%", filterOptionsUsers.getFirstName().get()));
        }

        if (filterOptionsUsers.getLastName().isPresent() && !filterOptionsUsers.getLastName().get().isEmpty()){
            filterAttributes.add(" last_name like :lastName");
            params.addValue("lastName", String.format("%%%s%%", filterOptionsUsers.getLastName().get()));
        }

        if (filterOptionsUsers.getRoleType().isPresent() && !filterOptionsUsers.getRoleType().get().isEmpty()){
            filterAttributes.add(" role like :roleType");
            params.addValue("roleType", String.format("%%%s%%", filterOptionsUsers.getRoleType().get()));
        }

        if (!filterAttributes.isEmpty()) {
            queryString.append(" where ").append(String.join(" and ", filterAttributes));
        }
        queryString.append(generateOrderBy(filterOptionsUsers));


        return namedParameterJdbcTemplate.query(queryString.toString(),params, USER_MAPPER);

    }

    private String generateOrderBy(FilterOptionsUsers filterOptionsUsers) {
        if (filterOptionsUsers.getSortBy().isEmpty() || (filterOptionsUsers.getSortBy().isPresent()
                && filterOptionsUsers.getSortBy().get().isEmpty())) {
            return "";
        }

        String orderBy = switch (filterOptionsUsers.getSortBy().get()) {
            case "email" -> "email";
            case "firstName" -> "first_name";
            case "lastName" -> "last_name";
            case "roleType" -> "role";
            default -> "";
        };

        orderBy = String.format(" order by %s", orderBy);

        if (filterOptionsUsers.getSortOrder().isPresent()
                && filterOptionsUsers.getSortOrder().get().equalsIgnoreCase("desc")) {
            orderBy = String.format("%s desc", orderBy);
        }
        return orderBy;
    }

    @Override
    public boolean emailExists(String email) {
        try {
            get(email);
            return true;
        } catch (EntityNotFoundException e) {
            return false;
        }
    }

    @Override
    public void create(User user) {
        String sql = "INSERT INTO users (email, password, first_name, last_name, role_id, picture_url) " +
                     "VALUES (:email, :password, :first_name, :last_name, :role_id, :picture_url)";

        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("email", user.getEmail());
        in.addValue("password", user.getPassword());
        in.addValue("first_name", user.getFirstName());
        in.addValue("last_name", user.getLastName());
        in.addValue("role_id", user.getRole().getRoleId());
        in.addValue("picture_url", user.getPictureUrl());

        namedParameterJdbcTemplate.update(sql, in);
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE users SET email = :email, password = :password, " +
                     "first_name = :firstName, last_name = :lastName, " +
                     "role_id = :role, picture_url = :pictureUrl, is_verified = :isVerified " +
                     "WHERE id = :userId";

        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("userId", user.getUserId());
        in.addValue("email", user.getEmail());
        in.addValue("password", user.getPassword());
        in.addValue("firstName", user.getFirstName());
        in.addValue("lastName", user.getLastName());
        in.addValue("role", user.getRole().getRoleId());
        in.addValue("pictureUrl", user.getPictureUrl());
        in.addValue("isVerified", user.isVerified());

        namedParameterJdbcTemplate.update(sql, in);
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM users WHERE id = :id";
        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("id", id);
        namedParameterJdbcTemplate.update(sql, in);
    }

    public Role getRole(String roleType) {
        String query = "SELECT id as roleId, role as roleType " +
                       "FROM roles " +
                       "WHERE role like :roleType;";

        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("roleType", roleType);

        try {
            return namedParameterJdbcTemplate.queryForObject(query, in, new BeanPropertyRowMapper<>(Role.class));
        } catch (IncorrectResultSizeDataAccessException e) {
            throw new EntityNotFoundException(String.format("No role %s found.", roleType));
        }
    }

    public List<Role> getRoles() {
        String query = "SELECT id as roleId, role as roleType " +
                "FROM roles";

            return namedParameterJdbcTemplate.query(query,new BeanPropertyRowMapper<>(Role.class));
    }

    @Override
    public List<GradedUserDtoOut> getStudentsByLectureId(int lectureId) {
        String sql = "SELECT users.id as userId,                         " +
                "users.email as email,                                   " +
                "users.first_name as firstName,                          " +
                "users.last_name as lastName,                            " +
                "solutions.lecture_id as lectureId,                      " +
                "solutions.id as solutionId,                             " +
                "solutions.solution_url as solutionUrl,                  " +
                "lectures.course_id as courseId,                         " +
                "solutions.grade as grade                                " +
                "FROM solutions                                          " +
                " LEFT JOIN users ON solutions.user_id = users.id        " +
                "LEFT JOIN  lectures ON solutions.lecture_id=lectures.id " +
                "WHERE lecture_id = :lectureId;                          ";

        MapSqlParameterSource param = new MapSqlParameterSource();
        param.addValue("lectureId",lectureId);
        return namedParameterJdbcTemplate.query(sql,param,new BeanPropertyRowMapper<>(GradedUserDtoOut.class));
    }


}
