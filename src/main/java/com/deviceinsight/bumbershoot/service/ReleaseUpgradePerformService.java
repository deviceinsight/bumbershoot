package com.deviceinsight.bumbershoot.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.deviceinsight.bumbershoot.exception.ChartNotFoundException;
import com.deviceinsight.bumbershoot.exception.ChartUpgradeException;
import com.deviceinsight.bumbershoot.exception.UnsupportedChartRepositoryType;
import com.deviceinsight.bumbershoot.infrastructure.chartrepository.ChartRepositoryClient;
import com.deviceinsight.bumbershoot.infrastructure.chartrepository.ChartRepositoryClientFactory;
import com.deviceinsight.bumbershoot.infrastructure.tiller.SwiftTillerClient;
import com.deviceinsight.bumbershoot.model.ChartRepository;
import com.deviceinsight.bumbershoot.model.ChartUpdateNotification;
import com.deviceinsight.bumbershoot.model.ManagedChart;
import com.deviceinsight.bumbershoot.model.ReleaseUpgradeCheck;
import com.deviceinsight.bumbershoot.model.ReleaseUpgradeResult;
import com.deviceinsight.bumbershoot.model.ReleaseUpgradeResult.Status;
import com.deviceinsight.bumbershoot.model.tiller.Chart;
import com.deviceinsight.bumbershoot.model.tiller.ChartUpgrade;
import com.deviceinsight.bumbershoot.model.tiller.Release;
import com.deviceinsight.bumbershoot.service.strategy.SubChartUpgradeStrategy;

import com.github.zafarkhaja.semver.Version;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReleaseUpgradePerformService {

	private final ChartRepositoryClientFactory clientFactory;
	private final SwiftTillerClient tillerClient;
	private final List<SubChartUpgradeStrategy> upgradeStrategies;

	private final Map<String, ManagedChart> managedCharts;

	public ReleaseUpgradePerformService(List<ManagedChart> managedCharts,
			ChartRepositoryClientFactory clientFactory,
			SwiftTillerClient tillerClient,
			List<SubChartUpgradeStrategy> upgradeStrategies) {

		this.clientFactory = clientFactory;
		this.tillerClient = tillerClient;
		this.upgradeStrategies = upgradeStrategies;

		this.managedCharts = managedCharts.stream()
				.collect(Collectors.toMap(ManagedChart::getName, Function.identity()));
	}

	public ReleaseUpgradeResult upgradeRelease(ReleaseUpgradeCheck releaseUpgrade,
			ChartUpdateNotification notification) {

		Chart umbrellaChart = releaseUpgrade.getRelease().getChart();
		var umbrellaChartRepository = managedCharts.get(umbrellaChart.getMetaData().getName()).getRepository();

		try {
			if (releaseUpgrade.getHierarchyLevel() == 0) {
				upgradeUmbrellaChart(releaseUpgrade, umbrellaChartRepository,
						notification.getChart().getVersion());
			} else {
				var client = clientFactory.getClientForType(umbrellaChartRepository.getType());

				var availableVersions = client.getVersionsOfChart(umbrellaChartRepository.getUrl(),
						umbrellaChart.getMetaData().getName());
				var currentVersion = Version.valueOf(umbrellaChart.getMetaData().getVersion());

				var strategy = upgradeStrategies.stream()
						.filter(s -> s.canUpgradeUmbrellaChart(currentVersion, availableVersions))
						.findFirst();

				if (strategy.isPresent()) {
					strategy.get().upgradeUmbrellaChart(releaseUpgrade.getRelease(),
							umbrellaChartRepository, notification, availableVersions, this::deployChart);
				} else {
					log.info("Found no strategy to handle upgrade");
					return ReleaseUpgradeResult.builder()
							.details("No strategy found to handle upgrade")
							.releaseName(releaseUpgrade.getRelease().getName())
							.releaseNamespace(releaseUpgrade.getRelease().getNamespace())
							.status(Status.UNCHANGED)
							.build();
				}
			}

			return ReleaseUpgradeResult
					.fromReleaseStatus(getReleaseStatus(releaseUpgrade.getRelease()));

		} catch (UnsupportedChartRepositoryType | ChartUpgradeException e) {
			log.error("Failed to upgrade release", e);
			return ReleaseUpgradeResult.builder()
					.details(e.getMessage())
					.releaseName(releaseUpgrade.getRelease().getName())
					.releaseNamespace(releaseUpgrade.getRelease().getNamespace())
					.status(Status.UPGRADE_FAILED)
					.build();
		}
	}

	private Release getReleaseStatus(Release release) throws ChartUpgradeException {
		int currentVersion = release.getVersion();

		int tries = 3;
		while (--tries >= 0) {
			Release newRelease;
			try {
				newRelease = CompletableFuture.supplyAsync(() -> tillerClient.getReleaseContent(release.getName()),
						CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS)).get().getRelease();
				if (newRelease.getVersion() > currentVersion) {
					return newRelease;
				}
			} catch (ExecutionException e) {
				log.warn(String.format("Failed to get release status. Tries %d left", tries), e);
			} catch (InterruptedException e) {
				log.warn("Interrupted on retrieving release status", e);
				tries = 0;
				Thread.currentThread().interrupt();
			}
		}

		throw new ChartUpgradeException("Tiller upgrade call failed");
	}

	private void upgradeUmbrellaChart(ReleaseUpgradeCheck releaseUpgrade, ChartRepository umbrellaChartRepository,
			String version) throws ChartUpgradeException {

		ChartRepositoryClient client;
		try {
			client = clientFactory.getClientForType(umbrellaChartRepository.getType());
			String chartUrl = client.getChartFileUrl(umbrellaChartRepository.getUrl(),
					releaseUpgrade.getRelease().getChart().getMetaData().getName(), version);

			deployChart(chartUrl, releaseUpgrade.getRelease());
		} catch (UnsupportedChartRepositoryType | ChartNotFoundException e) {
			log.error("Failed to get chart for release upgrade", e);
			throw new ChartUpgradeException(e);
		}
	}

	private void deployChart(String chartUrl, Release release) {
		log.info("Upgrading release {} with new chart from {}", release.getName(), chartUrl);
		log.debug("... with config {}", release.getConfig());
		try {
			tillerClient.upgradeRelease(release.getName(), new ChartUpgrade(chartUrl, release.getConfig()));
		} catch (FeignException e) {
			log.warn("Tiler/Swift returned error on upgrade, will check release state afterwards", e);
		}
	}

}
