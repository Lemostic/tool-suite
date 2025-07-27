@echo off
echo 正在清理数据库相关文件...

REM 尝试停止可能运行的H2服务器进程
echo 正在检查H2服务器进程...
taskkill /F /IM java.exe /FI "WINDOWTITLE eq H2*" 2>nul
taskkill /F /IM javaw.exe /FI "WINDOWTITLE eq H2*" 2>nul

REM 删除H2数据库锁文件
if exist "data\workbench.lock.db" (
    del "data\workbench.lock.db"
    echo 已删除: data\workbench.lock.db
)

if exist "data\workbench.trace.db" (
    del "data\workbench.trace.db"
    echo 已删除: data\workbench.trace.db
)

if exist "data\workbench.temp.db" (
    del "data\workbench.temp.db"
    echo 已删除: data\workbench.temp.db
)

REM 删除可能的临时文件
if exist "data\*.tmp" (
    del "data\*.tmp"
    echo 已删除临时文件
)

echo.
echo 数据库清理完成！
echo 应用现在将使用H2服务器模式，可以支持多个连接。
echo 现在可以启动应用了。
echo.
pause
