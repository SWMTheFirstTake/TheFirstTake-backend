package com.thefirsttake.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")  // 테스트 시 'test' 프로파일 활성화
class ThefirsttakeApplicationTests {

	// @Test
	// void contextLoads() {
	// }

	// @Test
	// void sanityCheck() {
	// 	org.junit.jupiter.api.Assertions.assertEquals(4, 2 + 2);
	// }

}
