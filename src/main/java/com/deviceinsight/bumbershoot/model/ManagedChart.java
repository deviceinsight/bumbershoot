package com.deviceinsight.bumbershoot.model;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

	@Builder.Default
	private Map<String, UpgradePolicy> upgradePolicies = new HashMap<>();

	public enum UpgradePolicy {

		NEVER, MINOR, MAJOR

	}

}
