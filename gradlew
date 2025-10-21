#!/bin/sh

DEFAULT_JVM_OPTS='"-Xmx64m"  "-Xms64m"'
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

die ( ) {
    echo
    echo "$*"
    echo
    exit 1
}

warn ( ) {
    echo "$*"
}

OS="`uname`"
case $OS in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
esac

GRADLE_APP_DIR=`pwd -P`

GRADLE_WRAPPER_EXECUTABLE="$GRADLE_APP_DIR/gradle/wrapper/gradle-wrapper.jar"

if [ ! -r "$GRADLE_WRAPPER_EXECUTABLE" ]; then
    die "ERROR: GRADLE_WRAPPER_EXECUTABLE='$GRADLE_WRAPPER_EXECUTABLE' does not exist or is not readable."
fi

CLASSPATH=$GRADLE_WRAPPER_EXECUTABLE

JAVA_EXE="java"
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVA_EXE="$JAVA_HOME/jre/sh/java"
    else
        JAVA_EXE="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVA_EXE" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    warn "JAVA_HOME environment variable is not set"
fi

exec "$JAVA_EXE" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS "\"-Dorg.gradle.appname=$APP_BASE_NAME\"" -classpath "\"$CLASSPATH\"" org.gradle.wrapper.GradleWrapperMain "$@"
 
