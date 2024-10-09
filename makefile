MAKEFLAGS += --silent

emulator-sync-clock:
	./scripts/emulator-sync-clock.sh

start-dev-server:
	./scripts/start-dev-server.sh

publish-package:
	./scripts/publish-package.sh
