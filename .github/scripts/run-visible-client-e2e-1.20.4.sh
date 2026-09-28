#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
artifact="${1:-$project_dir/build/libs/creepermilk-1.20.4-1.0.0.jar}"
evidence_dir="${2:-$project_dir/build/reports/client-e2e}"
prism_root="${PRISM_ROOT:-$HOME/Library/Application Support/PrismLauncher}"
instance_id="${PRISM_INSTANCE_ID:-Creeper-Milk-1.20.4-Test}"
prism_bin="${PRISM_BIN:-/Applications/Prism Launcher.app/Contents/MacOS/prismlauncher}"
java_bin="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/java"
port="${CREEPER_MILK_E2E_PORT:-25573}"
profile="$prism_root/instances/$instance_id"
profile_game="$profile/minecraft"
profile_mods="$profile_game/mods"
profile_release="$profile_mods/creepermilk-1.20.4-1.0.0.jar"
profile_fapi="$profile_mods/fabric-api-0.97.3+1.20.4.jar"
profile_flk="$profile_mods/fabric-language-kotlin-1.13.2+kotlin.2.1.20.jar"
profile_harness="$profile_mods/creepermilk-client-e2e-1.0.0.jar"
profile_report="$profile_game/creepermilk-client-e2e-result.json"
profile_log="$profile_game/logs/latest.log"
profile_address="$profile_game/creepermilk-client-e2e-server.txt"
run_dir="$(mktemp -d "${TMPDIR:-/tmp}/creepermilk-1.20.4-e2e.XXXXXX")"
server_pid=""
prism_pid=""
input_open=false
harness=""
fresh=false

preserve() {
    mkdir -p "$evidence_dir"
    [[ "$fresh" == true && -s "$profile_report" ]] && cp -- "$profile_report" "$evidence_dir/client-result.json"
    [[ "$fresh" == true && -s "$profile_log" ]] && cp -- "$profile_log" "$evidence_dir/client-latest.log"
    [[ -s "$run_dir/server/server.log" ]] && cp -- "$run_dir/server/server.log" "$evidence_dir/server-latest.log"
    [[ -n "$harness" && -f "$harness" ]] && cp -- "$harness" "$evidence_dir/client-e2e-harness-USED.jar"
}

cleanup() {
    status=$?
    set +e
    if [[ -n "$server_pid" ]] && kill -0 "$server_pid" 2>/dev/null; then
        [[ "$input_open" == true ]] && printf 'stop\n' >&3
        for _ in $(seq 1 30); do kill -0 "$server_pid" 2>/dev/null || break; sleep 1; done
        kill "$server_pid" 2>/dev/null || true
        wait "$server_pid" 2>/dev/null || true
    fi
    if [[ -n "$prism_pid" ]] && kill -0 "$prism_pid" 2>/dev/null; then
        kill -TERM "$prism_pid" 2>/dev/null || true
        wait "$prism_pid" 2>/dev/null || true
    fi
    [[ "$input_open" == true ]] && exec 3>&-
    preserve
    rm -f -- "$profile_harness" "$profile_address"
    rm -rf -- "$run_dir"
    exit "$status"
}
trap cleanup EXIT

[[ -f "$artifact" ]] || { echo "Missing artifact: $artifact" >&2; exit 1; }
[[ -x "$prism_bin" && -x "$java_bin" ]] || { echo "Missing Prism or Java 17" >&2; exit 1; }
[[ -f "$profile_release" && -f "$profile_fapi" && -f "$profile_flk" ]] || {
    echo "Profile is missing an exact release dependency" >&2; exit 1;
}
cmp "$artifact" "$profile_release"
[[ "$(find "$profile_mods" -maxdepth 1 -type f -name '*.jar' | wc -l | tr -d ' ')" -eq 3 ]] || {
    echo "Profile must contain exactly release, FAPI, and FLK before E2E" >&2; exit 1;
}
rg -q '^name=Creeper Milk 1\.20\.4 Test$' "$profile/instance.cfg"
rg -q '^JavaPath=/Library/Java/JavaVirtualMachines/temurin-17\.jdk/Contents/Home/bin/java$' "$profile/instance.cfg"
rg -q '"version": "1\.20\.4"' "$profile/mmc-pack.json"
rg -q '"version": "0\.16\.14"' "$profile/mmc-pack.json"
[[ ! -L "$evidence_dir" ]] || { echo "Refusing symlinked evidence directory" >&2; exit 1; }
mkdir -p "$evidence_dir"
rm -f -- "$evidence_dir/client-result.json" "$evidence_dir/client-latest.log" \
    "$evidence_dir/server-latest.log" "$evidence_dir/client-e2e-harness-USED.jar"

cd "$project_dir"
export JAVA_HOME="${java_bin%/bin/java}"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon --console=plain remapClientE2eJar
harness="$project_dir/build/libs/creepermilk-client-e2e-1.0.0-installed.jar"
[[ -s "$harness" ]]

mkdir -p "$run_dir/server/mods"
cp -- "$artifact" "$run_dir/server/mods/creepermilk-1.20.4-1.0.0.jar"
cp -- "$profile_fapi" "$run_dir/server/mods/"
cp -- "$profile_flk" "$run_dir/server/mods/"
cp -- "$harness" "$run_dir/server/mods/creepermilk-client-e2e-1.0.0.jar"
cp -- "$harness" "$profile_harness"
printf '127.0.0.1:%s\n' "$port" > "$profile_address"

