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


## News and Noteworthy

* v0.1.1 - 2022-03-21
  * Updated IAL.xsd to the latest version
* v0.1.0 - 2022-03-11
  * Initial release
  