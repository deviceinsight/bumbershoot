package com.deviceinsight.bumbershoot.config.infrastructure;

import com.deviceinsight.bumbershoot.infrastructure.chartrepository.ChartRepositoryClient;
import com.deviceinsight.bumbershoot.infrastructure.chartrepository.ChartRepositoryClientFactory;
import com.deviceinsight.bumbershoot.infrastructure.chartrepository.chartmuseum.ChartMuseumRepositoryClient;
import com.deviceinsight.bumbershoot.infrastructure.tiller.SwiftTillerClient;

import feign.Contract;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.cloud.openfeign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@EnableFeignClients(clients = SwiftTillerClient.class)
public class InfrastructureConfiguration {

	@Bean
	public ChartRepositoryClientFactory chartRepositoryClientFactory(Map<String, ChartRepositoryClient> clients) {
		return new ChartRepositoryClientFactory(clients);
	}

	@Bean(name = "chartmuseum")
	public ChartMuseumRepositoryClient chartMuseumRepositoryClient(Encoder encoder, Decoder decoder, Contract contract) {

		return new ChartMuseumRepositoryClient(decoder, encoder, contract);
	}

	@Bean
	public Encoder encoder(ObjectFactory<HttpMessageConverters> messageConverters) {
		return new SpringEncoder(messageConverters);
	}

	@Bean
	public Decoder decoder(ObjectFactory<HttpMessageConverters> messageConverters) {
		return new SpringDecoder(messageConverters);
	}

	@Bean
	public Contract contract() {
		return new SpringMvcContract();
	}

}
