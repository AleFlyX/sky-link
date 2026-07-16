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
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTypeAliasesPackage(properties.getTypeAliasesPackage());
        factoryBean.setTypeAliasesSuperType(properties.getTypeAliasesSuperType());
        factoryBean.setTypeHandlersPackage(properties.getTypeHandlersPackage());
        factoryBean.setConfigurationProperties(properties.getConfigurationProperties());
        factoryBean.setDefaultScriptingLanguageDriver(properties.getDefaultScriptingLanguageDriver());

        Resource[] mapperLocations = properties.resolveMapperLocations();
        if (mapperLocations.length > 0) {
            factoryBean.setMapperLocations(mapperLocations);
        }

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(mapUnderscoreToCamelCase);
        configuration.setLogImpl(logImpl);
        factoryBean.setConfiguration(configuration);

        if (properties.getGlobalConfig() != null) {
            factoryBean.setGlobalConfig(properties.getGlobalConfig());
        }

        Interceptor[] interceptors = interceptorsProvider.orderedStream().toArray(Interceptor[]::new);
        if (interceptors.length > 0) {
            factoryBean.setPlugins(interceptors);
        }

        return factoryBean.getObject();
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
