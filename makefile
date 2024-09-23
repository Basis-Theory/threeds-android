MAKEFLAGS += --silent

emulator-sync-clock:
	./scripts/emulator-sync-clock.sh

start-dev-server:
	./scripts/start-dev-server.sh

run-e2e:
	./scripts/run-e2e.sh
