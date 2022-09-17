@echo off
call gradlew.bat chi-launcher:shadowJar

%GRAALVM_HOME%\bin\java.exe ^
    -Dgraalvm.locatorDisabled=true ^
    -cp chi-launcher\build\libs\chi-launcher-1.0-all.jar ^
    gh.marad.chi.launcher.MainKt %*
