package com.yoganavi.lecture;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication(scanBasePackages = {
	"com.yoganavi.lecture",
	"com.yoganavi.kafka"
})
@EnableJpaRepositories(basePackages = {
	"com.yoganavi.lecture.common.repository",
	"com.yoganavi.kafka.repository"
})
@EntityScan(basePackages = {
	"com.yoganavi.lecture.common.entity",
	"com.yoganavi.kafka.entity"
})
public class LiveLectureServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(LiveLectureServiceApplication.class, args);
	}

}
