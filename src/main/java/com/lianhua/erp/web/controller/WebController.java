package com.lianhua.erp.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebController {
    
    // 匹配所有非 API 且不包含「.」的路徑，例如 /home、/user/profile、/dashboard
    @RequestMapping("/{path:[^\\.]*}")
    public String forward() {
        return "forward:/index.html";
    }
}
