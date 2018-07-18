# Quickstart

This short guide will show you how to get bumbershoot running. 
For detailed information check the [user guide](user-guide.md) 

## Prerequisites

To install bumbershoot you need:

1. A Kubernetes cluster
2. Tiller 2.9 installed on your kubernetes cluster

> *Note*: Bumbershoot is a young project.
> It was only tested with Kubernetes 1.9.2 and Tiller 2.9.
> Probably it is also compatible to other Kubernetes and Tiller versions. 
> Just try a different [swift](https://github.com/appscode/swift) version: `mvn clean package -Dhelm.swift.version=<version>`

## Installation via helm

Bumbershoot can be installed via helm.
It will automatically include [swift](https://github.com/appscode/swift) as tiller proxy.

### Build chart

Currently bumbershoot is not available in a public chart repository (see [#3](/deviceinsight/bumbershoot/issues/3)). 

Therefore you need to build it via `mvn clean package`.
Afterwards you can find the generated chart under `target/helm/repo/bumbershoot-<version>.tgz`. 

### Create configuration file

Bumbershoot needs to know which umbrella charts to managed. 
This is configured on deployment.

Create a yaml file `values.yaml` with following content:
```yaml
managedCharts: 
  - name: <your-chart-name>
    repository:
      url: <chart museum repository holding the chart>
```

> *Note*: Currently only chart museum is supported as chart repository.

### Install chart

Run `helm install targethelm/repo/bumbershoot-<version>.tgz -f values.yaml`. 

This will install:

* swift proxy with:
  * deployment
  * service
* bumbershoot with:
  * deployment
  * service
  * ingress

When all ports are ready you can access bumbershoot's REST API on your ingress controller via `/api/charts`.

> Note: If you don't have an ingress controller, switch the service type to `NodePort`. 
> Then you can access the API with the `NodeIP` and `NodePort` of the node the pod is running.

### Try the API

Notify bumbershoot about a new chart available: 
```
curl -X POST http(s)://<host>/api/charts/notifications -d '{ "chart": { "name": "<yourChartsName>", "version": "<yourChartsVersion>" }, "repository": { "url" : "<url of your chart museum repository>" }}' -H Content-Type:application/json
```

Bumbershoot will return the upgrade result and also log detailed information.