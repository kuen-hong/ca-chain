/*
 * Copyright© 2023. HwaCom Systems Inc. All Rights Reserved.
 * No part of this software or any of its contents may be reproduced, copied,
 * modified or adapted, without the prior written consent of HwaCom Systems Inc.,
 * unless otherwise indicated for stand-alone materials.
 */
package idv.kun.server2;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @author kunhung.lin
 */
@RequestMapping("/api")
@RestController
public class GreetingController {

    @GetMapping("/greeting")
    public String greeting() {
        return "from server 2, hi there.";
    }

    @GetMapping("/retrieve")
    public String retrieve() {
        String url = "https://server1.kun-dev.net:8581/api/greeting";
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
        return resp.getBody();
    }
}
