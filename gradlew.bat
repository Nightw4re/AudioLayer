@echo off
setlocal
set DIRNAME=%~dp0
set APP_HOME=%DIRNAME%
set DEFAULT_JVM_OPTS=-Xmx64m -Xms64m

set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar;%APP_HOME%gradle\wrapper\gradle-wrapper-shared.jar;C:\gradle\lib\*

java %DEFAULT_JVM_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
