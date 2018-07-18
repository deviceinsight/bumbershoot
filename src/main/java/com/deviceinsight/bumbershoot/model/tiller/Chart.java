package com.deviceinsight.bumbershoot.model.tiller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Chart {

	@JsonProperty("metadata")
	private ChartMetaData metaData;

	/*
	 * FIXME: NPE for no args constructor. Enable again when lombok 1.8.1 is available
	 * @Builder.Default
	 */
	private List<Template> templates = new ArrayList<>();

	/*
	 * FIXME: NPE for no args constructor. Enable again when lombok 1.8.1 is available
	 * @Builder.Default
	 */
	private List<Chart> dependencies = new ArrayList<>();

	@JsonProperty("values")
	private Values defaultValues;
	
	public List<Template> getTemplates() {
		return templates != null? templates : Collections.emptyList();
	}
	
	public List<Chart> getDependencies() {
		return dependencies != null? dependencies : Collections.emptyList();
	}

}
