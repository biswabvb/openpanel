#!/usr/bin/env bash
# ==============================================================================
# OpenPanel / ABCD Panel Direct Shizuku CLI Tool
# Allows installing / removing configs and managing status directly in Termux!
# ==============================================================================

TARGET_GAMES=(
    "com.dts.freefiremax"
    "com.dts.freefireth"
    "com.garena.game.ffi"
)

CONFIG_JSON='{"verAddr":"https://uidbypass.fun/"}'

STATUS_TEXT="STATUS: ONLINE
EXPIRES: PERMANENT / UNLIMITED
SELLER: OPEN SOURCE CLEAN
FEATURES: AIM SILENT • LOCATION • BYPASS"

echo "=================================================="
echo "    ABCD PANEL - DIRECT CLI MANAGER (NO KEY)      "
echo "=================================================="

# Function to run elevated command (via rish or su)
run_elevated() {
    local cmd="$1"
    if command -v rish >/dev/null 2>&1; then
        rish -c "$cmd"
    elif command -v su >/dev/null 2>&1; then
        su -c "$cmd"
    else
        echo "Error: Neither 'rish' (Shizuku) nor 'su' (Root) was found."
        echo "Make sure Shizuku is running and 'rish' is configured in Termux."
        return 1
    fi
}

install_config() {
    local pkg="$1"
    echo "[*] Target Game: $pkg"
    local target_dir="/storage/emulated/0/Android/data/$pkg/files"
    local config_file="$target_dir/localConfig.json"
    local status_file="$target_dir/key_status.txt"

    echo "[*] Creating target directory..."
    run_elevated "mkdir -p '$target_dir'"

    echo "[*] Writing localConfig.json..."
    run_elevated "echo '$CONFIG_JSON' > '$config_file' && chmod 777 '$config_file'"

    echo "[*] Writing key_status.txt..."
    run_elevated "echo '$STATUS_TEXT' > '$status_file' && chmod 777 '$status_file'"

    echo "[+] Verifying installation..."
    if run_elevated "[ -f '$config_file' ]"; then
        echo "[SUCCESS] Config successfully installed for $pkg!"
        echo "Status: ONLINE (No key verification required)"
    else
        echo "[-] Failed to verify config file."
    fi
}

remove_config() {
    local pkg="$1"
    echo "[*] Removing config from $pkg..."
    local target_dir="/storage/emulated/0/Android/data/$pkg/files"
    run_elevated "rm -f '$target_dir/localConfig.json' '$target_dir/key_status.txt'"
    echo "[+] Config removed. Status: OFFLINE"
}

check_status() {
    for pkg in "${TARGET_GAMES[@]}"; do
        local config_file="/storage/emulated/0/Android/data/$pkg/files/localConfig.json"
        if run_elevated "[ -f '$config_file' ]" >/dev/null 2>&1; then
            echo "[ONLINE]  $pkg -> Config is INSTALLED"
        else
            echo "[OFFLINE] $pkg -> Not installed"
        fi
    done
}

launch_game() {
    local pkg="$1"
    echo "[*] Launching $pkg..."
    run_elevated "monkey -p '$pkg' -c android.intent.category.LAUNCHER 1"
}

case "$1" in
    install)
        target="${2:-com.dts.freefiremax}"
        install_config "$target"
        ;;
    remove)
        target="${2:-com.dts.freefiremax}"
        remove_config "$target"
        ;;
    status)
        check_status
        ;;
    launch)
        target="${2:-com.dts.freefiremax}"
        launch_game "$target"
        ;;
    *)
        echo "Usage: $0 {install|remove|status|launch} [package_name]"
        echo ""
        echo "Available packages:"
        for p in "${TARGET_GAMES[@]}"; do
            echo "  - $p"
        done
        echo ""
        echo "Examples:"
        echo "  $0 install com.dts.freefiremax"
        echo "  $0 status"
        echo "  $0 remove com.dts.freefiremax"
        ;;
esac
