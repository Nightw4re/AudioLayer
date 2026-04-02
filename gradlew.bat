@echo off
setlocal
set DIRNAME=%~dp0
set APP_HOME=%DIRNAME%
set DEFAULT_JVM_OPTS=-Xmx64m -Xms64m

if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%APP_HOME%.gradle
if not exist "%GRADLE_USER_HOME%" mkdir "%GRADLE_USER_HOME%"

set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar;%APP_HOME%gradle\wrapper\gradle-wrapper-shared.jar;C:\gradle\lib\*

java %DEFAULT_JVM_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
