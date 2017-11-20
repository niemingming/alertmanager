package com.haier.alertmanager.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * @description mongo配置类，用于去掉java自动生成的_class字段
 * @date 2017/11/16
 * @author Niemingming
 */
@Configuration
public class MongoConfiguration {
    @Autowired
    private MongoDbFactory mongoDbFactory;
    @Autowired
    private MongoMappingContext mongoMappingContext;

    @Bean
    public MappingMongoConverter mappingMongoConverter(){
        DefaultDbRefResolver refResolver = new DefaultDbRefResolver(mongoDbFactory);
        MappingMongoConverter converter = new MappingMongoConverter(refResolver,mongoMappingContext);
        //将默认的mapper类型设置为null，这样就不会再库中生成额外字段。
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return converter;
    }
}
