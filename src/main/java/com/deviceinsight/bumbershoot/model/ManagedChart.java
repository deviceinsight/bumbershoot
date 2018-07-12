package com.deviceinsight.bumbershoot.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ManagedChart {

	@NotBlank
	private String name;

	@NotNull
	private ChartRepository repository;

	@Builder.Default
	private UpgradePolicy defaultUpgradePolicy = UpgradePolicy.MINOR;

	// upgrade policies for subcharts. Key is subchart name
	@Builder.Default
	private Map<String, UpgradePolicy> upgradePolicies = new HashMap<>();

	public enum UpgradePolicy {

		NEVER, MINOR, MAJOR

	}

}
