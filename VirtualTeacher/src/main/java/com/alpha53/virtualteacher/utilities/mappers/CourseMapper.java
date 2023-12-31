package com.alpha53.virtualteacher.utilities.mappers;

import com.alpha53.virtualteacher.models.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class CourseMapper implements RowMapper<Course> {


    @Override
    public Course mapRow(ResultSet rs, int rowNum) throws SQLException {
        Course course = new Course();
        Topic topic = new Topic();
        User creator = new User();
        CourseDescription description = new CourseDescription();

        description.setDescription(rs.getString("description"));
        description.setCourseId(rs.getInt("id"));
        course.setCourseId(rs.getInt("id"));
        course.setTitle(rs.getString("title"));
        course.setStartingDate(rs.getDate("start_date").toLocalDate());
        course.setPublished(rs.getBoolean("is_published"));
        course.setPassingGrade(rs.getDouble("passing_grade"));

        //TODO course dto

        // TODO: 29.11.23 Viktor: consider removing all user fields and leaving only the ID of the creator.
        creator.setUserId(rs.getInt("creator_id"));
        creator.setEmail(rs.getString("email"));
        creator.setFirstName(rs.getString("first_name"));
        creator.setLastName(rs.getString("last_name"));
        creator.setPictureUrl(rs.getString("picture_url"));
        creator.setVerified(rs.getBoolean("is_verified"));
        topic.setTopicId(rs.getInt("topic_id"));
        topic.setTopic(rs.getString("topic"));

        course.setCreator(creator);
        course.setTopic(topic);
        course.setAvgRating(rs.getDouble("avg_rating"));
        course.setDescription(description);
        return course;
    }


}

