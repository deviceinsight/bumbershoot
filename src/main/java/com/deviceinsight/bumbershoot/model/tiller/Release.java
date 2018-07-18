package com.deviceinsight.bumbershoot.model.tiller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Release {

	private String name;

	private String namespace;

	private Integer version;

	private ReleaseInfo info;

	private Chart chart;

	private Values config;


}
