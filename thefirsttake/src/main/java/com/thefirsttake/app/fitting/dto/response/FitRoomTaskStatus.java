package com.thefirsttake.app.fitting.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class FitRoomTaskStatus {
    @JsonProperty("task_id")
    private String taskId;
    private String status;
    private Integer progress;
    @JsonProperty("download_signed_url")
    private String downloadSignedUrl;
    private String error;
}
