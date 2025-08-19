package com.thefirsttake.app.fitting.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FitRoomTaskResponse {
    @JsonProperty("task_id")
    private String taskId;
    private String status;
}
