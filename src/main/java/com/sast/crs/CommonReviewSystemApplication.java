package com.sast.crs;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan("com.sast.crs.mapper")
@EnableScheduling
@SpringBootApplication
public class CommonReviewSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(CommonReviewSystemApplication.class, args);
	}

}
