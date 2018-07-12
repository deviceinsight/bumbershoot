package com.deviceinsight.bumbershoot.model;

import java.net.URL;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChartRepository {

	@NotNull
	private URL url;

	private String type = "chartmuseum";

	private BasicAuthentication basicAuth;

	@Data
	public static class BasicAuthentication {

		private String username;

		private String password;

	}

}