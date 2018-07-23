package com.deviceinsight.bumbershoot.config.service;

import com.deviceinsight.bumbershoot.config.BumbershootConfigurationProperties;
import com.deviceinsight.bumbershoot.infrastructure.chartrepository.ChartRepositoryClientFactory;
import com.deviceinsight.bumbershoot.infrastructure.tiller.SwiftTillerClient;
import com.deviceinsight.bumbershoot.service.ChartArchiveModifier;
import com.deviceinsight.bumbershoot.service.ReleaseUpgradeCheckService;
import com.deviceinsight.bumbershoot.service.ReleaseUpgradePerformService;
import com.deviceinsight.bumbershoot.service.LatestUmbrellaChartUpgradeStrategy;
import com.deviceinsight.bumbershoot.service.SubChartUpgradeStrategy;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfiguration {

	@Bean
	public ReleaseUpgradePerformService chartUpgradePerformService(BumbershootConfigurationProperties properties,
			ChartRepositoryClientFactory clientFactory, SwiftTillerClient tillerClient,
			List<SubChartUpgradeStrategy> upgradeStrategies) {

		return new ReleaseUpgradePerformService(properties.getManagedCharts(), clientFactory, tillerClient,
				upgradeStrategies);
	}

	@Bean
	public ReleaseUpgradeCheckService chartUpgradeCheckService(BumbershootConfigurationProperties properties,
			SwiftTillerClient tillerClient) {
		return new ReleaseUpgradeCheckService(tillerClient, properties.getManagedCharts());
	}

	@Bean
	public LatestUmbrellaChartUpgradeStrategy latestUmbrellaChartUpgradeStragey(
			ChartRepositoryClientFactory chartRepositoryClientFactory) {

		return new LatestUmbrellaChartUpgradeStrategy(chartRepositoryClientFactory, chartArchiveModifier());
	}

	@Bean
	public ChartArchiveModifier chartArchiveModifier() {
		return new ChartArchiveModifier();
	}

}
