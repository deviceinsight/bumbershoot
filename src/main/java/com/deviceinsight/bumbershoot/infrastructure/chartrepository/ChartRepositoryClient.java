package com.deviceinsight.bumbershoot.infrastructure.chartrepository;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

import com.deviceinsight.bumbershoot.exception.ChartDownloadException;
import com.deviceinsight.bumbershoot.exception.ChartNotFoundException;
import com.deviceinsight.bumbershoot.exception.ChartUploadException;
import com.github.zafarkhaja.semver.Version;

public interface ChartRepositoryClient {

	void downloadChart(URL baseUrl, String name, String version,  ChartDownloadConsumer responseConsumer)
			throws ChartDownloadException;
	
	String getChartFileUrl(URL baseUrl, String name, String version) throws ChartNotFoundException;

	String uploadChart(URL baseUrl, String name, String version, File chartArchive) throws ChartUploadException;

	Collection<Version> getVersionsOfChart(URL baseUrl, String name);

	@FunctionalInterface
	public static interface ChartDownloadConsumer {

		void downloadChart(InputStream inputStream) throws IOException;

	}
}
