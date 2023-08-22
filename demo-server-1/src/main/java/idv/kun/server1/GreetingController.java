/*
 * Copyright© 2023. HwaCom Systems Inc. All Rights Reserved.
 * No part of this software or any of its contents may be reproduced, copied,
 * modified or adapted, without the prior written consent of HwaCom Systems Inc.,
 * unless otherwise indicated for stand-alone materials.
 */
package idv.kun.server1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author kunhung.lin
 */
@RequestMapping("/api")
@RestController
public class GreetingController {

    @GetMapping("/greeting")
    public String greeting() {
        return "from server 1, hi there.";
    }
}