rm -f -- "$profile_report"
if [[ -e "$profile_log" ]]; then
    mv -- "$profile_log" "$profile_log.previous-$(date +%Y%m%d-%H%M%S)"
fi
fresh=true

curl --fail --location --retry 3 --silent --show-error \
    'https://meta.fabricmc.net/v2/versions/loader/1.20.4/0.16.14/1.0.3/server/jar' \
    --output "$run_dir/server/fabric-server-launch.jar"
printf 'eula=true\n' > "$run_dir/server/eula.txt"
printf '%s\n' 'online-mode=false' "server-port=$port" \
    'level-name=creeper-milk-visible-e2e-1.20.4' 'gamemode=survival' \
    'difficulty=normal' 'spawn-protection=0' 'view-distance=6' \
    'simulation-distance=6' > "$run_dir/server/server.properties"
mkfifo "$run_dir/server-input"
exec 3<> "$run_dir/server-input"
input_open=true
(
    cd "$run_dir/server"
    exec "$java_bin" -Xms512M -Xmx1G -jar fabric-server-launch.jar nogui \
        <&3 > server.log 2>&1
) &
server_pid=$!
for _ in $(seq 1 180); do
    rg -q 'Done \(' "$run_dir/server/server.log" 2>/dev/null && break
    kill -0 "$server_pid" 2>/dev/null || { cat "$run_dir/server/server.log" >&2; exit 1; }
    sleep 1
done
rg -q 'Done \(' "$run_dir/server/server.log"
rg -q 'Loading Minecraft 1\.20\.4 with Fabric Loader 0\.16\.14' "$run_dir/server/server.log"

"$prism_bin" --launch "$instance_id" --server "127.0.0.1:$port" --show-window &
prism_pid=$!
for _ in $(seq 1 240); do
    [[ -s "$profile_report" ]] && break
    if [[ -s "$profile_log" ]] && rg -q \
        'CREEPER_MILK_CLIENT_E2E_FAIL|Could not execute entrypoint|Failed to start' "$profile_log"; then
        tail -n 240 "$profile_log" >&2
        exit 1
    fi
    sleep 1
done
[[ -s "$profile_report" ]] || { tail -n 240 "$profile_log" >&2 || true; exit 1; }
rg -q '"result": "PASS"' "$profile_report"
rg -q '"minecraft": "1\.20\.4"' "$profile_report"
rg -q '"modVersion": "1\.0\.0"' "$profile_report"
for marker in \
    CREEPER_MILK_CLIENT_E2E_PASS \
    CREEPER_MILK_CLIENT_E2E_MILKING_PASS \
    CREEPER_MILK_CLIENT_E2E_CREEPER_DRINK_PASS \
    CREEPER_MILK_CLIENT_E2E_CREEPER_DRINK_SOUND_PASS \
    CREEPER_MILK_CLIENT_E2E_VANILLA_DRINK_PASS \
    CREEPER_MILK_CLIENT_E2E_VANILLA_DRINK_SOUND_PASS \
    CREEPER_MILK_CLIENT_E2E_CONVERSION_PASS \
    CREEPER_MILK_CLIENT_E2E_FINAL_ACCOUNTING_PASS; do
    rg -q "$marker" "$profile_log"
done
rg -q 'CREEPER_MILK_CLIENT_E2E_MILKING_SOUND_(PASS|KNOWN_LIMITATION)' "$profile_log"
rg -q 'CREEPER_MILK_CLIENT_E2E_SERVER_ADVANCEMENT_PASS' "$run_dir/server/server.log"
for pattern in MMM CMM MCM MMC CCM CMC MCC CCC; do
    rg -q "CREEPER_MILK_CLIENT_E2E_CAKE_PASS .*pattern=$pattern" "$profile_log"
done
for _ in $(seq 1 60); do
    rg -q 'CREEPER_MILK_CLIENT_E2E_CLEAN_CLIENT_STOP' "$profile_log" \
        && rg -q 'Stopping!' "$profile_log" && break
    sleep 1
done
rg -q 'CREEPER_MILK_CLIENT_E2E_CLEAN_CLIENT_STOP' "$profile_log"
rg -q 'Stopping!' "$profile_log"

# A newly started Prism GUI remains alive after the game exits, while an IPC
# invocation against an existing GUI exits immediately.  In either case, the
# launched game has already demonstrated a normal client stop at this point.
if kill -0 "$prism_pid" 2>/dev/null; then
    kill -TERM "$prism_pid"
    wait "$prism_pid" 2>/dev/null || true
fi
prism_pid=""

printf 'stop\n' >&3
for _ in $(seq 1 30); do kill -0 "$server_pid" 2>/dev/null || break; sleep 1; done
! kill -0 "$server_pid" 2>/dev/null
wait "$server_pid"
server_pid=""
exec 3>&-
input_open=false
rg -q 'Stopping server' "$run_dir/server/server.log"
rg -q 'Saving worlds' "$run_dir/server/server.log"
rg -q 'ThreadedAnvilChunkStorage: All dimensions are saved' "$run_dir/server/server.log"

preserve
for f in client-result.json client-latest.log server-latest.log client-e2e-harness-USED.jar; do
    [[ -s "$evidence_dir/$f" ]]
done
echo "Visible Prism 1.20.4 full-contract E2E passed: $evidence_dir"
