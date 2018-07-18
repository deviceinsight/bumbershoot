package com.deviceinsight.bumbershoot.infrastructure.chartrepository.chartmuseum;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.deviceinsight.bumbershoot.model.chartmuseum.ChartMetaData;

@FeignClient(name = "chartMuseum")
public interface ChartMuseumMetaDataClient {

	@GetMapping("/api/charts/{name}/{version}")
	ChartMetaData getChartMetaData(@PathVariable String name, @PathVariable String version);

	@GetMapping("/api/charts/{name}")
	List<ChartMetaData> listChartMetaData(@PathVariable String name); 

}
