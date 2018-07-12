package com.deviceinsight.bumbershoot.config;

import com.deviceinsight.bumbershoot.config.facade.FacadeConfiguration;
import com.deviceinsight.bumbershoot.config.infrastructure.InfrastructureConfiguration;
import com.deviceinsight.bumbershoot.config.interfaces.rest.RestConfiguration;
import com.deviceinsight.bumbershoot.config.service.ServiceConfiguration;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableConfigurationProperties(BumbershootConfigurationProperties.class)
@Import({
		InfrastructureConfiguration.class,
		RestConfiguration.class,
		FacadeConfiguration.class,
		ServiceConfiguration.class})
public class BumbershootConfiguration {

}
