package com.umlpilot.umlpilot_api.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

@Configuration
public class ChatMongoConfig {

    @Bean
    @ConditionalOnMissingBean(GridFsTemplate.class)
    public GridFsTemplate gridFsTemplate(MongoDatabaseFactory databaseFactory, MongoConverter converter) {
        return new GridFsTemplate(databaseFactory, converter);
    }
}
