package com.deviceinsight.bumbershoot.interfaces.rest;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.deviceinsight.bumbershoot.facade.UmbrellaChartUpgradeFacade;
import com.deviceinsight.bumbershoot.model.ChartUpdateNotification;
import com.deviceinsight.bumbershoot.model.ChartUpdateNotificationResult;

@RestController
@RequestMapping(path = "/api/charts/notifications", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class ChartUpdateNotificationController {

	private final UmbrellaChartUpgradeFacade facade;

	public ChartUpdateNotificationController(UmbrellaChartUpgradeFacade facade) {
		this.facade = facade;
	}

	@PostMapping
	public ChartUpdateNotificationResult notifyChartUpdate(@Valid @RequestBody ChartUpdateNotification request) {
		return facade.upgradeUmbrellaCharts(request);
	}

}
