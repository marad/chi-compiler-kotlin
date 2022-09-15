@echo off
REM call gradlew.bat chi-truffle-language:shadowJar chi-launcher:jar
call gradlew.bat chi-launcher:shadowJar

REM %GRAALVM_HOME%\bin\java.exe ^
REM     -Dgraalvm.locatorDisabled=true ^
REM     -cp chi-truffle-language\build\libs\chi-truffle-language-1.0-all.jar;chi-launcher\build\libs\chi-launcher-1.0.jar ^
REM     gh.marad.chi.launcher.MainKt %*

%GRAALVM_HOME%\bin\java.exe ^
    -Dgraalvm.locatorDisabled=true ^
    -cp chi-launcher\build\libs\chi-launcher-1.0-all.jar ^
    gh.marad.chi.launcher.MainKt %*
