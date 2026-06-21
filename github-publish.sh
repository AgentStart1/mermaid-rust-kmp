#!/bin/sh
# 从local.properties 中读取gpr.user和gpr.key
GPR_USER=$(grep '^gpr\.user=' local.properties | cut -d'=' -f2)
GPR_KEY=$(grep '^gpr\.key=' local.properties | cut -d'=' -f2)
echo "GPR_USER: $GPR_USER"
export VERSION=1.0.0-SNAPSHOT
export GROUP=com.storyteller_f.mermaid

cd mermaid-ffi/dist/kotlin-multiplatform
./gradlew publishAllPublicationsToGitHubPackagesRepository -Pgpr.user="$GPR_USER" -Pgpr.key="$GPR_KEY" -Pversion=$VERSION -Pgroup="${GROUP}"
