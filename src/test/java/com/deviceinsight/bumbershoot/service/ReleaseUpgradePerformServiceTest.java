package com.deviceinsight.bumbershoot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.assertj.core.util.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.deviceinsight.bumbershoot.exception.ChartNotFoundException;
import com.deviceinsight.bumbershoot.exception.ChartUpgradeException;
import com.deviceinsight.bumbershoot.infrastructure.chartrepository.ChartRepositoryClient;
import com.deviceinsight.bumbershoot.infrastructure.chartrepository.ChartRepositoryClientFactory;
import com.deviceinsight.bumbershoot.infrastructure.tiller.SwiftTillerClient;
import com.deviceinsight.bumbershoot.model.ChartIdentifier;
import com.deviceinsight.bumbershoot.model.ChartRepository;
import com.deviceinsight.bumbershoot.model.ChartUpdateNotification;
import com.deviceinsight.bumbershoot.model.ManagedChart;
import com.deviceinsight.bumbershoot.model.ReleaseUpgradeCheck;
import com.deviceinsight.bumbershoot.model.ReleaseUpgradeResult.Status;
import com.deviceinsight.bumbershoot.model.tiller.Chart;
import com.deviceinsight.bumbershoot.model.tiller.ChartMetaData;
import com.deviceinsight.bumbershoot.model.tiller.Release;
import com.deviceinsight.bumbershoot.model.tiller.ReleaseContent;
import com.deviceinsight.bumbershoot.model.tiller.ReleaseInfo;
import com.deviceinsight.bumbershoot.model.tiller.ReleaseStatus;
import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ReleaseUpgradePerformServiceTest {

	private ChartIdentifier chartIdentifier = new ChartIdentifier("junit", "0.2.0-SNAPSHOT");

	private ChartMetaData umbrellaMetaData = new ChartMetaData("", "junit", "0.1.0");

	private Chart umbrellaChart = Chart.builder()
			.metaData(umbrellaMetaData)
			.build();

	private Release release = Release.builder()
			.chart(umbrellaChart)
			.name("junit")
			.version(0)
			.build();

	private ChartRepository umbrellaRepository = new ChartRepository();

	private ManagedChart managedChart = ManagedChart.builder()
			.name(umbrellaChart.getMetaData().getName())
			.repository(umbrellaRepository)
			.build();

	private List<ManagedChart> managedCharts = Lists.newArrayList(managedChart);

	private ChartRepositoryClient chartRepositoryClient = mock(ChartRepositoryClient.class);

	private ChartRepositoryClientFactory chartMuseumClientFactory = new ChartRepositoryClientFactory(
			Maps.newHashMap("chartmuseum", chartRepositoryClient));

	@Mock
	private ChartRepository repository;

	@Mock
	private SwiftTillerClient tillerClient;

	@Test
	public void test_upgrade_umbrella_chart_with_no_fitting_subchart_stragey_should_skip() {
		List<SubChartUpgradeStrategy> upgradeStrategies = Collections.emptyList();
		var svc = new ReleaseUpgradePerformService(managedCharts, chartMuseumClientFactory, tillerClient,
				upgradeStrategies);

		var releaseUpgrade = ReleaseUpgradeCheck.builder()
				.hierarchyLevel(1)
				.required(true)
				.release(release)
				.build();

		var notification = new ChartUpdateNotification(chartIdentifier, repository);

		when(repository.getType()).thenReturn("unsupported");
		assertThat(svc.upgradeRelease(releaseUpgrade, notification).getStatus())
				.isEqualTo(Status.UNCHANGED);
	}

	@Test
	public void test_upgrade_with_umbrella_chart_should_upgrade_umbrella_chart_directly()
			throws ChartNotFoundException {
		List<SubChartUpgradeStrategy> upgradeStrategies = Collections.emptyList();
		var svc = new ReleaseUpgradePerformService(managedCharts, chartMuseumClientFactory, tillerClient,
				upgradeStrategies);

		var releaseUpgrade = ReleaseUpgradeCheck.builder()
				.hierarchyLevel(0)
				.required(true)
				.release(release)
				.build();

		var notification = new ChartUpdateNotification(chartIdentifier, repository);
		mockSuccessfulUpgrade();

		assertThat(svc.upgradeRelease(releaseUpgrade, notification).getStatus())
				.isEqualTo(Status.UPGRADE_SUCCEEDED);
		verify(chartRepositoryClient).getChartFileUrl(nullable(URL.class), eq(umbrellaMetaData.getName()),
				eq("0.2.0-SNAPSHOT"));
		verify(tillerClient).upgradeRelease(eq(release.getName()), any());
	}

	@Test
	public void test_upgrade_with_subchart_should_delegate_to_subchart_upgrade_strategy() throws ChartUpgradeException {
		var upgradeEverythingStrategy = mock(SubChartUpgradeStrategy.class);
		when(upgradeEverythingStrategy.canUpgradeUmbrellaChart(any(), anyCollection()))
				.thenReturn(true);
		var upgradeStrategies = Lists.newArrayList(upgradeEverythingStrategy);
		var svc = new ReleaseUpgradePerformService(managedCharts, chartMuseumClientFactory, tillerClient,
				upgradeStrategies);

		var releaseUpgrade = ReleaseUpgradeCheck.builder()
				.hierarchyLevel(1)
				.required(true)
				.release(release)
				.build();

		var notification = new ChartUpdateNotification(chartIdentifier, repository);
		mockSuccessfulUpgrade();

		assertThat(svc.upgradeRelease(releaseUpgrade, notification).getStatus())
				.isEqualTo(Status.UPGRADE_SUCCEEDED);
		
		verify(upgradeEverythingStrategy)
			.upgradeUmbrellaChart(eq(release), eq(umbrellaRepository), eq(notification), anyCollection(), any());
		
	}

	private void mockSuccessfulUpgrade() {
		ReleaseContent releaseContent = new ReleaseContent(
				release.toBuilder()
						.version(release.getVersion() + 1)
						.info(ReleaseInfo.builder()
								.status(new ReleaseStatus("DEPLOYED"))
								.build())
						.build());

		when(tillerClient.getReleaseContent(eq(release.getName())))
				.thenReturn(releaseContent);
	}

}
