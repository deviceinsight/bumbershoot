package com.deviceinsight.bumbershoot.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import com.deviceinsight.bumbershoot.exception.ChartInvalidException;
import com.deviceinsight.bumbershoot.model.tiller.ChartMetaData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.zafarkhaja.semver.Version;

public class ChartArchiveModifier {

	private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

	public String setChartVersion(ChartMetaData metaData, File umbrellaArchive, Version newVersion) throws ChartInvalidException {
		FileSelector chartYamlSelector = metadata -> String.format("%s/Chart.yaml", metaData.getName());
		FileModifier<JsonNode> chartYamlModifier = (is, os) -> {
			JsonNode chartYamlNode = modifyChartDescriptorVersion(is, newVersion);
			yamlMapper.writeValue(os, chartYamlNode);
			return chartYamlNode;
		};

		return modifyChartArchive(metaData, umbrellaArchive, chartYamlSelector, chartYamlModifier)
				.get("version").textValue();

	}

	public void updateChartDependency(ChartMetaData umbrellaMetaData, ChartMetaData dependencyMetaData,
			File umbrellaArchive, File newChartArchive)
			throws ChartInvalidException {

		modifyRequirementsYaml(umbrellaMetaData, dependencyMetaData, umbrellaArchive);
		exchangeSubchartTar(umbrellaMetaData, dependencyMetaData, umbrellaArchive, newChartArchive);
	}

	private <R> R modifyChartArchive(ChartMetaData metaData, File umbrellaArchive,
			FileSelector selector, FileModifier<R> modifier) throws ChartInvalidException {

		R result = null;

		File tempFolder = new File("/tmp/" + UUID.randomUUID().toString());
		tempFolder.mkdirs();
		File tempArchive = new File(tempFolder, UUID.randomUUID().toString() + ".tgz");

		try (TarArchiveInputStream is =
				new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(umbrellaArchive)));
				TarArchiveOutputStream os =
						new TarArchiveOutputStream(new GZIPOutputStream(new FileOutputStream(tempArchive)))) {

			os.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

			TarArchiveEntry entry = null;
			while ((entry = is.getNextTarEntry()) != null) {
				String entryName = entry.getName();
				File copy = new File(tempFolder, entryName);
				if (entry.isDirectory()) {
					if (!copy.isDirectory() && !copy.mkdirs()) {
						throw new IOException("failed to create directory " + copy);
					}
				} else {
					File parent = copy.getParentFile();
					if (!parent.isDirectory() && !parent.mkdirs()) {
						throw new IOException("failed to create directory " + parent);
					}
				}

				try (FileOutputStream fos = new FileOutputStream(copy)) {
					if (selector.matchFileName(metaData, entryName)) {
						entryName = selector.getExpectedFileName(metaData);
						if (entryName == null) {
							continue;
						}

						result = modifier.modifyFile(is, fos);
					} else {
						IOUtils.copy(is, fos);
					}
				}

				ArchiveEntry newEntry = os.createArchiveEntry(copy, entryName);
				os.putArchiveEntry(newEntry);
				try (FileInputStream fos = new FileInputStream(copy)) {
					IOUtils.copy(fos, os);
				}

				os.closeArchiveEntry();
			}

			os.finish();

		} catch (IOException e) {
			throw new ChartInvalidException(e);
		}

		try (FileOutputStream fos = new FileOutputStream(umbrellaArchive)) {
			Files.copy(tempArchive.toPath(), fos);
		} catch (IOException e) {
			throw new ChartInvalidException("Failed to copy from temporary output to result");
		}

		return result;
	}

	private JsonNode modifyChartDescriptorVersion(InputStream is, Version newVersion)
			throws IOException {

		JsonNode chartYaml = yamlMapper.readTree(IOUtils.toByteArray(is));
		((ObjectNode) chartYaml).put("version", newVersion.toString());
		return chartYaml;
	}

	private void modifyRequirementsYaml(ChartMetaData umbrellaMetaData, ChartMetaData dependencyMetaData,
			File umbrellaArchive)
			throws ChartInvalidException {

		FileSelector requirementsYamlSelector =
				metadata -> String.format("%s/requirements.yaml", umbrellaMetaData.getName());
		FileModifier<JsonNode> requirementsYamlModifier = (is, os) -> {
			JsonNode descriptor =
					modifyRequirementsDescriptor(is, dependencyMetaData.getName(), dependencyMetaData.getVersion());
			yamlMapper.writeValue(os, descriptor);
			return descriptor;
		};

		modifyChartArchive(umbrellaMetaData, umbrellaArchive, requirementsYamlSelector, requirementsYamlModifier);
	}

	private void exchangeSubchartTar(ChartMetaData umbrellaMetaData, ChartMetaData dependencyMetaData,
			File umbrellaArchive, File newChartArchive) throws ChartInvalidException {

		FileSelector subtarSelector = new FileSelector() {

			@Override
			public boolean matchFileName(ChartMetaData metaData, String filename) {
				return filename
						.equalsIgnoreCase(String.format("%s/charts/%s/Chart.yaml", metaData.getName(),
								dependencyMetaData.getName()));
			}

			@Override
			public String getExpectedFileName(ChartMetaData metadData) {
				return String.format("%s/charts/%s-%s.tgz", metadData.getName(), dependencyMetaData.getName(),
						dependencyMetaData.getVersion());
			}
		};
		FileModifier<File> tarExchanger = (is, os) -> {
			Files.copy(newChartArchive.toPath(), os);
			return newChartArchive;
		};

		FileSelector oldChartFileFilter = new FileSelector() {

			@Override
			public boolean matchFileName(ChartMetaData metaData, String filename) {
				return !filename.endsWith("tgz")
						&& filename.startsWith(
								String.format("%s/charts/%s", metaData.getName(), dependencyMetaData.getName()));
			}

			@Override
			public String getExpectedFileName(ChartMetaData metadData) {
				return null;
			}
		};

		modifyChartArchive(umbrellaMetaData, umbrellaArchive, subtarSelector, tarExchanger);
		modifyChartArchive(umbrellaMetaData, umbrellaArchive, oldChartFileFilter, (is, os) -> null);

	}

	private JsonNode modifyRequirementsDescriptor(InputStream is, String requirement, String newVersion)
			throws IOException {
		JsonNode chartYaml = yamlMapper.readTree(IOUtils.toByteArray(is));
		JsonNode dependencies = chartYaml.get("dependencies");
		if (dependencies.isArray()) {
			for (JsonNode dependency : dependencies) {
				if (dependency.get("name").asText().equals(requirement)) {
					((ObjectNode) dependency).put("version", newVersion);
					return chartYaml;
				}
			}
		}

		throw new IOException(
				String.format("Could not find requirement %s in requirements descriptor", requirement));
	}

	@FunctionalInterface
	private static interface FileModifier<R> {

		R modifyFile(InputStream inputFileStream, OutputStream outputStream) throws IOException;
	}

	@FunctionalInterface
	private static interface FileSelector {

		String getExpectedFileName(ChartMetaData metaData);

		default boolean matchFileName(ChartMetaData metaData, String filename) {
			return filename.equalsIgnoreCase(getExpectedFileName(metaData));
		}
	}
}
