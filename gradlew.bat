@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT command extensions
if "%EXTRA_VERBOSE%"=="1" set

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%"=="0" goto execute

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%DIRNAME%\gradle\wrapper\gradle-wrapper.jar

@rem Execute Gradle
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%~nx0" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

set ERROR_CODE=%ERRORLEVEL%
if %ERROR_CODE% equ 0 goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if not "%GRADLE_EXIT_CONSOLE%"=="" exit 1
if "%ERROR_CODE%"=="" set ERROR_CODE=1
exit /b %ERROR_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal
exit /b 0
