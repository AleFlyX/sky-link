package com.skylink.land.config;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusProperties;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import javax.sql.DataSource;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@EnableConfigurationProperties(MybatisPlusProperties.class)
public class MybatisPlusConfiguration {

    @Value("${mybatis-plus.configuration.map-underscore-to-camel-case:true}")
    private boolean mapUnderscoreToCamelCase;

    @Value("${mybatis-plus.configuration.log-impl:org.apache.ibatis.logging.nologging.NoLoggingImpl}")
    private Class<? extends Log> logImpl;

    @Bean
    public SqlSessionFactory sqlSessionFactory(
        DataSource dataSource,
        MybatisPlusProperties properties,
        ObjectProvider<Interceptor> interceptorsProvider
    )
        throws Exception {
        // 手动组装 MyBatis-Plus 的工厂：它负责把实体、Mapper、XML 和数据源连成可执行 SQL 的会话。
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTypeAliasesPackage(properties.getTypeAliasesPackage());
        factoryBean.setTypeAliasesSuperType(properties.getTypeAliasesSuperType());
        factoryBean.setTypeHandlersPackage(properties.getTypeHandlersPackage());
        factoryBean.setConfigurationProperties(properties.getConfigurationProperties());
        factoryBean.setDefaultScriptingLanguageDriver(properties.getDefaultScriptingLanguageDriver());

        Resource[] mapperLocations = properties.resolveMapperLocations();
        if (mapperLocations.length > 0) {
            // 复杂查询仍可写在 Mapper XML 中；这里把配置扫描到的 XML 注册给 MyBatis。
            factoryBean.setMapperLocations(mapperLocations);
        }

        MybatisConfiguration configuration = new MybatisConfiguration();
        // 让数据库列 create_time 能自动映射到 Java 字段 createTime，减少重复映射代码。
        configuration.setMapUnderscoreToCamelCase(mapUnderscoreToCamelCase);
        configuration.setLogImpl(logImpl);
        factoryBean.setConfiguration(configuration);

        if (properties.getGlobalConfig() != null) {
            factoryBean.setGlobalConfig(properties.getGlobalConfig());
        }

        Interceptor[] interceptors = interceptorsProvider.orderedStream().toArray(Interceptor[]::new);
        if (interceptors.length > 0) {
            // 将 Spring 中注册的 MyBatis 拦截器（例如分页）挂到同一个 SqlSessionFactory。
            factoryBean.setPlugins(interceptors);
        }

        return factoryBean.getObject();
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // selectPage 会依赖此插件按 MySQL 方言生成分页 SQL，而不是一次把所有记录查回内存。
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
