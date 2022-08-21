@echo off
call gradlew.bat chi-truffle-language:shadowJar chi-launcher:jar

%GRAALVM_HOME%\bin\java.exe ^
    -Dgraalvm.locatorDisabled=true ^
    -cp chi-truffle-language\build\libs\chi-truffle-language-1.0-all.jar;chi-launcher\build\libs\chi-launcher-1.0.jar ^
    gh.marad.chi.launcher.MainKt %*
