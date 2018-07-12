package com.deviceinsight.bumbershoot.config.interfaces.rest;

import com.deviceinsight.bumbershoot.facade.UmbrellaChartUpgradeFacade;
import com.deviceinsight.bumbershoot.interfaces.rest.ChartUpdateNotificationController;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestConfiguration {

	@Bean
	public ChartUpdateNotificationController chartUpdateNotificationController(UmbrellaChartUpgradeFacade facade) {
		return new ChartUpdateNotificationController(facade);
	}

}
