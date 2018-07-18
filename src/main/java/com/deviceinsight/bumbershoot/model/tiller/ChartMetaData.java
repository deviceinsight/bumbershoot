package com.deviceinsight.bumbershoot.model.tiller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChartMetaData {

	private String home;

	private String name;

	private String version;

}
