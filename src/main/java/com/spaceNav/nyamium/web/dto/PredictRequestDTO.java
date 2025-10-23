package com.spaceNav.nyamium.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class PredictRequestDTO {

        Float e1;
        Float e2;
        Float e3;
        Float e4;
        Float l1;
        Float l2;
        Float l3;
        Float l4;

        Float rangingError;
        Float delta;
        Float fd;
        Float sigma;
}
