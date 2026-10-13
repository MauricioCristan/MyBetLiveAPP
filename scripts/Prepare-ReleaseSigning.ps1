param([string]$JavaHome = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot')
$ErrorActionPreference = 'Stop'
$taskRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$taskSigningDirectory = Join-Path $taskRoot '.local/signing'
$taskStore = Join-Path $taskSigningDirectory 'mybetlive-upload.keystore'
$taskProperties = Join-Path $taskSigningDirectory 'release-signing.properties'
$taskKeytool = Join-Path $JavaHome 'bin/keytool.exe'
if (!(Test-Path -LiteralPath $taskKeytool)) { throw 'Java keytool is unavailable.' }
if ((Test-Path -LiteralPath $taskStore) -or (Test-Path -LiteralPath $taskProperties)) {
    throw 'Signing files already exist. Restore or reuse them; this script never replaces an existing key.'
}
New-Item -ItemType Directory -Path $taskSigningDirectory -Force | Out-Null
$taskPassword = [Convert]::ToHexString([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
@"
storeFile=.local/signing/mybetlive-upload.keystore
storePassword=$taskPassword
keyAlias=mybetlive-upload
keyPassword=$taskPassword
"@ | Set-Content -LiteralPath $taskProperties -Encoding ascii
$previousPassword = $env:MYBET_UPLOAD_SIGNING_PASSWORD
try {
    $env:MYBET_UPLOAD_SIGNING_PASSWORD = $taskPassword
    & $taskKeytool -genkeypair -keystore $taskStore -storetype PKCS12 -alias mybetlive-upload `
        -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 10000 `
        -dname 'CN=MyBetLive, OU=Android, O=MyBetLive, C=MX' `
        -storepass:env MYBET_UPLOAD_SIGNING_PASSWORD -keypass:env MYBET_UPLOAD_SIGNING_PASSWORD
    if ($LASTEXITCODE -ne 0) { throw 'Key generation failed. Signing properties were preserved for recovery.' }
    $taskPublicCertificate = Join-Path $taskSigningDirectory 'upload-certificate.der'
    & $taskKeytool -exportcert -keystore $taskStore -storetype PKCS12 -alias mybetlive-upload `
        -storepass:env MYBET_UPLOAD_SIGNING_PASSWORD -file $taskPublicCertificate
    if ($LASTEXITCODE -ne 0) { throw 'Public certificate export failed.' }
    $taskCertificateBytes = [System.IO.File]::ReadAllBytes($taskPublicCertificate)
    $taskSha1 = ([System.Security.Cryptography.SHA1]::HashData($taskCertificateBytes) | ForEach-Object { $_.ToString('X2') }) -join ':'
    $taskSha256 = ([System.Security.Cryptography.SHA256]::HashData($taskCertificateBytes) | ForEach-Object { $_.ToString('X2') }) -join ':'
    @"
Package: com.mybetlive
Alias: mybetlive-upload
SHA-1: $taskSha1
SHA-256: $taskSha256
"@ | Set-Content -LiteralPath (Join-Path $taskSigningDirectory 'certificate-fingerprints.txt') -Encoding ascii
    Write-Output 'Created the first MyBetLive upload key. Credentials were not printed.'
    Write-Output "Private signing folder: $taskSigningDirectory"
    Write-Output 'Back up the keystore and signing properties securely before publishing.'
} finally {
    $env:MYBET_UPLOAD_SIGNING_PASSWORD = $previousPassword
    $taskPassword = $null
}
