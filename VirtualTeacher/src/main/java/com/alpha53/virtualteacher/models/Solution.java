package com.alpha53.virtualteacher.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Solution {
    @NotNull
    @Positive(message = "User ID must be positive integer")
    private int userId;
    @NotNull
    @Positive(message = "Lecture ID must be positive integer")
    private int lectureId;
    @NotNull
    @Positive(message = "Solution ID must be positive integer")
    private int solutionId;
    private String solutionUrl;
    @NotNull
    @Positive(message = "Grade must be positive")
    private double grade;
    @NotNull
    @Positive(message = "Course ID must be positive integer")
    private int courseId;

    public Solution(int userId, int lectureId, double grade) {
        this.userId = userId;
        this.lectureId = lectureId;
        this.grade = grade;
    }
}
