package com.deviceinsight.bumbershoot.model;


import javax.validation.constraints.NotEmpty;

import com.deviceinsight.bumbershoot.model.tiller.ChartMetaData;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartIdentifier {

	@NotEmpty
	private String name;

	@NotEmpty
	private String version;


	public ChartMetaData toTillerChartMetaData() {
		return ChartMetaData.builder()
			.name(name)
			.version(version)
			.build();
	}

}
