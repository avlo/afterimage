package com.prosilion.afterimage.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    name = "server.ssl.enabled",
    havingValue = "false",
    matchIfMissing = true)
public class AfterimageWsConfig extends AfterimageBaseConfig {}
