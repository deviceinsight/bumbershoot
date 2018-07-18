# Bumbershoot

Bumershoot is a kubernetes aware application providing continous delivery for [complex umbrella charts](https://github.com/kubernetes/helm/blob/master/docs/charts_tips_and_tricks.md#complex-charts-with-many-dependencies).

## Motivation

When deploying applications to several [Kubernetes clusters](https://kubernetes.io), there comes a moment when you want to use 
[Helm](https://helm.sh). 
Helm helps you to reduce the amount of manifests, you need to write, by providing a template mechanism.
This can reduce your environment specific infrastructure code to some helm variables. 

A special use case for helm are [umbrella charts](https://github.com/kubernetes/helm/blob/master/docs/charts_tips_and_tricks.md#complex-charts-with-many-dependencies) to deploy a whole stack of services. 
While umbrella charts allow deploying a whole environment with a single command, **continous deployment** can be difficult, since the umbrella chart itself, needs to be updated and released every time.

Here comes **bumbershoot** into play. 
Bumbershoot watches charts installed on a kubernetes cluster and updates these when there is a new version of a dependency available.

## Quick start

For a quick setup sample, check out the [quick start](docs/quickstart.md)

## Documentation

The full documentation can be found in the [user guide](docs/user-guide.md)

## Changelog 

Check out the [changelog](CHANGELOG.md) for the latest releases and changes.

## Roadmap

If you want to know what features are planned for the future or get an idea for contribution, check out the [open feature requests](https://github.com/deviceinsight/bumbershoot/issues?q=is%3Aissue+is%3Aopen+label%3Aenhancement)
