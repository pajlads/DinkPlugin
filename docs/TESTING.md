# Building and Tests

This RuneLite plugin uses the [Gradle](https://docs.gradle.org/current/userguide/userguide.html) build system.

See our [build script](../build.gradle.kts) and [current gradle version](../gradle/wrapper/gradle-wrapper.properties).

## Build task

To compile (and test) the plugin, simply run `gradlew build`

At a minimum, Java 8 is required for compilation, but Java 11+ is recommended (if you wish to enable developer mode).

## Shadow Jar task

To create an executable JAR (with dependencies), run `gradlew shadowJar`

Note: to be able to run this jar, the "enable assertions" flag (`-ea`) must be specified.
See our [IDE run config](../.run/Run%20Dink.run.xml) for other common parameters.

## Test task

Dink features a comprehensive test suite using [JUnit5](https://junit.org/junit5/) and [Mockito](https://site.mockito.org/) to test individual notifiers (and their OkHttp integration).

To execute these tests, run `gradlew test`

### Configuration

In order for the test notifications to be sent to an actual webhook server,
one can specify the environmental variable: `TEST_WEBHOOK_URL`.

In addition, one can disable the Discord rich embed formatting of test notifications
by setting the environmental variable `TEST_WEBHOOK_RICH` to `false`.

Currently, it is not possible to enable the webhook retry behavior (or configure timeouts)
for tests without modifying [MockedNotifierTest](../src/test/java/dinkplugin/notifiers/MockedNotifierTest.java).
