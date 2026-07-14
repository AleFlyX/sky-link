package com.skylink.land;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "DB_USERNAME=test",
    "DB_PASSWORD=test",
    "JWT_SECRET=0123456789abcdef0123456789abcdef",
    "skylink.bootstrap.security-data-enabled=false"
})
class LandApplicationTests {

    @Test
    void contextLoads() {
    }

}
