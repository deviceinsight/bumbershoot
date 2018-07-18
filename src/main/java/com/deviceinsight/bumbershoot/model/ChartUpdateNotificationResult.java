package com.deviceinsight.bumbershoot.model;

import java.util.Collection;

import lombok.Data;

@Data
public class ChartUpdateNotificationResult {

	private final Collection<ReleaseUpgradeResult> results;
}
