liquibase-filechangelog [![Build and Test Extension](https://github.com/liquibase/liquibase-filechangelog/actions/workflows/build.yml/badge.svg)](https://github.com/liquibase/liquibase-filechangelog/actions/workflows/build.yml)
================

Liquibase extension to support file based changeSet tracking rather than database-table based.
When enabled, a CSV file is used instead of the DATABASECHANGELOG table.

To use the extension, simply add the liquibase-filechangelog.jar to your classpath.

You can disable the extension logic by setting the system parameter "liquibase.ext.filechangelog.enabled" to "false"

The file to use can be set with the liquibase.ext.filechangelog.file system property.
If not set, it defaults to "databasechangelog.csv" in the current working directory.

Liquibase Requirements: **Requires Liquibase 3.1.0+**

Download the liquibase-filechangelog.jar file from:

* Direct: ["release" section on github](https://github.com/liquibase/liquibase-filechangelog/releases)
* Maven: Group org.liquibase.ext, Artifact liquibase-filechangelog
