# Changelog

## 0.1.0

First version of bumbershoot with following features:

* Notification REST API to inform bumbershoot about chart updates
* Automatic umbrella chart modifications 
  * Version bumb
  * Dependency upgrades
* Automatic umbrella chart upgrade via tiller API through [swift proxy](https://github.com/appscode/swift)
* Chartmuseum client for meta data retrieval, chart up- and download
* Configuration for watched umbrella charts including upgrade policy