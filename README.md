# ial-service

DE4A IAL Service

This service will replace the Mock IAL in Iteration 2 only.
This code will not be used by Iteration 1 code.

Work in progress

## Submodules

This project consists of the following submodules:

* `ial-api` - the API level with the technical interfaces. To be used by the Connector
* `ial-webapp` - the web application of the IAL that takes requests, queries the Directory and enriches the data. To be called by the Connector or by Data Evaluators directly.

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

## WebApp REST APIs

### `/api/provision/{canonicalObjectTypeIDs}`

Get a list of all participants, that matches any of the Canonical Object Types (Canonical Evidences and Canonical Events)
in the provided list. Multiple Canonical Object Types can be provided, separated by comma.

Example calls:
* `/provision/urn:de4a-eu:CanonicalEvidenceType::CompanyRegistration:1.0`
    * Search for all EPs that support the "Company Registration" evidence type, independent of the country
* `/provision/urn:de4a-eu:CanonicalEvidenceType::MarriageRegistration:1.0,urn:de4a-eu:CanonicalEvidenceType::BirthCertificate:1.0`
    * Search for all EPs that support the "Marriage Registration" or the "Birth Certificate" evidence type, independent of the country

### `/api/provision/{canonicalObjectTypeIDs}/{atuCode}`

Get a list of all participants, that matches any of the Canonical Object Types (Canonical Evidences and Canonical Events)
in the provided list, filtering it by a target ATU code. Multiple Canonical Object Types can be provided, separated by comma.

Example calls:
* `/provision/urn:de4a-eu:CanonicalEvidenceType::CompanyRegistration:1.0/AT`
    * Search for all EPs that support the "Company Registration" evidence type, limit to the matches in Austria
* `/provision/urn:de4a-eu:CanonicalEvidenceType::CompanyRegistration:1.0/AT130`
    * Search for all EPs that support the "Company Registration" evidence type, limit to the matches in Vienna, Austria (NUTS 3)
* `/provision/urn:de4a-eu:CanonicalEvidenceType::MarriageRegistration:1.0,urn:de4a-eu:CanonicalEvidenceType::BirthCertificate:1.0/SE`
    * Search for all EPs that support the "Marriage Registration" or the "Birth Certificate" evidence type, limit to the matches in Sweden

## `/mor/xx.json`

Get the MOR data for the provided country code `xx`.
Available country codes (case sensitive):
* `en` - English
* `es` - Spanish
* `fr` - French
* `pt` - Portuguese
* `ro` - Romanian
* `sl` - Slovenian


## WebApp Configuration parameters

* **`global.debug`** (boolean) - enable or disable global debug checks in the application. Should only be enabled during development.
* **`global.production`** (boolean) - enable or disable global functionality only meant to be used in production. Should only be enabled on production systems.

* **`ial.directory.url`** (string) - the full URL of the Directory to be queried. E.g. `https://de4a.simplegob.com/directory/`
* **`ial.directory.tls.trustall`** (boolean) (v0.1.4) - pass true to trust all SSL/TLS certificates for the Directory (unsecure setting)

* **`ial.rest.payload-on-error`** (boolean) - true to log payload in case of an error
* **`ial.rest.log-exceptions`** (boolean) - true to print exception stack traces in case of error

* **`ial.webapp.status.enabled`** (boolean) - true to enable the `/status` servlet to show data
* **`ial.webapp.data.path`** (string) - the file system path where runtime data should be stored

## News and Noteworthy

* v0.1.10 - 2022-12-15
    * Fixed an issue with search results if CanonicalEvidenceType and ATU code were provided together
    * Improved internal error resilience
    * Added new internal API `/internal/clear-smpclient-cache` to clear the IAL SMP client lookup cache 
* v0.1.9 - 2022-10-19
    * Made the SMP truststore configurable via the properties `ial.smp.truststore.path` and `ial.smp.truststore.password`
* v0.1.8 - 2022-10-18
    * The IAL filters out all participant identifiers that does not support the process ID "urn:de4a-eu:MessageType::Request" with the idea that only DOs are left
* v0.1.7 - 2022-10-05
    * Updated to Apache Http Client v5.x
    * Added response header `Access-Control-Allow-Origin: *` to all queries
    * Fixed character encoding problem when querying IAL
* v0.1.6 - 2022-08-09
    * Removed name prefix `idk` from `IALMarshaller` method
    * Added the public folder `/mor` to provide the MOR JSON files
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
  