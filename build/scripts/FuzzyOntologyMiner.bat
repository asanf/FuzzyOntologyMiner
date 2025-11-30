@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  FuzzyOntologyMiner startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and FUZZY_ONTOLOGY_MINER_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\FuzzyOntologyMiner.jar;%APP_HOME%\lib\owlapi-distribution-3.4.4.jar;%APP_HOME%\lib\stanford-corenlp-4.5.10.jar;%APP_HOME%\lib\owlapi-contract-3.4.4.jar;%APP_HOME%\lib\AppleJavaExtensions-1.4.jar;%APP_HOME%\lib\jollyday-0.4.9.jar;%APP_HOME%\lib\commons-lang3-3.3.1.jar;%APP_HOME%\lib\jakarta.servlet-api-4.0.4.jar;%APP_HOME%\lib\xom-1.3.9.jar;%APP_HOME%\lib\xalan-2.7.3.jar;%APP_HOME%\lib\serializer-2.7.3.jar;%APP_HOME%\lib\joda-time-2.13.0.jar;%APP_HOME%\lib\ejml-simple-0.39.jar;%APP_HOME%\lib\ejml-dsparse-0.39.jar;%APP_HOME%\lib\ejml-ddense-0.39.jar;%APP_HOME%\lib\ejml-fsparse-0.39.jar;%APP_HOME%\lib\ejml-fdense-0.39.jar;%APP_HOME%\lib\ejml-cdense-0.39.jar;%APP_HOME%\lib\ejml-zdense-0.39.jar;%APP_HOME%\lib\ejml-core-0.39.jar;%APP_HOME%\lib\jakarta.json-1.1.6.jar;%APP_HOME%\lib\slf4j-api-1.7.12.jar;%APP_HOME%\lib\protobuf-java-3.25.5.jar;%APP_HOME%\lib\jaxb-api-2.4.0-b180830.0359.jar;%APP_HOME%\lib\javax.activation-api-1.2.0.jar;%APP_HOME%\lib\istack-commons-runtime-3.0.7.jar;%APP_HOME%\lib\jaxb-impl-2.4.0-b180830.0438.jar;%APP_HOME%\lib\owlapi-apibinding-3.4.4.jar;%APP_HOME%\lib\owlapi-impl-3.4.4.jar;%APP_HOME%\lib\owlapi-parsers-3.4.4.jar;%APP_HOME%\lib\owlapi-tools-3.4.4.jar;%APP_HOME%\lib\owlapi-api-3.4.4.jar;%APP_HOME%\lib\org.osgi.core-1.0.0.jar;%APP_HOME%\lib\xercesImpl-2.12.2.jar;%APP_HOME%\lib\jsr305-3.0.2.jar;%APP_HOME%\lib\xml-apis-1.4.01.jar


@rem Execute FuzzyOntologyMiner
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %FUZZY_ONTOLOGY_MINER_OPTS%  -classpath "%CLASSPATH%" fuzzyOntologyMiner.main.Main %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable FUZZY_ONTOLOGY_MINER_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%FUZZY_ONTOLOGY_MINER_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
