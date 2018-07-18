package com.deviceinsight.bumbershoot.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.deviceinsight.bumbershoot.infrastructure.tiller.SwiftTillerClient;
import com.deviceinsight.bumbershoot.model.ChartIdentifier;
import com.deviceinsight.bumbershoot.model.ChartUpdateNotification;
import com.deviceinsight.bumbershoot.model.ManagedChart;
import com.deviceinsight.bumbershoot.model.ManagedChart.UpgradePolicy;
import com.deviceinsight.bumbershoot.model.ReleaseUpgradeCheck;
import com.deviceinsight.bumbershoot.model.tiller.Chart;
import com.deviceinsight.bumbershoot.model.tiller.Release;
import com.github.zafarkhaja.semver.Version;
import com.google.common.annotations.VisibleForTesting;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReleaseUpgradeCheckService {

	private final SwiftTillerClient tillerClient;

	private final Map<String, ManagedChart> managedCharts;


	public ReleaseUpgradeCheckService(SwiftTillerClient tillerClient, List<ManagedChart> managedCharts) {
		this.tillerClient = tillerClient;
		this.managedCharts = managedCharts.stream()
				.collect(Collectors.toMap(ManagedChart::getName, Function.identity()));
	}

	public Set<ReleaseUpgradeCheck> getReleaseUpgradeCandidates(ChartUpdateNotification notification) {
		return tillerClient.getReleases().getReleases().stream()
				.map(r -> tillerClient.getReleaseContent(r.getName()).getRelease())
				.map(r -> isUmbrellaChartUpgradeRequired(r, notification.getChart()))
				.filter(ReleaseUpgradeCheck::isRequired)
				.collect(Collectors.toSet());
	}

	private ReleaseUpgradeCheck isChartUpgradeRequired(Chart baseChart, ChartIdentifier newChart, ManagedChart managedChart,
			int level) {

		var deployedVersion = Version.valueOf(baseChart.getMetaData().getVersion());
		var updateVersion = Version.valueOf(newChart.getVersion());
		if (updateVersion.greaterThan(deployedVersion)
				|| updateVersion.equals(deployedVersion) && updateVersion.getPreReleaseVersion().contains("SNAPSHOT")) {

			var upgradePolicy = managedChart.getUpgradePolicies().getOrDefault(newChart.getName(),
					managedChart.getDefaultUpgradePolicy());

			if (isUpgradeRequiredForUpgradePolicy(deployedVersion, updateVersion, upgradePolicy)) {
				log.info("Upgrading, since chart {} should be upgraded to {}",
						baseChart.getMetaData(), newChart);

				return new ReleaseUpgradeCheck(true, level);
			} else {
				log.info("Skipping upgrade: upgrade policy {} prevents upgrade from {} to {}",
						upgradePolicy, baseChart.getMetaData(), newChart);
			}

		} else {
			log.info("Skipping upgrade: version of deployed chart {} is newer than chart of notification {}",
					baseChart.getMetaData(), newChart);
		}

		return new ReleaseUpgradeCheck(false, level);
	}

	@VisibleForTesting
	protected ReleaseUpgradeCheck isUmbrellaChartUpgradeRequired(Release release, ChartIdentifier chartIdentifier) {
		ReleaseUpgradeCheck upgrade;
		var umbrellaChart = release.getChart();
		var managedChart = managedCharts.get(umbrellaChart.getMetaData().getName());
		if (managedChart != null) {
			log.info("Found managed chart {} of release {}. Checking if upgrade is necessary",
					umbrellaChart.getMetaData(), release.getInfo());

			upgrade = isUpgradeForChartInHierarchy(umbrellaChart, chartIdentifier, managedChart, 0);

		} else {
			log.debug("Ignoring unmanaged chart {} of release {}",
					umbrellaChart.getMetaData(), release.getInfo());

			upgrade = new ReleaseUpgradeCheck(false, 0);
		}

		upgrade.setRelease(release);
		return upgrade;
	}

	private ReleaseUpgradeCheck isUpgradeForChartInHierarchy(Chart rootChart, ChartIdentifier chart,
			ManagedChart managedChart, int level) {

		if (rootChart.getMetaData().getName().equals(chart.getName())) {
			return isChartUpgradeRequired(rootChart, chart, managedChart, level);
		} else {
			ReleaseUpgradeCheck upgrade = new ReleaseUpgradeCheck(false, level);
			for (Chart subChart : rootChart.getDependencies()) {
				upgrade = isUpgradeForChartInHierarchy(subChart, chart, managedChart, level + 1);
				if (upgrade.isRequired()) {
					break;
				}
			}

			return upgrade;
		}
	}

	private boolean isUpgradeRequiredForUpgradePolicy(Version deployedVersion, Version updateVersion,
			UpgradePolicy updatePolicy) {

		boolean doUpgrade;
		switch (updatePolicy) {
			case MAJOR:
				doUpgrade = true;
				break;
			case MINOR:
				doUpgrade = deployedVersion.getMajorVersion() == updateVersion.getMajorVersion();
				break;
			case NEVER:
			default:
				doUpgrade = false;
				break;
		}

		return doUpgrade;
	}

}
