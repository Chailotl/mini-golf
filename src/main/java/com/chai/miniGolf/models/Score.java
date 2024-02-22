package com.chai.miniGolf.models;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
@Data
public class Score {
    private Integer score;
    private Long timestamp;
}
