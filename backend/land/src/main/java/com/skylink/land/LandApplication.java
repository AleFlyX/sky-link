package com.skylink.land;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.skylink.land.mapper")
public class LandApplication {

	public static void main(String[] args) {
		SpringApplication.run(LandApplication.class, args);
	}

}
