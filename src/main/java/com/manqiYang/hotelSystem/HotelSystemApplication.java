package com.manqiYang.hotelSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan("com.manqiYang.hotelSystem.mapper")
public class HotelSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(HotelSystemApplication.class, args);
	}

}

