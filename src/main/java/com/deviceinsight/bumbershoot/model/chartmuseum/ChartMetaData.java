package com.deviceinsight.bumbershoot.model.chartmuseum;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChartMetaData {

	private String name;

	private String version;

	private Instant createdAt;

	private List<String> urls;

}
