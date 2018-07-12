package com.deviceinsight.bumbershoot.config;

import com.deviceinsight.bumbershoot.model.ManagedChart;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import javax.validation.constraints.NotEmpty;

@ConfigurationProperties("bumbershoot")
@Data
@Validated
public class BumbershootConfigurationProperties {

	@NotEmpty
	private List<ManagedChart> managedCharts;

}
