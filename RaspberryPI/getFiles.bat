set root=\\PI\root
cd %~dp0


call :process usr\local\bin\homeAutomation\runJar-debug.sh
call :process etc\init.d\homeAutomation

goto :eof
:process
    md %~dp1
    pushd %~dp1
        del /y %~nx1
        copy %root%\%1 .
    popd