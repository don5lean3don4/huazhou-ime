# 构建无需 Gradle：aapt2 -> javac -> d8 -> zipalign -> apksigner
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSCommandPath
$workspace = Split-Path -Parent $root
$tools = Join-Path $workspace 'tools'
$jdk = Join-Path $tools 'x_jdk17\jdk-17.0.20.1+1'
$bt = Join-Path $tools 'x_build-tools_r34-windows\android-14'
$platform = Join-Path $tools 'x_platform-35\android-35'
$androidJar = Join-Path $platform 'android.jar'

$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$bt;$env:PATH"

function Invoke-Native {
    param([string]$Exe, [string[]]$Argv)
    $ErrorActionPreference = 'Continue'
    $out = & $Exe @Argv 2>&1
    if ($LASTEXITCODE -ne 0) {
        $out | ForEach-Object { Write-Host $_ }
        throw "command failed ($LASTEXITCODE): $Exe"
    }
    return $out
}

$build = Join-Path $root 'build'
if (Test-Path $build) { Remove-Item -Recurse -Force $build }
New-Item -ItemType Directory -Force -Path $build, "$build\gen", "$build\classes", "$build\dex" | Out-Null

Write-Host '[1/6] aapt2 compile'
Invoke-Native "$bt\aapt2.exe" @('compile', '--dir', "$root\res", '-o', "$build\res.zip") | Out-Null

Write-Host '[2/6] aapt2 link'
Invoke-Native "$bt\aapt2.exe" @(
    'link', '-o', "$build\base.apk", '-I', $androidJar,
    '--manifest', "$root\AndroidManifest.xml", '-R', "$build\res.zip",
    '-A', "$root\assets", '--java', "$build\gen",
    '--min-sdk-version', '21', '--target-sdk-version', '34',
    '--version-code', '7', '--version-name', '5.1', '--auto-add-overlay'
) | Out-Null

Write-Host '[3/6] javac'
$srcs = @(Get-ChildItem -Recurse -Path "$root\src", "$build\gen" -Include *.java | ForEach-Object { $_.FullName })
Invoke-Native "$jdk\bin\javac.exe" (@('-encoding', 'UTF-8', '--release', '8', '-classpath', $androidJar, '-d', "$build\classes") + $srcs) | Out-Null

Write-Host '[4/6] d8'
$classes = @(Get-ChildItem -Recurse -Path "$build\classes" -Include *.class | ForEach-Object { $_.FullName })
Invoke-Native "$bt\d8.bat" (@('--release', '--min-api', '21', '--lib', $androidJar, '--output', "$build\dex") + $classes) | Out-Null

Write-Host '[5/6] package + zipalign'
$py = 'C:\Users\ASUS\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe'
Invoke-Native $py @("$root\tools\add_dex.py", "$build\base.apk", "$build\dex\classes.dex", "$build\unsigned.apk") | Out-Null
Invoke-Native "$bt\zipalign.exe" @('-p', '-f', '4', "$build\unsigned.apk", "$build\aligned.apk") | Out-Null

Write-Host '[6/6] sign'
$ks = Join-Path $root 'debug.keystore'
if (-not (Test-Path $ks)) {
    Invoke-Native "$jdk\bin\keytool.exe" @(
        '-genkeypair', '-keystore', $ks, '-storepass', 'android', '-keypass', 'android',
        '-alias', 'androiddebugkey', '-dname', 'CN=Huazhou IME, O=Huazhou, C=CN',
        '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000'
    ) | Out-Null
}
Invoke-Native "$bt\apksigner.bat" @(
    'sign', '--ks', $ks, '--ks-key-alias', 'androiddebugkey',
    '--ks-pass', 'pass:android', '--key-pass', 'pass:android',
    '--out', "$build\huazhou-ime-v5.1.apk", "$build\aligned.apk"
) | Out-Null

Invoke-Native "$bt\apksigner.bat" @('verify', '--print-certs', "$build\huazhou-ime-v5.1.apk") | ForEach-Object { Write-Host $_ }
Write-Host "APK => $build\huazhou-ime-v5.1.apk" -ForegroundColor Green
Get-Item "$build\huazhou-ime-v5.1.apk" | Select-Object Name, Length
