---
layout: docs
title: Installation
permalink: installation/
lang: en
---

## Adding the jars

To install WAW in your `webapp.war`, you must add the `webappwatcher-*.jar` and its dependencies to the libs of your webapp.

### Manually
1. Put `webappwatcher-*.jar` in the `WEB-INF/lib/` directory of your webapp;

1. Also add in `WEB-INF/lib` the jar dependencies from the `lib/` directory of the WAW distribution (be careful not to add jars which would already be present in your webapp);

1. Also add the jars in `optional-jar` (`log4j*` & `slf4j*`) if your application server don't provide them;

### With Maven
Add this dependency to your project :

```xml
	<dependency>
		<groupId>com.iorga</groupId>
		<artifactId>webappwatcher</artifactId>
		<version>1.5.2</version>
		<scope>runtime</scope>
	</dependency>
```

## Integration
1. Be sure that you've got a filter which allows [HttpServletRequest.getUserPrincipal()](http://docs.oracle.com/javaee/6/api/javax/servlet/http/HttpServletRequest.html#getUserPrincipal(\)) to return the current logged in user (used by WAW filter);

1. Add this in your `WEB-INF/web.xml` (just after the filter we talked in the previous point):

```xml
    <filter>
        <filter-name>Log Filter</filter-name>
        <filter-class>com.iorga.webappwatcher.RequestLogFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>Log Filter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>
```

With JBoss 7, you also have to add `com/sun/management` to `jboss-as-7/modules/sun/jdk/main/modules.xml`:

```xml
...
<module xmlns="urn:jboss:module:1.1" name="sun.jdk">
    <resources>
        <!-- currently jboss modules has not way of importing services from
        classes.jar so we duplicate them here -->
        <resource-root path="service-loader-resources"/>
    </resources>
    <dependencies>
        <system export="true">
            <paths>
                <path name="com/sun/management"/>
                <path name="com/sun/script/javascript"/>
...
```
