package com.alpha53.virtualteacher.models;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class RatingDto {
    private String comment;
    @NotNull
    private double rating;
    @NotNull
    private int courseId;

}
