
package com.cloudtask.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminAuthorizationTestController {

    @GetMapping("/api/admin/test")
    public String adminEndpoint() {
        return "Admin access granted";
    }

    @GetMapping("/api/manager/test")
    public String managerEndpoint() {
        return "Manager access granted";
    }
}

