#!/bin/sh
APP_HOME=$(cd "${0%/*}" >/dev/null 2>&1 && pwd -P)
exec "${JAVA_HOME:-/usr}/bin/java" -Xmx64m -Xms64m -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
