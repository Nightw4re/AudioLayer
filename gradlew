#!/usr/bin/env sh
DIRNAME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
APP_HOME="$DIRNAME/"
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
