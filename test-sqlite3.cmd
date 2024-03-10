@echo off
cls
call gradlew compileJava
java com.xilinx.rapidwright.interchange.DeviceResourcesExampleSqlite xc7a50t
