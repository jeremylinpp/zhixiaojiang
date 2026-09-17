package com.zhixiaojiang.config;

import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 全局配置。
 *
 * <p>用 Java 配置而不是 application.yml：映射文件与 Mapper 接口同包同名（resources 下镜像包路径），
 * MyBatis 会自动加载，无需再声明 mapper-locations；配置集中在这里，不与部署配置混在一起。
 */
@Configuration
public class MyBatisConfig {

    @Bean
    ConfigurationCustomizer myBatisConfigurationCustomizer() {
        return configuration -> {
            // 库内下划线列名 → PO/VO 的驼峰属性，避免依赖数据库对别名大小写的处理差异
            configuration.setMapUnderscoreToCamelCase(true);
            // 传入 null 时使用 jdbcType=NULL，避免驱动对未知类型报错
            configuration.setJdbcTypeForNull(JdbcType.NULL);
            // 单条语句超时，避免慢查询把连接池占满
            configuration.setDefaultStatementTimeout(30);
        };
    }
}
