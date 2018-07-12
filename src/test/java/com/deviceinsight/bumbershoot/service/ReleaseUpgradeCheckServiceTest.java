package com.deviceinsight.bumbershoot.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.assertj.core.util.Maps;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.deviceinsight.bumbershoot.infrastructure.tiller.SwiftTillerClient;
import com.deviceinsight.bumbershoot.model.ChartIdentifier;
import com.deviceinsight.bumbershoot.model.ManagedChart;
import com.deviceinsight.bumbershoot.model.ManagedChart.UpgradePolicy;
import com.deviceinsight.bumbershoot.model.tiller.Chart;
import com.deviceinsight.bumbershoot.model.tiller.ChartMetaData;
import com.deviceinsight.bumbershoot.model.tiller.Release;
import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class ReleaseUpgradeCheckServiceTest {

	@Mock
	private SwiftTillerClient tillerClient;

	@Test
	public void test_is_umbrella_chart_upgrade_required_should_return_true_if_umbrella_chart_is_newer_or_snapshot() {
		var version = Version.valueOf("0.1.0-SNAPSHOT");
		var metaData = ChartMetaData.builder()
				.name("junit")
				.version(version.toString())
				.build();

		var chart = Chart.builder()
				.metaData(metaData)
				.build();

		var release = Release.builder()
				.chart(chart)
				.name("junit")
				.build();

		var managedChart = ManagedChart.builder()
				.name(metaData.getName())
				.build();
		var managedCharts = Lists.newArrayList(managedChart);

		var newChart = new ChartIdentifier(metaData.getName(), version.incrementMinorVersion().toString());
		var service = new ReleaseUpgradeCheckService(tillerClient, managedCharts);

		var result = service.isUmbrellaChartUpgradeRequired(release, newChart);
		assertThat(result.getHierarchyLevel()).isEqualTo(0);
		assertThat(result.isRequired()).isTrue();
		assertThat(result.getRelease()).isEqualTo(release);

		newChart = new ChartIdentifier(newChart.getName(), version.toString());
		result = service.isUmbrellaChartUpgradeRequired(release, newChart);
		assertThat(result.getHierarchyLevel()).isEqualTo(0);
		assertThat(result.isRequired()).isTrue();
		assertThat(result.getRelease()).isEqualTo(release);
	}

	@Test
	public void test_is_umbrella_chart_upgrade_required_should_return_false_if_umbrella_chart_is_not_machting_policy() {
		var version = Version.valueOf("0.2.0");
		var metaData = ChartMetaData.builder()
				.name("junit")
				.version(version.toString())
				.build();

		var chart = Chart.builder()
				.metaData(metaData)
				.build();

		var release = Release.builder()
				.chart(chart)
				.name("junit")
				.build();

		var managedChart = ManagedChart.builder()
				.name(metaData.getName())
				.build();
		var managedCharts = Lists.newArrayList(managedChart);

		var newChart = new ChartIdentifier(metaData.getName(), version.toString());
		var service = new ReleaseUpgradeCheckService(tillerClient, managedCharts);

		var result = service.isUmbrellaChartUpgradeRequired(release, newChart);
		assertThat(result.isRequired()).isFalse();
		assertThat(result.getRelease()).isEqualTo(release);

		newChart = new ChartIdentifier(newChart.getName(), "0.1.5");
		result = service.isUmbrellaChartUpgradeRequired(release, newChart);
		assertThat(result.isRequired()).isFalse();
		assertThat(result.getRelease()).isEqualTo(release);

		newChart = new ChartIdentifier(newChart.getName(), "1.0.0-SNAPSHOT");
		result = service.isUmbrellaChartUpgradeRequired(release, newChart);
		assertThat(result.isRequired()).isFalse();
		assertThat(result.getRelease()).isEqualTo(release);
	}

	@Test
	public void test_is_umbrella_chart_upgrade_required_should_return_true_if_subchart_is_matching_update_policy() {
		var metaData = ChartMetaData.builder()
				.name("junit")
				.version("0.1.0")
				.build();

		var subSubVersion = Version.valueOf("0.2.0");
		var subSubChart = Chart.builder()
				.metaData(metaData.toBuilder()
						.name("subsub")
						.version(subSubVersion.toString())
						.build())
				.build();

		var subVersion = Version.valueOf("0.2.0-SNAPSHOT");
		var subChart = Chart.builder()
				.metaData(metaData.toBuilder()
						.name("sub")
						.version(subVersion.toString())
						.build())
				.dependencies(Lists.newArrayList(subSubChart))
				.build();
		var chart = Chart.builder()
				.metaData(metaData)
				.dependencies(Lists.newArrayList(subChart))
				.build();

		var release = Release.builder()
				.chart(chart)
				.name("junit")
				.build();

		var managedChart = ManagedChart.builder()
				.name(metaData.getName())
				.build();
		var managedCharts = Lists.newArrayList(managedChart);

		verifyMinorUpgradePolicyUpgrades(subSubVersion, subSubChart, subVersion, subChart, release, managedCharts);
		verifyMajorUpgradePolicyUpgrades(subSubVersion, subSubChart, subVersion, subChart, release, managedCharts);
		verifyNeverUpgradePolicyUpgrades(subSubVersion, subSubChart, subVersion, subChart, release, managedCharts);

	}

	private void verifyMinorUpgradePolicyUpgrades(Version subSubVersion, Chart subSubChart, Version subVersion,
			Chart subChart, Release release, List<ManagedChart> managedCharts) {

		var service = new ReleaseUpgradeCheckService(tillerClient, managedCharts);

		// snapshot is always upgraded
		var newChart = new ChartIdentifier(subChart.getMetaData().getName(), subVersion.toString());
		var result = service.isUmbrellaChartUpgradeRequired(release, newChart);
		assertThat(result.getHierarchyLevel()).isEqualTo(1);
		assertThat(result.isRequired()).isTrue();
		assertThat(result.getRelease()).isEqualTo(release);

		// higher minor version
		newChart = new ChartIdentifier(subSubChart.getMetaData().getName(),
				subSubVersion.incrementMinorVersion().toString());
		result = service.isUmbrellaChartUpgradeRequired(release, newChart);
		assertThat(result.getHierarchyLevel()).isEqualTo(2);
		assertThat(result.isRequired()).isTrue();
		assertThat(result.getRelease()).isEqualTo(release);

		// major version upgrade
		newChart = new ChartIdentifier(subChart.getMetaData().getName(), subVersion.incrementMajorVersion().toString());
		result = service.isUmbrellaChartUpgradeRequired(release, newChart);
		assertThat(result.isRequired()).isFalse();
		assertThat(result.getRelease()).isEqualTo(release);
	}

	private void verifyMajorUpgradePolicyUpgrades(Version subSubVersion, Chart subSubChart, Version subVersion,
			Chart subChart, Release release, List<ManagedChart> managedCharts) {

		managedCharts = Lists.newArrayList(Iterables.getOnlyElement(managedCharts).toBuilder()
				.defaultUpgradePolicy(UpgradePolicy.MAJOR)
				.build());

		var service = new ReleaseUpgradeCheckService(tillerClient, managedCharts);

		// snapshot is always upgraded
		var newChart = new ChartIdentifier(subChart.getMetaData().getName(), subVersion.toString());
		var result = service.isUmbrellaChartUpgradeRequired(release, newChart);
		assertThat(result.getHierarchyLevel()).isEqualTo(1);
		assertThat(result.isRequired()).isTrue();
		assertThat(result.getRelease()).isEqualTo(release);

		// higher minor version
		newChart = new ChartIdentifier(subSubChart.getMetaData().getName(),
				subSubVersion.incrementMinorVersion().toString());
		result = service.isUmbrellaChartUpgradeRequired(release, newChart);
		assertThat(result.getHierarchyLevel()).isEqualTo(2);
		assertThat(result.isRequired()).isTrue();
		assertThat(result.getRelease()).isEqualTo(release);

		// major version upgrade
		newChart = new ChartIdentifier(subChart.getMetaData().getName(), subVersion.incrementMajorVersion().toString());
		result = service.isUmbrellaChartUpgradeRequired(release, newChart);
		assertThat(result.getHierarchyLevel()).isEqualTo(1);
		assertThat(result.isRequired()).isTrue();
		assertThat(result.getRelease()).isEqualTo(release);
	}

	private void verifyNeverUpgradePolicyUpgrades(Version subSubVersion, Chart subSubChart, Version subVersion,
			Chart subChart, Release release, List<ManagedChart> managedCharts) {

		managedCharts = Lists.newArrayList(Iterables.getOnlyElement(managedCharts).toBuilder()
				.defaultUpgradePolicy(UpgradePolicy.MAJOR)
				.upgradePolicies(Maps.newHashMap(subChart.getMetaData().getName(), UpgradePolicy.NEVER))
				.build());

		var service = new ReleaseUpgradeCheckService(tillerClient, managedCharts);

		// snapshot is not upgraded anymore
		var newChart = new ChartIdentifier(subChart.getMetaData().getName(), subVersion.toString());
		var result = service.isUmbrellaChartUpgradeRequired(release, newChart);
		assertThat(result.isRequired()).isFalse();
		assertThat(result.getRelease()).isEqualTo(release);

		// other chart is still upgraded
		newChart = new ChartIdentifier(subSubChart.getMetaData().getName(), subSubVersion.incrementMinorVersion().toString());
		result = service.isUmbrellaChartUpgradeRequired(release, newChart);
		assertThat(result.getHierarchyLevel()).isEqualTo(2);
		assertThat(result.isRequired()).isTrue();
		assertThat(result.getRelease()).isEqualTo(release);
	}

}
