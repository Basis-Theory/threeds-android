#!/bin/bash
set -e

# Save the current directory
current_directory="$PWD"

# Navigate to the target directory
cd "$(dirname "$0")/../.maestro/support/3ds-auth-backend"

echo "$BT_API_KEY_PVT"

yarn && yarn start &

result=$?

cd "$current_directory"

exit $result
