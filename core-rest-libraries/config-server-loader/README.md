Overview
--------

![logo](./common%20diagram.png)

`config-server-loader` is indented for loading spring properties from a config-server service.  
This module contains the following submodules:
* config-server-loader-common
* config-server-loader-resttemplate
* config-server-loader-webclient

The first module contains common logic for downloading properties.  
Since it's a inner module and does have any restclients so you don't need to take this one. Instead of it you should use one of these:

* [config-server-loader-resttemplate](./config-server-loader-resttemplate/README.md) if we work on resttemplate  
* [config-server-loader-webclient](./config-server-loader-webclient/README.md) if we work on webclient

_Please, note that working on **resttemplate** is a legacy approach and we strongly recommend to use **webclient** instead._
 
**Attention!** The principle of the configloader work has changed since Springboot version 2.2.4. 

If property `spring.config.import` is included, you must add value `configserver:` or `optional:configserver:<config-server-url>`
to it or application won't start (you'll get exception with message).

Example for `<config-server-url>` is configserver:http://config-server:8080.

If configloader can't connect to configserver, module will make several retries. User can configure number of retries and 
their prolongation with properties.

* `core.spring.cloud.config.retry.max-attempts` allows setting number of retry attempts (default is 12)
* `core.spring.cloud.config.retry.max-interval-ms` allows setting prolongation of each retry attempt (default is 5 sec)