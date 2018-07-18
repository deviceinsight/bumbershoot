package com.deviceinsight.bumbershoot.infrastructure.tiller;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.deviceinsight.bumbershoot.model.tiller.ChartUpgrade;
import com.deviceinsight.bumbershoot.model.tiller.ReleaseContent;
import com.deviceinsight.bumbershoot.model.tiller.Releases;

@FeignClient(name = "tiller", path = "/tiller/v2/releases/")
public interface SwiftTillerClient {

	@GetMapping("/json")
	Releases getReleases();

	@GetMapping("/{releaseName}/content/json?format_values_as_json=true")
	ReleaseContent getReleaseContent(@PathVariable String releaseName);

	@PutMapping("/{releaseName}/json")
	void upgradeRelease(@PathVariable String releaseName, @RequestBody ChartUpgrade chartUpgrade);


}
