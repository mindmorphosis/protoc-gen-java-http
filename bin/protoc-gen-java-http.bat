@echo off
setlocal enabledelayedexpansion

set "classpath="
for %%f in ("%~dp0..\lib\*.jar") do (
    set "classpath=!classpath!;%%f"
)

java -classpath "%classpath%" com.mindmorphosis.protoc.gen.http.PluginMain %*
