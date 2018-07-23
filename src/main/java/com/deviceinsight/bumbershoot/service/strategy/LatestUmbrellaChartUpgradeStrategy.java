package com.deviceinsight.bumbershoot.service.strategy;

import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;

import com.deviceinsight.bumbershoot.exception.ChartDownloadException;
import com.deviceinsight.bumbershoot.exception.ChartInvalidException;
import com.deviceinsight.bumbershoot.exception.ChartUpgradeException;
import com.deviceinsight.bumbershoot.exception.ChartUploadException;
import com.deviceinsight.bumbershoot.exception.UnsupportedChartRepositoryType;
import com.deviceinsight.bumbershoot.infrastructure.chartrepository.ChartRepositoryClientFactory;
import com.deviceinsight.bumbershoot.model.ChartRepository;
import com.deviceinsight.bumbershoot.model.ChartUpdateNotification;
import com.deviceinsight.bumbershoot.model.tiller.ChartMetaData;
import com.deviceinsight.bumbershoot.model.tiller.Release;
import com.deviceinsight.bumbershoot.service.ChartArchiveModifier;

import com.github.zafarkhaja.semver.Version;

import lombok.extern.slf4j.Slf4j;

/**
 * Strategy upgrading releases, when latest version of an umbrella chart is deployed.
 */
@Slf4j
public class LatestUmbrellaChartUpgradeStrategy extends SubChartUpgradeStrategy {

	private static final String SNAPSHOT_VERSION = "SNAPSHOT";

	public LatestUmbrellaChartUpgradeStrategy(ChartRepositoryClientFactory chartRepositoryClientFactory,
			ChartArchiveModifier archiveModifier) {

		super(chartRepositoryClientFactory, archiveModifier);
	}

	@Override
	public boolean canUpgradeUmbrellaChart(Version currentVersion, Collection<Version> availableVersions) {
		var highestVersion = getHighestCompatibleToMajorVersion(currentVersion, availableVersions);
		
		if (highestVersion.isPresent()) {
			return currentVersion.greaterThanOrEqualTo(highestVersion.get())
					|| isCompatibleSnapshotTo(currentVersion, highestVersion.get());
		}
		
		return true;
	}

	@Override
	public void upgradeUmbrellaChart(Release release, ChartRepository umbrellaChartRepository,
			ChartUpdateNotification update, Collection<Version> availableVersions, ChartDeployFunction deployFunction)
			throws ChartUpgradeException {

		var umbrellaChart = release.getChart().getMetaData();
		Version currentVersion = Version.valueOf(umbrellaChart.getVersion());
		var highestVersion = getHighestCompatibleToMajorVersion(currentVersion, availableVersions);
		if (highestVersion.isPresent() && highestVersion.get().greaterThan(currentVersion)) {
			umbrellaChart.setVersion(highestVersion.get().toString());
		}
		
		File umbrellaArchive;
		try {
			umbrellaArchive = downloadChart(umbrellaChart, umbrellaChartRepository);

			var newChartMetaData = update.getChart().toTillerChartMetaData();
			File newChartArchive = downloadChart(newChartMetaData, update.getRepository());

			umbrellaChart = modifyUmbrellaChart(availableVersions, umbrellaChart, umbrellaArchive,
					newChartMetaData, newChartArchive);

			String newChartUrl = uploadChart(umbrellaChartRepository, umbrellaChart, umbrellaArchive);
			deployFunction.deployChart(newChartUrl, release);

		} catch (ChartDownloadException | UnsupportedChartRepositoryType | ChartInvalidException
				| ChartUploadException e) {
			log.error("Failed to upgrade chart", e);
			throw new ChartUpgradeException(e);

		}
	}
	
	private Version buildNextVersion(Version currentVersion, Collection<Version> availableVersions) {
		var nextVersion = getHighestCompatibleToMajorVersion(currentVersion, availableVersions)
				.orElse(currentVersion);

		if (nextVersion.getPreReleaseVersion().isEmpty()) {
			nextVersion = nextVersion.incrementMinorVersion();
			nextVersion = nextVersion.setPreReleaseVersion(SNAPSHOT_VERSION);
		}

		if (nextVersion.getPreReleaseVersion().contains("bumbershoot")) {
			nextVersion = nextVersion.incrementPreReleaseVersion();
		} else {
			nextVersion = nextVersion.setPreReleaseVersion(nextVersion.getPreReleaseVersion() + "-bumbershoot.1");
		}

		return nextVersion;
	}

	private Optional<Version> getHighestCompatibleToMajorVersion(Version currentVersion, Collection<Version> availableVersions) {
		var compatibleWithExpression = String.format("%d.x", currentVersion.getMajorVersion());

		return availableVersions.stream()
				.filter(v -> v.satisfies(compatibleWithExpression))
				.max(Comparator.naturalOrder());
	}

	private boolean isCompatibleSnapshotTo(Version currentVersion, Version latestVersion) {
		return currentVersion.getPreReleaseVersion().toUpperCase().startsWith(SNAPSHOT_VERSION)
				&& latestVersion.getPreReleaseVersion().toUpperCase().startsWith(SNAPSHOT_VERSION)
				&& Version.valueOf(currentVersion.getNormalVersion()).equals(
						Version.valueOf(latestVersion.getNormalVersion()));
	}
	
	private ChartMetaData modifyUmbrellaChart(Collection<Version> availableVersions, ChartMetaData umbrellaChart,
			File umbrellaArchive,
			ChartMetaData newChartMetaData, File newChartArchive) throws ChartInvalidException {

		archiveModifier.updateChartDependency(umbrellaChart, newChartMetaData, umbrellaArchive, newChartArchive);
		Version newVersion = buildNextVersion(Version.valueOf(umbrellaChart.getVersion()), availableVersions);
		archiveModifier.setChartVersion(umbrellaChart, umbrellaArchive, newVersion);

		return umbrellaChart.toBuilder()
				.version(newVersion.toString())
				.build();

	}

}
