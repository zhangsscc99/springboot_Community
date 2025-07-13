# PowerShell脚本执行SQL修复

$mysqlPath = "mysql"
$username = "root"
$password = "wyt!!010611ABC"
$host = "localhost"
$port = "3306"
$database = "jinshuxqm_community"
$sqlFile = "fix-agent-users.sql"

Write-Host "正在执行Agent用户信息修复..."

# 执行SQL文件
$command = "& $mysqlPath -u $username -p'$password' -h $host -P $port $database"

try {
    Get-Content $sqlFile | & $mysqlPath -u $username -p"$password" -h $host -P $port $database
    Write-Host "修复脚本执行完成！"
} catch {
    Write-Host "执行出错: $($_.Exception.Message)"
} 