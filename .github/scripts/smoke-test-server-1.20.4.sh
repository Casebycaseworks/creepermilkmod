#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
artifact="${1:-$project_dir/build/libs/creepermilk-1.20.4-1.0.0.jar}"
evidence_log="${2:-$project_dir/build/reports/server-smoke/server-latest.log}"
java_bin="${JAVA17_BIN:-/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/java}"
run_dir="$(mktemp -d "${TMPDIR:-/tmp}/creepermilk-1.20.4-smoke.XXXXXX")"
server_pid=""
input_open=false

case "$artifact" in /*) ;; *) artifact="$project_dir/$artifact" ;; esac
case "$evidence_log" in /*) ;; *) evidence_log="$project_dir/$evidence_log" ;; esac

cleanup() {
    status=$?
    set +e
    if [[ -n "$server_pid" ]] && kill -0 "$server_pid" 2>/dev/null; then
        [[ "$input_open" == true ]] && printf 'stop\n' >&3
        for _ in $(seq 1 30); do kill -0 "$server_pid" 2>/dev/null || break; sleep 1; done
        kill "$server_pid" 2>/dev/null || true
        wait "$server_pid" 2>/dev/null || true
    fi
    [[ "$input_open" == true ]] && exec 3>&-
    rm -rf -- "$run_dir"
    exit "$status"
}
trap cleanup EXIT

[[ -f "$artifact" ]] || { echo "Missing release artifact: $artifact" >&2; exit 1; }
[[ -x "$java_bin" ]] || { echo "Missing Java 17: $java_bin" >&2; exit 1; }
[[ ! -L "$evidence_log" ]] || { echo "Refusing symlinked evidence log" >&2; exit 1; }
mkdir -p "$run_dir/mods" "$(dirname "$evidence_log")"
rm -f -- "$evidence_log"
cp -- "$artifact" "$run_dir/mods/creepermilk-1.20.4-1.0.0.jar"
curl --fail --location --retry 3 --silent --show-error \
    'https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.97.3%2B1.20.4/fabric-api-0.97.3%2B1.20.4.jar' \
    --output "$run_dir/mods/fabric-api-0.97.3+1.20.4.jar"
curl --fail --location --retry 3 --silent --show-error \
    'https://maven.fabricmc.net/net/fabricmc/fabric-language-kotlin/1.13.2%2Bkotlin.2.1.20/fabric-language-kotlin-1.13.2%2Bkotlin.2.1.20.jar' \
    --output "$run_dir/mods/fabric-language-kotlin-1.13.2+kotlin.2.1.20.jar"
curl --fail --location --retry 3 --silent --show-error \
    'https://meta.fabricmc.net/v2/versions/loader/1.20.4/0.16.14/1.0.3/server/jar' \
    --output "$run_dir/fabric-server-launch.jar"
[[ "$(find "$run_dir/mods" -maxdepth 1 -type f -name '*.jar' | wc -l | tr -d ' ')" -eq 3 ]]
printf 'eula=true\n' > "$run_dir/eula.txt"
printf 'online-mode=false\nserver-port=0\nlevel-name=packaged-smoke-1.20.4\n' > "$run_dir/server.properties"
mkfifo "$run_dir/input"
exec 3<> "$run_dir/input"
input_open=true
(
    cd "$run_dir"
    exec "$java_bin" -Xms512M -Xmx1G -jar fabric-server-launch.jar nogui <&3 > "$evidence_log" 2>&1
) &
server_pid=$!
for _ in $(seq 1 180); do
    rg -q 'Done \(' "$evidence_log" 2>/dev/null && break
    kill -0 "$server_pid" 2>/dev/null || { cat "$evidence_log" >&2; exit 1; }
    sleep 1
done
rg -q 'Done \(' "$evidence_log"
rg -q 'Loading Minecraft 1\.20\.4 with Fabric Loader 0\.16\.14' "$evidence_log"
rg -q 'creepermilk 1\.0\.0' "$evidence_log"
rg -q 'fabric-api 0\.97\.3\+1\.20\.4' "$evidence_log"
rg -q 'fabric-language-kotlin 1\.13\.2\+kotlin\.2\.1\.20' "$evidence_log"
printf 'stop\n' >&3
for _ in $(seq 1 30); do kill -0 "$server_pid" 2>/dev/null || break; sleep 1; done
! kill -0 "$server_pid" 2>/dev/null
wait "$server_pid"
server_pid=""
exec 3>&-
input_open=false
rg -q 'Stopping server' "$evidence_log"
rg -q 'Saving worlds' "$evidence_log"
rg -q 'ThreadedAnvilChunkStorage: All dimensions are saved' "$evidence_log"
echo "Packaged 1.20.4 Java-17 server ready/save/stop smoke passed: $evidence_log"
