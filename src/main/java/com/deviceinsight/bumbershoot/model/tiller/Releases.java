package com.deviceinsight.bumbershoot.model.tiller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Releases {

	private List<Release> releases;

}
