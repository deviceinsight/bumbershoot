package com.deviceinsight.bumbershoot.model;

import com.deviceinsight.bumbershoot.model.tiller.ChartMetaData;
import com.deviceinsight.bumbershoot.model.tiller.Release;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReleaseUpgradeResult {

	private String releaseName;

	private String releaseNamespace;

	private Status status;

	private String details;

	private Integer newVersion;
	
	private ChartMetaData chart;

	public enum Status {
		UNCHANGED, UPGRADE_SUCCEEDED, UPGRADE_FAILED, OTHER
	}

	public static ReleaseUpgradeResult fromReleaseStatus(Release release) {
		ReleaseUpgradeResultBuilder result = ReleaseUpgradeResult.builder()
				.releaseName(release.getName())
				.releaseNamespace(release.getNamespace());
		
		switch (release.getInfo().getStatus().getCode()) {
			case "DEPLOYED":
				result.status(Status.UPGRADE_SUCCEEDED);
				break;
			case "FAILED":
				result.status(Status.UPGRADE_FAILED);
				break;
			default:
				result.status(Status.OTHER);
				break;
		}

		result.details(release.getInfo().getDescription());
		result.newVersion(release.getVersion());
		result.chart(release.getChart().getMetaData());

		return result.build();
	}
}
