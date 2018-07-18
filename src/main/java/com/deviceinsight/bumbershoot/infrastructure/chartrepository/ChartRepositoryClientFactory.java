package com.deviceinsight.bumbershoot.infrastructure.chartrepository;

import com.deviceinsight.bumbershoot.exception.UnsupportedChartRepositoryType;

import java.util.Map;
import java.util.Optional;

public class ChartRepositoryClientFactory {

	private final Map<String, ChartRepositoryClient> clients;

	public ChartRepositoryClientFactory(Map<String, ChartRepositoryClient> clients) {
		this.clients = clients;
	}

	public ChartRepositoryClient getClientForType(String type) throws UnsupportedChartRepositoryType {
		return Optional.ofNullable(clients.get(type)).orElseThrow(UnsupportedChartRepositoryType::new);
	}

}
