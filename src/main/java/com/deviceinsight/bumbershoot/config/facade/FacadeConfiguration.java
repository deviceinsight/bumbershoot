package com.deviceinsight.bumbershoot.config.facade;

import com.deviceinsight.bumbershoot.facade.UmbrellaChartUpgradeFacade;
import com.deviceinsight.bumbershoot.service.ReleaseUpgradeCheckService;
import com.deviceinsight.bumbershoot.service.ReleaseUpgradePerformService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FacadeConfiguration {

	@Bean
	public UmbrellaChartUpgradeFacade umbrellChartUpdateFacade(ReleaseUpgradeCheckService releaseUpgradeCheck,
			ReleaseUpgradePerformService releaseUpgradePerform) {

		return new UmbrellaChartUpgradeFacade(releaseUpgradeCheck, releaseUpgradePerform);
	}

}
