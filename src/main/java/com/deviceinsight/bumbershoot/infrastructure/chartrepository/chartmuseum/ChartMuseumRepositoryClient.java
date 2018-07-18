package com.deviceinsight.bumbershoot.infrastructure.chartrepository.chartmuseum;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.web.util.UriComponentsBuilder;

import com.deviceinsight.bumbershoot.exception.ChartDownloadException;
import com.deviceinsight.bumbershoot.exception.ChartNotFoundException;
import com.deviceinsight.bumbershoot.exception.ChartUploadException;
import com.deviceinsight.bumbershoot.infrastructure.chartrepository.ChartRepositoryClient;
import com.deviceinsight.bumbershoot.model.chartmuseum.ChartMetaData;
import com.github.zafarkhaja.semver.Version;
import com.google.common.collect.Iterables;

import feign.Contract;
import feign.Feign;
import feign.Logger.Level;
import feign.codec.Decoder;
import feign.codec.Encoder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
public class ChartMuseumRepositoryClient implements ChartRepositoryClient {

	private final OkHttpClient httpClient = new OkHttpClient();

	private final Decoder decoder;

	private final Encoder encoder;


	private final Contract contract;

	public ChartMuseumRepositoryClient(Decoder decoder, Encoder encoder, Contract contract) {
		this.decoder = decoder;
		this.encoder = encoder;
		this.contract = contract;
	}

	@Override
	public void downloadChart(URL baseUrl, String name, String version, ChartDownloadConsumer consumer)
			throws ChartDownloadException {
		String fileUri = getFileUri(baseUrl, name, version);

		Request request = new Request.Builder()
				.url(fileUri)
				.build();

		try {
			Response response = httpClient.newCall(request).execute();
			consumer.downloadChart(response.body().byteStream());
		} catch (IOException e) {
			log.error("Failed to download chart from " + fileUri, e);
			throw new ChartDownloadException();
		}
	}

	@Override
	public String uploadChart(URL baseUrl, String name, String version, File chartArchive) throws ChartUploadException {

		try {
			URI uploadUri = UriComponentsBuilder.fromUri(baseUrl.toURI())
					.path("/api/charts")
					.build()
					.toUri();

			RequestBody body = RequestBody.create(null, chartArchive);
			Request request = new Request.Builder()
					.url(uploadUri.toURL())
					.post(body)
					.build();

			Response response = httpClient.newCall(request).execute();
			if (response.isSuccessful()) {

				return getFileUri(baseUrl, name, version);
			} else {
				log.error("Failed to upload chart. Response {}", response);
				throw new ChartUploadException();
			}
		} catch (URISyntaxException | IOException e) {
			log.error("Failed to upload chart", e);
			throw new ChartUploadException(e);
		}
	}
	
	@Override
	public Collection<Version> getVersionsOfChart(URL baseUrl, String name) {
		ChartMuseumMetaDataClient client = buildMetaDataClient(baseUrl);
		return client.listChartMetaData(name).stream()
			.map(ChartMetaData::getVersion)
			.map(Version::valueOf)
			.collect(Collectors.toSet());
	}
	
	@Override
	public String getChartFileUrl(URL baseUrl, String name, String version) throws ChartNotFoundException {
		ChartMuseumMetaDataClient metaDataClient = buildMetaDataClient(baseUrl);
		return Optional.ofNullable(metaDataClient.getChartMetaData(name, version))
			.map(ChartMetaData::getUrls)
			.filter(urls -> ! urls.isEmpty())
			.map(urls -> Iterables.get(urls, 0))
			.orElseThrow(ChartNotFoundException::new);
	}

	private String getFileUri(URL baseUrl, String name, String version) {
		ChartMuseumMetaDataClient metaDataClient = buildMetaDataClient(baseUrl);
		return Iterables.get(metaDataClient.getChartMetaData(name, version).getUrls(), 0);
	}

	private ChartMuseumMetaDataClient buildMetaDataClient(URL baseUrl) {
		return Feign.builder()
				.encoder(encoder)
				.decoder(decoder)
				.contract(contract)
				.logLevel(Level.BASIC)
				.target(ChartMuseumMetaDataClient.class, baseUrl.toString());
	}

}
