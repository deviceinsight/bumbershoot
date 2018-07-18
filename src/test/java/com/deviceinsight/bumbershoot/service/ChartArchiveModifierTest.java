package com.deviceinsight.bumbershoot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.junit.Test;

import com.deviceinsight.bumbershoot.exception.ChartInvalidException;
import com.deviceinsight.bumbershoot.model.tiller.ChartMetaData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.zafarkhaja.semver.Version;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;


public class ChartArchiveModifierTest {

	private ChartArchiveModifier archiveModifier = new ChartArchiveModifier();

	private ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

	@Test
	public void test_set_chart_version_should_modify_version_in_file()
			throws ArchiveException, ChartInvalidException, IOException {

		File chartArchive = getTemporaryArchiveCopy("umbrella-chart-0.1.0-SNAPSHOT.tgz");

		assertThat(chartArchive.exists()).isTrue();

		ChartMetaData metaData = new ChartMetaData();
		metaData.setName("umbrella-chart");
		Version newVersion = Version.valueOf("0.2.0");
				
		archiveModifier.setChartVersion(metaData, chartArchive, newVersion);

		try (TarArchiveInputStream is =
				new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(chartArchive)))) {
			ArchiveEntry entry = null;
			File chart = File.createTempFile(UUID.randomUUID().toString(), ".yaml");

			while ((entry = is.getNextEntry()) != null) {
				if (entry.getName().equalsIgnoreCase("umbrella-chart/chart.yaml")) {
					chart.deleteOnExit();

					try (FileOutputStream fos = new FileOutputStream(chart)) {
						IOUtils.copy(is, fos);
					}
				}
			}

			assertThat(yamlMapper.readTree(chart).get("version").asText()).isEqualTo(newVersion.toString());

		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	@SuppressWarnings("deprecation")
	@Test
	public void test_update_dependency_should_exchange_dependency_version_and_sub_archive()
			throws IOException, ChartInvalidException {

		File chartArchive = getTemporaryArchiveCopy("umbrella-chart-0.1.0-SNAPSHOT.tgz");
		File subArchive = getTemporaryArchiveCopy("test-chart-0.2.0-SNAPSHOT.tgz");

		ChartMetaData umbrellaMetadata = new ChartMetaData();
		umbrellaMetadata.setName("umbrella-chart");

		ChartMetaData subChartMetadata = new ChartMetaData();
		subChartMetadata.setName("test-chart");
		subChartMetadata.setVersion("0.2.0-SNAPSHOT");

		archiveModifier.updateChartDependency(umbrellaMetadata, subChartMetadata, chartArchive, subArchive);

		try (TarArchiveInputStream is =
				new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(chartArchive)))) {
			ArchiveEntry entry = null;

			byte[] extractedRequirements = new byte[0];
			byte[] extractedArchive = new byte[0];

			while ((entry = is.getNextEntry()) != null) {
				if (entry.getName().equalsIgnoreCase("umbrella-chart/requirements.yaml")) {
					extractedRequirements = IOUtils.toByteArray(is);
				} else if (entry.getName().equalsIgnoreCase("umbrella-chart/charts/test-chart-0.2.0-SNAPSHOT.tgz")) {
					extractedArchive = IOUtils.toByteArray(is);
				} else if (entry.getName().contains("remote-service")){
					System.err.println(entry.getName());
				}
			}

			try (FileInputStream expected = new FileInputStream(subArchive)) {
				assertThat(Hashing.md5().hashBytes(extractedArchive)).isEqualTo(
						Hashing.md5().hashBytes(IOUtils.toByteArray(expected)));
			}
			
			boolean dependencyFound = false;
			for (JsonNode dependency : yamlMapper.readTree(extractedRequirements).get("dependencies")) {
				if (dependency.get("name").asText().equals("test-chart")) {
					assertThat(dependency.get("version").asText()).isEqualTo("0.2.0-SNAPSHOT");
					dependencyFound = true;
					break;
				}
			}
			
			assertThat(dependencyFound).isTrue();

		} catch (IOException e) {
			fail(e.getMessage());
		}
	}

	private File getTemporaryArchiveCopy(String archive) throws IOException {
		File chartSource = new File(getClass().getClassLoader().getResource(archive).getFile());
		File chartArchive = File.createTempFile(UUID.randomUUID().toString(), ".tgz");
		chartArchive.deleteOnExit();
		Files.copy(chartSource, chartArchive);
		return chartArchive;
	}
}
