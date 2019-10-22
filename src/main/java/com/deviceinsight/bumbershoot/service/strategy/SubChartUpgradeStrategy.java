package com.deviceinsight.bumbershoot.service.strategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import org.apache.commons.compress.utils.IOUtils;

import com.deviceinsight.bumbershoot.exception.ChartDownloadException;
import com.deviceinsight.bumbershoot.exception.ChartUpgradeException;
import com.deviceinsight.bumbershoot.exception.ChartUploadException;
import com.deviceinsight.bumbershoot.exception.UnsupportedChartRepositoryType;
import com.deviceinsight.bumbershoot.infrastructure.chartrepository.ChartRepositoryClient;
import com.deviceinsight.bumbershoot.infrastructure.chartrepository.ChartRepositoryClientFactory;
import com.deviceinsight.bumbershoot.model.ChartRepository;
import com.deviceinsight.bumbershoot.model.ChartUpdateNotification;
import com.deviceinsight.bumbershoot.model.tiller.ChartMetaData;
import com.deviceinsight.bumbershoot.model.tiller.Release;
import com.deviceinsight.bumbershoot.service.ChartArchiveModifier;

import com.github.zafarkhaja.semver.Version;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SubChartUpgradeStrategy {

	private final ChartRepositoryClientFactory chartRepositoryClientFactory;
	protected final ChartArchiveModifier archiveModifier;

	public SubChartUpgradeStrategy(ChartRepositoryClientFactory chartRepositoryClientFactory, 
			ChartArchiveModifier archiveModifier) {
		
		this.chartRepositoryClientFactory = chartRepositoryClientFactory;
		this.archiveModifier = archiveModifier;
	}

	public abstract boolean canUpgradeUmbrellaChart(Version currentVersion, Collection<Version> availableVersions);

	/**
	 * Upgrade the umbrella chart injecting the new sub chart 
	 * @param release the release to update
	 * @param umbrellaChartRepository repository holding the umbrella chart
	 * @param update sub chart notification information
	 * @param availableVersions available versions of the umbrella chart
	 * @param deployFunction function to deploy new chart
	 * @return the version deployed
	 * @throws ChartUpgradeException
	 */
	public abstract void upgradeUmbrellaChart(Release release, ChartRepository umbrellaChartRepository,
			ChartUpdateNotification update, Collection<Version> availableVersions, ChartDeployFunction deployFunction)
			throws ChartUpgradeException;

	protected String uploadChart(ChartRepository repository, ChartMetaData metaData, File chartArchive)
			throws ChartUploadException, UnsupportedChartRepositoryType {

		log.info("Uploading chart {} to repository {}", metaData, repository.getUrl());

		ChartRepositoryClient client = chartRepositoryClientFactory.getClientForType(repository.getType());
		return client.uploadChart(repository.getUrl(), metaData.getName(), metaData.getVersion(), chartArchive);

	}

	protected File downloadChart(ChartMetaData chartMetaData, ChartRepository chartRepository)
			throws ChartDownloadException, UnsupportedChartRepositoryType {

		ChartRepositoryClient client = chartRepositoryClientFactory.getClientForType(chartRepository.getType());

		File targetChartArchive;
		try {
			targetChartArchive = generateTempFileForChartArchive(chartMetaData);
			try (FileOutputStream fis = new FileOutputStream(targetChartArchive)) {
				client.downloadChart(chartRepository.getUrl(), chartMetaData.getName(), chartMetaData.getVersion(),
						is -> IOUtils.copy(is, fis));
			}
		} catch (IOException e) {
			throw new ChartDownloadException(e);
		}

		return targetChartArchive;
	}

	private File generateTempFileForChartArchive(ChartMetaData chartMetaData) throws IOException {
		return File.createTempFile(String.format("%s-%s-%s", chartMetaData.getName(), chartMetaData.getVersion(),
				UUID.randomUUID().toString()), ".zip");
	}
	
	
	@FunctionalInterface
	public static interface ChartDeployFunction {
		
		void deployChart(String chartUrl, Release release) throws ChartUpgradeException;
		
	}

}
