package main

import (
	log "github.com/sirupsen/logrus"
	"k8s.io/helm/pkg/chartutil"
)

func main() {
	chart, err := chartutil.Load("stable/postgresql")
	if err != nil {
		log.WithError(err).Panic("Failed to download chart")
	}

	log.WithField("dependencies", chart.Dependencies).Info("Downloaded")

}
