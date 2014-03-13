---
layout: docs
title: Installation
permalink: installation/
lang: en
---

## Prerequisites

JDK 7 installed in the PATH. (Check `javac -v` and `java -v` to be sure it's 7).

## Checkout the code

```bash
git clone https://github.com/iorga-group/webapp-watcher.git
cd webapp-watcher
```

## Build

```bash
cd waw-analyzer-parent
mvn clean && mvn package
```

This will generate `/waw-analyzer/target/waw-analyzer-<version>.war`.

## Install in an Application Server

Just move the built war to your Application Server wars directory (depends on each Application Server, `$TOMCAT_HOME/webapps` for Tomcat for example).

WAW-Analyzer will be accessible at [http://localhost:8080/waw-analyzer/](http://localhost:8080/waw-analyzer/).

## <a name="create-self-running-java-application"></a>Create self-running Java application
In order to create self-running Java application (that is to say independent from any Application Server), go to `waw-analyzer-parent` directory, run :

```bash
mvn package org.apache.tomcat.maven:tomcat7-maven-plugin:exec-war-only
```

This will generate `/waw-analyzer/target/waw-analyzer-<version>-war-exec.jar`.

You can then start WAW-Analyzer with :

```bash
java -jar waw-analyzer-<version>-war-exec.jar
```

WAW-Analyzer will be accessible at [http://localhost:8080/](http://localhost:8080/).

You can optionnaly pass in [options](http://tomcat.apache.org/maven-plugin-trunk/executable-war-jar.html#Generated_executable_jarwar) to change for example the listening port (instead of default `8080`).
