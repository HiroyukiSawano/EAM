package com.eam.assetcenter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.eam.assetcenter.infrastructure.mapper")
/**
 * 信息化资产管理中心后端启动类。
 */
public class AssetCenterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AssetCenterApplication.class, args);
    }
}


