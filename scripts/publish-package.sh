#!/bin/bash
set -x

./gradlew :lib:bundleReleaseAar -x test
./gradlew publish -Pversion="$NEW_TAG"

exit
