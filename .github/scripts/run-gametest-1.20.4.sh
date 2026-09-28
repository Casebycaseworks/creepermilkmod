#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
java_home="${JAVA17_HOME:-/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home}"
report="${1:-$project_dir/build/reports/gametest/TEST-creepermilk-1.20.4.xml}"
runtime_log="${2:-$project_dir/build/reports/gametest/runtime-1.20.4.log}"
[[ -x "$java_home/bin/java" ]] || { echo "Missing Java 17: $java_home/bin/java" >&2; exit 1; }
case "$report" in /*) ;; *) report="$project_dir/$report" ;; esac
case "$runtime_log" in /*) ;; *) runtime_log="$project_dir/$runtime_log" ;; esac
for output in "$report" "$runtime_log"; do
    [[ ! -L "$output" ]] || { echo "Refusing symlinked output: $output" >&2; exit 1; }
done
mkdir -p "$(dirname "$report")" "$(dirname "$runtime_log")" "$project_dir/run/mods"
rm -f -- "$report" "$runtime_log" "$project_dir/run/mods/creepermilk-gametest.jar"

cd "$project_dir"
export JAVA_HOME="$java_home"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon --console=plain clean gametestJar validateReleaseJar
mkdir -p "$(dirname "$report")" "$(dirname "$runtime_log")"
{
    echo "JAVA_EXECUTABLE=$JAVA_HOME/bin/java"
    "$JAVA_HOME/bin/java" -version
} > "$runtime_log" 2>&1
cp -- build/libs/creepermilk-gametest-1.0.0.jar run/mods/creepermilk-gametest.jar
printf 'eula=true\n' > run/eula.txt
printf 'online-mode=false\nserver-port=0\nlevel-name=gametest-1.20.4\n' > run/server.properties

JAVA_TOOL_OPTIONS="-Dfabric-api.gametest=true -Dfabric-api.gametest.report-file=$report" \
    ./gradlew --no-daemon --console=plain runServer 2>&1 | tee -a "$runtime_log"

[[ -f "$report" && ! -L "$report" && -s "$report" ]]
[[ "$(rg -o '<testcase ' "$report" | wc -l | tr -d ' ')" -eq 4 ]]
! rg -q '<(failure|error)' "$report"
for test_name in \
    creepermilkgametests.survivalmilkinginventory \
    creepermilkgametests.milkclearsmultipleeffectsreturnsbucket \
    creepermilkgametests.creepermilkcraftscake \
    creepermilkgametests.conversionconsumesinputnoremainder; do
    rg -q "name=\"$test_name\"" "$report"
done
rg -q 'fabric-gametest-api-v1 1\.3\.6\+85d85a934f' "$runtime_log"
rg -q 'All 4 required tests passed' "$runtime_log"
rg -q 'Stopping server' "$runtime_log"
rg -q 'Saving worlds' "$runtime_log"
echo "Fresh four-case 1.20.4 GameTest JUnit passed: $report"
