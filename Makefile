SRC = $(shell find src -name "*.java")
BIN = bin
LIB = lib/*

.PHONY: build run clean

build:
	@mkdir -p lib bin
	@if [ ! -f lib/sqlite-jdbc.jar ]; then \
		echo "Downloading SQLite JDBC..."; \
		curl -L -o lib/sqlite-jdbc.jar https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.51.3.0/sqlite-jdbc-3.51.3.0.jar; \
	fi

	@echo "Compiling Minesweeper..."
	@javac -cp "$(LIB)" -d $(BIN) $(SRC)

run:
	@java -cp "$(BIN):$(LIB)" --enable-native-access=ALL-UNNAMED Main

clean:
	@rm -rf bin