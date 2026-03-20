package com.eam.assetcenter.config;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置，统一注册分页与自动填充能力。
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器，提供分页等通用能力。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    /**
     * 注册字段自动填充处理器，统一维护创建时间和更新时间。
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            /**
             * 在新增记录时自动填充公共字段。
             */
            @Override
            public void insertFill(MetaObject metaObject) {
                LocalDateTime now = LocalDateTime.now();
                strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
                strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
                strictInsertFill(metaObject, "deleted", Integer.class, 0);
            }

            /**
             * 在更新记录时自动填充公共字段。
             */
            @Override
            public void updateFill(MetaObject metaObject) {
                strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}


