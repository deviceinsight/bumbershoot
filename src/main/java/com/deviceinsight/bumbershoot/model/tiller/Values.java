package com.deviceinsight.bumbershoot.model.tiller;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Values {

	private String raw;

}
