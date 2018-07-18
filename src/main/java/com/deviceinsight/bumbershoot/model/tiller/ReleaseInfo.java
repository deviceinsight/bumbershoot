package com.deviceinsight.bumbershoot.model.tiller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReleaseInfo {

	@JsonProperty("first_deployed")
	private Instant firstDeployed;

	@JsonProperty("last_deployed")
	private Instant lastDeployed;

	private ReleaseStatus status;

	@JsonProperty("Description")
	private String description;

}
