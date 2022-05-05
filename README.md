# ial-service

DE4A IAL Service

This service will replace the Mock IAL in Iteration 2 only.
This code will not be used by Iteration 1 code.

Work in progress

## Submodules

This project consists of the following submodules:

* `ial-api` - the API level with the technical interfaces. To be used by the Connector

## Maven Coordinates

The Maven BOM can be used like this (replacing `x.y.z` with the real version number):

```xml
  <dependencyManagement>
    <dependencies>
      ...
      <dependency>
        <groupId>eu.de4a.ial</groupId>
        <artifactId>ial-parent-pom</artifactId>
        <version>x.y.z</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      ...
    </dependencies>
  </dependencyManagement>
```

## Configuration parameters

* **`global.debug`** (boolean) - enable or disable global debug checks in the application. Should only be enabled during development.
* **`global.production`** (boolean) - enable or disable global functionality only meant to be used in production. Should only be enabled on production systems.

* **`ial.directory.url`** (string) - the full URL of the Directory to be queried. E.g. `https://de4a.simplegob.com/directory/`
* **`ial.directory.tls.trustall`** (boolean) (v0.1.4) - pass true to trust all SSL/TLS certificates for the Directory (unsecure setting)

* **`ial.rest.payload-on-error`** (boolean) - true to log payload in case of an error
* **`ial.rest.log-exceptions`** (boolean) - true to print exception stack traces in case of error

* **`ial.webapp.status.enabled`** (boolean) - true to enable the `/status` servlet to show data
* **`ial.webapp.data.path`** (string) - the file system path where runtime data should be stored

## News and Noteworthy

* v0.1.5 - 2022-05-05
    * Fixed an internal error if no search result was found
    * Added the missing `country` parameter when searching the Directory
* v0.1.4 - 2022-05-05
    * Added a new configuration item `ial.directory.tls.trustall`
* v0.1.3 - 2022-04-26
    * First version of the IAL service
* v0.1.2 - 2022-04-13
    * Updated to the latest IAL.xsd matching the Technical Design v1.1
* v0.1.1 - 2022-03-21
    * Updated IAL.xsd to the latest version
* v0.1.0 - 2022-03-11
    * Initial release
  