
REM %GRAALVM_HOME%\bin\native-image -H:+ReportUnsupportedElementsAtRuntime --macro:truffle --no-fallback -cp native\kotlin-stdlib-1.6.21.jar;chi-launcher\build\libs\chi-launcher-1.0.jar;chi-truffle-language\build\libs\chi-truffle-language-1.0.jar;chi-compiler\build\libs\chi-compiler-1.0.jar -H:Class=gh.marad.chi.launcher.MainKt -H:Name=chi

call gradlew.bat chi-truffle-language:shadowJar
%GRAALVM_HOME%\bin\native-image -H:+ReportUnsupportedElementsAtRuntime --macro:truffle --no-fallback -cp chi-truffle-language\build\libs\chi-truffle-language-1.0-all.jar -H:Class=gh.marad.chi.truffle.Main -H:Name=chi --initialize-at-build-time=gh.marad.chi.truffle.runtime.ChiFunctionGen
