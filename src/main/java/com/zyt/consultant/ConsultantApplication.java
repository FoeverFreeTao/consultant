package com.zyt.consultant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
@MapperScan("com.zyt.consultant.mapper")
@SpringBootApplication
public class ConsultantApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsultantApplication.class, args);
    }

}
