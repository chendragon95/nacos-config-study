package com.chenlongji.nacosconfigstudy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author clj
 */
@SpringBootApplication (scanBasePackages = {"com.chenlongji"})
public class NacosConfigStudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(NacosConfigStudyApplication.class, args);
    }

}
