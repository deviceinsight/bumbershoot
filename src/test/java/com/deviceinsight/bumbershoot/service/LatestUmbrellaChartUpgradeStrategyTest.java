package com.deviceinsight.bumbershoot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.deviceinsight.bumbershoot.exception.ChartInvalidException;
import com.deviceinsight.bumbershoot.exception.ChartUpgradeException;
import com.deviceinsight.bumbershoot.exception.UnsupportedChartRepositoryType;
import com.deviceinsight.bumbershoot.infrastructure.chartrepository.ChartRepositoryClient;
import com.deviceinsight.bumbershoot.infrastructure.chartrepository.ChartRepositoryClientFactory;
import com.deviceinsight.bumbershoot.model.ChartIdentifier;
import com.deviceinsight.bumbershoot.model.ChartRepository;
import com.deviceinsight.bumbershoot.model.ChartUpdateNotification;
import com.deviceinsight.bumbershoot.model.tiller.Chart;
import com.deviceinsight.bumbershoot.model.tiller.ChartMetaData;
import com.deviceinsight.bumbershoot.model.tiller.Release;
import com.github.zafarkhaja.semver.Version;

@RunWith(MockitoJUnitRunner.Silent.class)
public class LatestUmbrellaChartUpgradeStrategyTest {
	
	@Mock
	private Release release;
	
	@Mock
	private Chart chart;
	
	@Mock
	private ChartMetaData chartMeta;
	
	@Mock
	private ChartRepository repository;
	
	@Mock
	private ChartUpdateNotification update;
	
	// dependencies
	
	@Mock
	private ChartArchiveModifier chartModifier;
	
	@Mock
	private ChartRepositoryClientFactory clientFactory;
	
	@InjectMocks
	private LatestUmbrellaChartUpgradeStrategy strategy;
	
	@Before
	public void mockBehavior() throws MalformedURLException, UnsupportedChartRepositoryType {
		when(release.getChart()).thenReturn(chart);
		when(chart.getMetaData()).thenReturn(chartMeta);
		when(clientFactory.getClientForType(any())).thenReturn(mock(ChartRepositoryClient.class));
		when(update.getRepository()).thenReturn(repository);
		when(chartMeta.toBuilder()).thenCallRealMethod();
	}
	
	@Test
	public void test_check_can_upgrade_umbrella_chart_should_return_true_if_is_latest_snapshot_chart() {
		Version currentVersion = Version.valueOf("0.3.0-SNAPSHOT-bumbershoot.2");
		Collection<Version> availableVersions = Arrays.asList(
				Version.valueOf("0.1.0"),
				Version.valueOf("0.2.0"),
				Version.valueOf("0.3.0-SNAPSHOT-bumbershoot.1")
		);
		
		assertThat(strategy.canUpgradeUmbrellaChart(currentVersion, availableVersions)).isTrue();
	}
	
	@Test
	public void test_check_can_upgrade_umbrella_chart_should_return_true_if_is_latest_release() {
		Version currentVersion = Version.valueOf("0.3.0");
		Collection<Version> availableVersions = Arrays.asList(
				Version.valueOf("0.1.0"),
				Version.valueOf("0.2.0"),
				Version.valueOf("0.3.0")
		);
		
		assertThat(strategy.canUpgradeUmbrellaChart(currentVersion, availableVersions)).isTrue();
	}
	
	@Test
	public void test_check_can_upgrade_umbrella_chart_should_return_false_if_there_is_newer_release() {
		Version currentVersion = Version.valueOf("0.2.0");
		Collection<Version> availableVersions = Arrays.asList(
				Version.valueOf("0.1.0"),
				Version.valueOf("0.2.0"),
				Version.valueOf("0.3.0")
		);
		
		assertThat(strategy.canUpgradeUmbrellaChart(currentVersion, availableVersions)).isFalse();
	}
	
	@Test
	public void test_check_can_upgrade_umbrella_chart_should_return_false_if_there_is_newer_snapshot() {
		Version currentVersion = Version.valueOf("0.3.0-SNAPSHOT");
		Collection<Version> availableVersions = Arrays.asList(
				Version.valueOf("0.1.0"),
				Version.valueOf("0.2.0"),
				Version.valueOf("0.3.0-SNAPSHOT"),
				Version.valueOf("0.3.0-SNAPSHOT-bumbershoot.1")
		);
		
		assertThat(strategy.canUpgradeUmbrellaChart(currentVersion, availableVersions)).isFalse();
	}
	
	@Test
	public void test_check_can_upgrade_umbrella_chart_should_return_false_if_there_is_release_of_current_snapshot() {
		Version currentVersion = Version.valueOf("0.3.0-SNAPSHOT");
		Collection<Version> availableVersions = Arrays.asList(
				Version.valueOf("0.1.0"),
				Version.valueOf("0.2.0"),
				Version.valueOf("0.3.0-SNAPSHOT"),
				Version.valueOf("0.3.0")
		);
		
		assertThat(strategy.canUpgradeUmbrellaChart(currentVersion, availableVersions)).isFalse();
	}
	
	@Test
	public void test_upgrade_umbrella_chart_should_use_bumbershoot_snapshot() throws ChartUpgradeException, ChartInvalidException {
		Version currentVersion = Version.valueOf("0.3.0-SNAPSHOT");
		Collection<Version> availableVersions = Arrays.asList(
				Version.valueOf("0.1.0"),
				Version.valueOf("0.2.0"),
				Version.valueOf("0.3.0-SNAPSHOT")
		);
		
		when(chartMeta.getVersion()).thenReturn(currentVersion.toString());
		ChartIdentifier identifier = new ChartIdentifier("sub", "0.2.0");
		when(update.getChart()).thenReturn(identifier);
		
		AtomicBoolean deployed = new AtomicBoolean(false);
		strategy.upgradeUmbrellaChart(release, repository, update, availableVersions, (url, r) -> deployed.set(true));
		
		verify(chartModifier).setChartVersion(any(), any(), eq(Version.valueOf("0.3.0-SNAPSHOT-bumbershoot.1")));
		
		assertThat(deployed.get()).isTrue();
	}
	
	@Test
	public void test_upgrade_umbrella_chart_for_released_version_should_bump_minor_version() throws ChartUpgradeException, ChartInvalidException {
		Version currentVersion = Version.valueOf("0.3.0");
		Collection<Version> availableVersions = Arrays.asList(
				Version.valueOf("0.1.0"),
				Version.valueOf("0.2.0"),
				Version.valueOf("0.3.0")
		);
		
		when(chartMeta.getVersion()).thenReturn(currentVersion.toString());
		ChartIdentifier identifier = new ChartIdentifier("sub", "0.2.0");
		when(update.getChart()).thenReturn(identifier);
		
		AtomicBoolean deployed = new AtomicBoolean(false);
		strategy.upgradeUmbrellaChart(release, repository, update, availableVersions, (url, r) -> deployed.set(true));
		
		verify(chartModifier).setChartVersion(any(), any(), eq(Version.valueOf("0.4.0-SNAPSHOT-bumbershoot.1")));
		
		assertThat(deployed.get()).isTrue();
	}

}
