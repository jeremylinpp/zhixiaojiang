package com.zhixiaojiang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zhixiaojiang.dao")
public class ServerApplication {
  public static void main(String[] args) { SpringApplication.run(ServerApplication.class, args); }
}
