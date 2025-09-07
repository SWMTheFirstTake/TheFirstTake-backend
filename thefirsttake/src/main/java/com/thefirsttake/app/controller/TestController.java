package com.thefirsttake.app.controller;

import com.thefirsttake.app.common.response.CommonResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/test")
    public CommonResponse test() {
        return CommonResponse.success("Test endpoint is working!");
    }
}
