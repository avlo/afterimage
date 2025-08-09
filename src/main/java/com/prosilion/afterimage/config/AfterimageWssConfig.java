package com.prosilion.afterimage.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    name = "server.ssl.enabled",
    havingValue = "true")
public class AfterimageWssConfig extends AfterimageBaseConfig {}
