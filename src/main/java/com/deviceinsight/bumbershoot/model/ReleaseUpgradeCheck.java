package com.deviceinsight.bumbershoot.model;

import com.deviceinsight.bumbershoot.model.tiller.Release;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ReleaseUpgradeCheck {

	private Release release;

	private final boolean required;

	private final int hierarchyLevel;

	@Override
	public String toString() {
		return "ReleaseUpgrade [required=" + required + ", hierarchyLevel=" + hierarchyLevel
				+ ", release=" + release.getName()
				+ "]";
	}

}