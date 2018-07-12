package com.deviceinsight.bumbershoot.facade;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.deviceinsight.bumbershoot.model.ChartUpdateNotification;
import com.deviceinsight.bumbershoot.model.ChartUpdateNotificationResult;
import com.deviceinsight.bumbershoot.model.ReleaseUpgradeCheck;
import com.deviceinsight.bumbershoot.model.ReleaseUpgradeResult;
import com.deviceinsight.bumbershoot.model.ReleaseUpgradeResult.Status;
import com.deviceinsight.bumbershoot.service.ReleaseUpgradeCheckService;
import com.deviceinsight.bumbershoot.service.ReleaseUpgradePerformService;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UmbrellaChartUpgradeFacade {

	private final ReleaseUpgradeCheckService releaseUpgradeCheck;

	private final ReleaseUpgradePerformService releaseUpgradePerform;

	public UmbrellaChartUpgradeFacade(ReleaseUpgradeCheckService releaseUpgradeCheck,
			ReleaseUpgradePerformService releaseUpgradePerform) {
		this.releaseUpgradeCheck = releaseUpgradeCheck;
		this.releaseUpgradePerform = releaseUpgradePerform;
	}

	public ChartUpdateNotificationResult upgradeUmbrellaCharts(ChartUpdateNotification notification) {
		var releaseUpgradeCandidates = getReleaseUpgradeCandidates(notification);
		var tooDeepHierarchyUpgrades = filterCandidatesNotUpgradeable(releaseUpgradeCandidates);
		var releasesToUpdate = Sets.difference(releaseUpgradeCandidates, tooDeepHierarchyUpgrades);
		log.info("Upgrading releases {}", releasesToUpdate);

		var upgradeResult = releasesToUpdate.stream()
				.map(r -> releaseUpgradePerform.upgradeRelease(r, notification))
				.collect(Collectors.toSet());

		upgradeResult.addAll(convertTooDeepHiearachyResult(tooDeepHierarchyUpgrades));
		return new ChartUpdateNotificationResult(upgradeResult);

	}

	private Set<ReleaseUpgradeCheck> filterCandidatesNotUpgradeable(
			Collection<ReleaseUpgradeCheck> releaseUpgradeCandidates) {
		var result = releaseUpgradeCandidates.stream()
				.filter(r -> r.getHierarchyLevel() > 1)
				.collect(Collectors.toSet());

		log.info("Filtered out releases with potential upgrades in deep hierarchy: {}", result);
		return result;
	}

	private Set<ReleaseUpgradeCheck> getReleaseUpgradeCandidates(ChartUpdateNotification notification) {
		var result = releaseUpgradeCheck.getReleaseUpgradeCandidates(notification);
		log.info("Found following releases upgrade candidates {}", result);
		return result;
	}

	private Collection<ReleaseUpgradeResult> convertTooDeepHiearachyResult(
			Set<ReleaseUpgradeCheck> tooDeepHierarchyUpgrades) {
		
		return tooDeepHierarchyUpgrades.stream()
				.map(r -> ReleaseUpgradeResult.builder()
						.chart(r.getRelease().getChart().getMetaData())
						.details("hierarchy level is too deep for upgrade")
						.releaseName(r.getRelease().getName())
						.releaseNamespace(r.getRelease().getNamespace())
						.status(Status.UNCHANGED)
						.build())
				.collect(Collectors.toSet());
	}

}
