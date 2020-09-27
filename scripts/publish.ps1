param([string] $Version)

if ( -not ($Version)) {
    throw "You must supply a version either as the first argument, or via -Version. Ex: 1.2-beta"
    exit 1
}

function record {
    param(
        [parameter(ValueFromRemainingArguments)]
        [string[]] $Passthrough
    )

    Write-Host "+ $Passthrough"
    Invoke-Expression "$Passthrough"
}

record .\gradlew --console verbose assemble

record docker build -f .\Dockerfile -t alt-text-bot --build-arg version="$Version" .\build\libs

record docker tag alt-text-bot "glossawy/alt-text-bot:$Version"
record docker tag alt-text-bot glossawy/alt-text-bot:current

record docker push "glossawy/alt-text-bot:$Version"
record docker push glossawy/alt-text-bot:current
