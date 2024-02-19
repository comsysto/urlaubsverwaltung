# Urlaubsverwaltung [![Build](https://github.com/urlaubsverwaltung/urlaubsverwaltung/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/urlaubsverwaltung/urlaubsverwaltung/actions/workflows/build.yml) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=urlaubsverwaltung_urlaubsverwaltung&metric=coverage)](https://sonarcloud.io/summary/new_code?id=urlaubsverwaltung_urlaubsverwaltung) [![Docker Pulls](https://badgen.net/docker/pulls/synyx/urlaubsverwaltung?icon=docker&label=pulls)](https://hub.docker.com/r/urlaubsverwaltung/urlaubsverwaltung/) [![Crowdin](https://badges.crowdin.net/urlaubsverwaltung/localized.svg)](https://crowdin.com/project/urlaubsverwaltung)

The vacation management is a web application for electronically managing *absences*.

Employees can submit **vacation requests**, which can then be approved, denied, or cancelled by the respective authorized persons. Every employee can manage their **overtime** to always keep track, and should someone be unable to work, the **sick leave** can be recorded directly.


If you would like to see more information and images about this project, please visit our [Landing Page].

**Version 4.x**
This Readme is for version 5 of the vacation management system. If you are looking for information on version 4,
you can find it [in the v4.x branch](https://github.com/urlaubsverwaltung/urlaubsverwaltung/tree/v4.x).

* [Demo System](#demo-system)
* [FAQ](#faq)
* [Permissions](#permissions)
* [Operation](#operation)
  * [Configuration](#configuration)
* [Demo Data Mode](#demo-data-mode)
* [Development](#development)


## Demo System

Would you like to try out the vacation management **without** a lengthy **registration** process?  
Then jump directly into the [Demo System] via our [Landing Page].

## FAQ

For questions that arise while using the vacation management, there is a [Help section].  
If this FAQ does not answer your questions, feel free to
[create a new Q&A](https://github.com/urlaubsverwaltung/urlaubsverwaltung/discussions/new?category=q-a).


## 🎉 Version 5.x

Version 5.0.0 of the vacation management is available!

We have made major adjustments to the database and security providers, as well as set the course for further development of the vacation management system. Therefore, there might not only be good news for everyone.

* No support for MariaDB and MySQL. We are completely switching to [PostgreSQL]. A migration path is already available in the [Migration-Guide-v5].
* We have removed the security providers LDAP and Active Directory and are now offering even stronger support for OIDC.

All information on migrating from 4.72.1 to 5.0.0 can be found in the [Migration-Guide-v5].


## Permissions

In the vacation management system, there are currently the following types of permissions:

* **inactive**: no longer has access to the vacation management system (user data remains for archival purposes)
* **User**: can request vacation for themselves
* **Department Head**: can view, approve, and deny vacation requests for users in their departments
* **Release Responsible**: is responsible for the final approval in the two-stage approval process of applications
* **Boss**: can view, approve, and deny vacation requests of all users
* **Office**: can make application settings, manage employees, apply/cancel vacation for employees, and maintain sick leave records

An active person can hold one or more roles.

  
---
  
## Operation

### Prerequisites

* [JDK 21](https://adoptium.net)
* [PostgreSQL Database (v15.3)](https://www.postgresql.org/)
* [Security Provider](#configure-security-provider)

### Download

The application is available as a
* [Java Archive (.jar)](https://github.com/urlaubsverwaltung/urlaubsverwaltung/releases/latest)
* [Docker Image](https://hub.docker.com/r/urlaubsverwaltung/urlaubsverwaltung)

#### Installation .jar Variant

* [Configure Database](#configure-database)
* [Configure Security Provider](#configure-security-provider)
* Create a directory for the vacation management (e.g., `/opt/urlaubsverwaltung`). Copy the .jar file there.
* Create a configuration file named `application.properties` in that directory, which contains the configuration for
the vacation management and overrides the default values.
 The complete configuration options are documented under [Configuration](#configuration).
  
After [configuration](#configuration), the vacation management can be started.

```bash
java -jar urlaubsverwaltung.jar
``` 

If there are problems starting the application, it is helpful to configure the [application's logging](#configure-logging)
to get more information about the error state.

#### Docker Variant

All information about operating with our Docker image can be found in the [.example](.examples) folder.

### Configuration

The application has a [configuration file](https://github.com/urlaubsverwaltung/urlaubsverwaltung/blob/main/src/main/resources/application.properties) in the `src/main/resources` directory.
This includes certain basic settings and default values. However, these alone are not sufficient for putting the
application into production. Specific configurations such as the [database settings](#configure-database)
and [Security Provider](#configure-security-provider) must be stored in their own configuration file.

The possibilities Spring Boot offers for using your own configuration file can be read in the
['External Config' Reference](http://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html#boot-features-external-config-application-property-files).

Below are all the specific configuration options of the vacation management system with their default values.


```yaml
uv:

  mail:
    from: ''
    fromDisplayName: Urlaubsverwaltung
    replyTo: ''
    replyToDisplayName: Urlaubsverwaltung
    application-url: ''

  development:
    demodata:
      create: 'false'
      additional-active-user: '0'
      additional-inactive-user: '0'

  calendar:
    organizer: ''
    refresh-interval: P1D

  security:
    oidc:
      claim-mappers:
        group-claim:
          enabled: 'false'
          claim-name: groups
        resource-access-claim:
          enabled: 'false'
          resource-app: urlaubsverwaltung
        role-prefix: urlaubsverwaltung_
      post-logout-redirect-uri: '{baseUrl}'

  application:
    upcoming-holiday-replacement-notification:
      cron: 0 0 7 * * *
    reminder-notification:
      cron: 0 0 7 * * *
    upcoming-notification:
      cron: 0 0 7 * * *

  account:
    update:
      cron: 0 0 5 1 1 *

  sick-note:
    end-of-pay-notification:
      cron: 0 0 6 * * *

```

#### Configure Security Provider

See the [Spring Boot OAuth2](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html#oauth2login-boot-property-mappings) configuration and for configuring the [Resource Server via JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html), for example.

Furthermore, the behavior of the vacation management can be influenced by `uv.security.oidc`.

It is expected that the OIDC Provider includes the following attributes in the Access Token: `given_name`, `family_name`, `email`. Therefore, the client registration must be configured with the scopes `openid`, `profile`, and `email`.
    
The first user who successfully logs into the vacation management will be created with the `Office` role. This allows for user and rights management and maintaining the settings within the application.

#### Configure Database

The application uses a PostgreSQL database management system for data storage. 
Create a database and a user with access rights to this database in your PostgreSQL database management system and configure these

```yaml
spring:
  datasource:
    url: jdbc:postgresql://$HOST:$PORT/$NAME_DER_DATENBANK
    username: $BENUTZER
    password: $PASSWORT
```
When you start the vacation management for the first time, all database tables are created automatically.

#### Configure E-mail Server

To configure the e-mail server, the following configurations must be made.

```yaml
uv:
  mail:
    from: absender@example.org
    fromDisplayName: Urlaubsverwaltung
    replyTo: no-reply@example.org
    replyToDisplayName: Urlaubsverwaltung
    application-url: https://example.org
spring:
  mail:
    host: $HOST
    port: $PORT
    username: $USERNAME
    password: $PASSWORT
```

All other `spring.mail.*` configurations can be viewed in the [Spring Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#io.email).

#### Configure User Synchronization

Persons are no longer automatically synchronized into the vacation management system,
but are now created only when the respective person logs into the vacation management system.

#### Configure Logging

If there are problems starting the application, a detailed debug output can be configured in the configuration file by setting the `logging.level.*` for each package,


```yaml
logging:
  level:
    org.springframework.security: TRACE
    org.synyx.urlaubsverwaltung: TRACE
```

as well as a log file.

```yaml
logging.file.name: logs/urlaubsverwaltung.log
```

as well as configuring a log file to be written.

#### Info-Banner

An info banner can be configured, for example, to announce maintenance work.
The banner will then be visible at the very top.

```properties
uv.info-banner.enabled=true
uv.info-banner.text.de=Maintenance starting Friday at 2:00 PM. There may be disruptions.
```

| Property                        | Type    | Description                                          |
|---------------------------------|---------|------------------------------------------------------|
| uv.info-banner.enabled          | Boolean | (default) `false`, `true` to activate the banner     |
| uv.info-banner.text.de          | String  | Text of the info banner for the German locale.       |

#### Launchpad

A launchpad can be configured, which allows jumping to other applications.

```yaml
launchpad:
  name-default-locale: de
  apps[1]:
    icon: ''
    name:
      de: Anwendung 2
      en: App 2
    url: https://example-2.org
  apps[0]:
    icon: ''
    name:
      en: App 1
      de: Anwendung 1
    url: https://example.org
```

| Property                        | Type     | Description                                                                                                                                                |
|---------------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| launchpad.name-default-locale   | Locale   | Default name of the application if no translation is found for a locale.                                                                                   |
| launchpad.apps[x].url           | String   | URL of the application.                                                                                                                                    |
| launchpad.apps[x].name.[locale] | String   | Name of the application for a locale.                                                                                                                      |
| launchpad.apps[x].icon          | String   | URL of an image or a base64 encoded image. Will be written into the `<img src="" />` attribute.<br/>The image should ideally be square.                    |

The launchpad has its own translations. Spring must be configured accordingly so that the messages.properties is found:

```yaml
spring.messages.basename: messages,launchpad-core
```

* **(required)** `messages` default application message properties
* **(required)** `launchpad-core` launchpad message properties

### Application as a Service

Since the application is based on Spring Boot, it can be very comfortably installed as a service. The exact procedure
can be found in the corresponding chapters of the Spring Boot documentation:


* [Linux Service](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html#deployment-service)
* [Windows Service](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html#deployment-windows)


---
  
## Demo Data Mode

### Starting the Application in Demo Data Mode

To be able to try out the application locally as quickly as possible, it is advisable
to start the database via [Docker Compose](https://docs.docker.com/compose/overview/):


```bash
docker-compose up
```

and to start the application with the `demodata` profile:


```bash
java -jar -Dspring.profiles.active=demodata urlaubsverwaltung.jar
```

In this way, the application is started with a PostgreSQL database management system and demo data is generated.

The demo data includes the following **users**, a password is not required:

| Username                                      | Password | Role                            |
|-----------------------------------------------|----------|----------------------------------|
| user@urlaubsverwaltung.cloud                  | secret   | User                             |
| departmentHead@urlaubsverwaltung.cloud        | secret   | User & Department Head           |
| secondStageAuthority@urlaubsverwaltung.cloud  | secret   | User & Release Responsible       |
| boss@urlaubsverwaltung.cloud                  | secret   | User & Boss                      |
| office@urlaubsverwaltung.cloud                | secret   | User & Office                    |
| admin@urlaubsverwaltung.cloud                 | secret   | User & Admin                     |


If you do not want demo data to be generated when the application starts, the configuration must be


`uv.development.demodata.create`

in the [application-demodata.properties](https://github.com/urlaubsverwaltung/urlaubsverwaltung/blob/main/src/main/resources/application-demodata.properties)
set to `false`.

### Accessing the Application

The following systems are accessible via `localhost`


| Service                                    | Port |
|--------------------------------------------|------|
| [Urlaubsverwaltung](http://localhost:8080) | 8080 |
| [Mailhog](http://localhost:8025)           | 8025 |
| Mailhog SMTP                               | 1025 |
  
---
  
## Development

If you would like to **support** us in the **development** of the vacation management system,
please take a look at the [Contributing to Urlaubsverwaltung](./CONTRIBUTING.md) reference and the following
sections. If you have any questions, feel free to [create a new Q&A](https://github.com/urlaubsverwaltung/urlaubsverwaltung/discussions/new?category=q-a).

### Prerequisites

* [JDK 21](https://adoptium.net)
* [Docker 20.10.+](https://docs.docker.com/get-docker/)
* [Docker Compose](https://docs.docker.com/compose/install/)


### Repository clonen

Without GitHub Account

```bash
https://github.com/urlaubsverwaltung/urlaubsverwaltung.git
```

With GitHub Account

```bash
git clone git@github.com:urlaubsverwaltung/urlaubsverwaltung.git
```

### git hooks (optional)

To automate various tasks, the project offers [git hooks](https://git-scm.com/book/uz/v2/Customizing-Git-Git-Hooks).
You can install them with the following command:


```bash
git config core.hooksPath '.githooks'
```

The Git hooks can be found in the [.githooks](./.githooks/) directory.


### Starting the Application

The vacation management system is a [Spring Boot](http://projects.spring.io/spring-boot/) application and can be started with the Maven
plugin. All dependencies, such as the database or the mail server, are started automatically.
It is advisable to start the application with the `demodata` profile to generate
test data:


```bash
./mvnw clean spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=demodata"
```

or for Windows users via:

```bash
./mvnw.cmd clean spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=demodata"
```

### Using the Application

The application can then be accessed in the browser via [http://localhost:8080/](http://localhost:8080/).

With the `demodata` profile, a PostgreSQL database is used and demo data is created,
i.e., users, vacation requests, and sick notes. Therefore, you can now log in to the web interface with various
[demo data users](#demo-data-users).


### Frontend Development

The 'User Experience' of some pages is further enhanced at runtime with JavaScript.

Assets can be found in `<root>/src/main/javascript`

* `bundles` are to be integrated into HTML pages
* `components` are individual components for reuse, such as the _datepicker_
* `js` includes page-specific things
* `lib` are third-party libraries

The frontend build is integrated into Maven. However, the assets can also be built on the command line in isolation.

* `npm run build`
  * builds optimized, minified assets
  * Note: the filename contains a hash that uniquely matches the content of the asset
* `npm run build:dev`
  * builds unminified assets
* `npm run build:watch`
  * automatically builds new assets after editing JavaScript / CSS files


#### Long Term Caching of Assets

When you start the Maven build or build the assets with the NPM task `npm run build`, a JSON file `assets-manifest.json` is created.
The manifest describes a mapping of the bundles to the generated filename including the hash. This mapped filename must
be integrated into the HTML pages. To avoid having to do this manually with every change, the filename can be automated at compile time using the
`AssetsHashResolverTag.java` tag library.

```html
<script defer asset:src="npm.jquery.js"></script>
```

During further development, it makes sense to disable caching. If the `demodata` profile is used,
nothing else needs to be done. If you are not using that profile, you can disable caching with the following application properties:


```yaml
spring:
  web:
    resources:
      chain:
        cache: 'false'
        strategy:
          content:
            enabled: 'false'
        cache:
          cachecontrol:
            max-age: '0'
```

#### Icons

We use the amazing Lucide icon set. Thank you! ♥️


- https://lucide.dev
- https://github.com/lucide-icons/lucide

### API

The vacation management system has an API accessible at [http://localhost:8080/api](http://localhost:8080/api).

### UI Tests with Playwright

The [test ui](src/test/java/org/synyx/urlaubsverwaltung/ui) package contains UI tests. These tests cover some end-to-end application cases such as enabling/disabling something in the settings and its effects.

[Playwright-Java](https://playwright.dev/java/) is used as the test runner and assertion library.
Details can be found in [Playwright for Java Getting Started](https://playwright.dev/java/docs/intro).


#### Headless Browser

The tests run by default without a visible browser (headless).
This can be configured accordingly in the [PageParameterResolver](src/test/java/org/synyx/urlaubsverwaltung/ui/extension/PageParameterResolver.java)
with `BrowserType.LaunchOptions`.


```java
final Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
```

#### Debugging

Playwright provides its own inspector. See [Playwright Debugging Tests](https://playwright.dev/java/docs/debug)
for more information.

To use it, the environment variable `PWDEBUG=1` must be set when starting the tests.

### Release

### GitHub action

Go to the GitHub action with the name [release trigger][github-action-release-trigger].
* Click on "Run workflow"
* Add the "Milestone ID" (see in the uri of a milestone)
* Add "Release version"
* Add "Next version"
* Run the workflow


[Landingpage]: https://urlaubsverwaltung.cloud "Landingpage"
[Demo-System]: https://urlaubsverwaltung.cloud/demo "Demo-System"
[Hilfe]: https://urlaubsverwaltung.cloud/hilfe/ "Hilfe"
[Migration-Guide-v5]: https://github.com/urlaubsverwaltung/urlaubsverwaltung/wiki/Urlaubsverwaltung-5.0-Migration-Guide "Migration Guide v5"
[github-action-release-trigger]: https://github.com/urlaubsverwaltung/urlaubsverwaltung/actions/workflows/release-trigger.yml "Release Trigger"
[PostgreSQL]: https://www.postgresql.org/ "PostgreSQL"

