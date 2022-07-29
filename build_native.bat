@echo off

REM call gradlew.bat clean jar shadowJar
REM %GRAALVM_HOME%\bin\native-image -H:+ReportUnsupportedElementsAtRuntime --macro:truffle --no-fallback -cp chi-launcher\build\libs\chi-launcher-1.0.jar;chi-truffle-language\build\libs\chi-truffle-language-1.0-all.jar;chi-compiler\build\libs\chi-compiler-1.0.jar gh.marad.chi.launcher.MainKt chi --initialize-at-build-time=gh.marad.chi.truffle.runtime.ChiFunctionGen
REM %GRAALVM_HOME%\bin\native-image -H:+ReportUnsupportedElementsAtRuntime --macro:truffle --no-fallback -cp chi-launcher\build\libs\chi-launcher-1.0.jar;chi-truffle-language\build\libs\chi-truffle-language-1.0.jar;chi-compiler\build\libs\chi-compiler-1.0.jar;native\kotlin-stdlib-1.6.21.jar gh.marad.chi.truffle.Main chi --initialize-at-build-time=gh.marad.chi.truffle.runtime.ChiFunctionGen
REM %GRAALVM_HOME%\bin\native-image -H:+ReportUnsupportedElementsAtRuntime --macro:truffle --no-fallback -cp chi-launcher\build\libs\chi-launcher-1.0.jar;chi-truffle-language\build\libs\chi-truffle-language-1.0.jar;chi-compiler\build\libs\chi-compiler-1.0.jar gh.marad.chi.truffle.Main chi --initialize-at-build-time=gh.marad.chi.truffle.runtime.ChiFunctionGen

call gradlew.bat clean chi-truffle-language:shadowJar chi-launcher:jar
%GRAALVM_HOME%\bin\native-image -H:+ReportUnsupportedElementsAtRuntime --macro:truffle --no-fallback -cp chi-truffle-language\build\libs\chi-truffle-language-1.0-all.jar;chi-launcher\build\libs\chi-launcher-1.0.jar -H:Class=gh.marad.chi.launcher.MainKt -H:Name=chi --initialize-at-build-time=gh.marad.chi.truffle.runtime.ChiFunctionGen
