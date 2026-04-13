SRC = $(shell find src -name "*.java")
BIN = bin
LIB = lib/*

JDBC_VERSION = 3.51.3.0
JDBC_JAR = lib/sqlite-jdbc.jar
JDBC_URL = https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/$(JDBC_VERSION)/sqlite-jdbc-$(JDBC_VERSION).jar
JDBC_JAR_HASH = lib/jar.sha256

.PHONY: build run clean

build:
	@mkdir -p lib bin
	@if [ ! -f lib/sqlite-jdbc.jar ]; then \
		echo "Downloading SQLite JDBC..."; \
		curl -L -o $(JDBC_JAR) https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.51.3.0/sqlite-jdbc-$(JDBC_VERSION).jar ; \
        curl -o $(JDBC_JAR_HASH) $(JDBC_URL).sha256 ; \
        echo "Verifying checksum..."; \
        sha256sum -c $(JDBC_JAR_HASH) - || (echo "Hash mismatch!" && rm -f $(JDBC_JAR) && exit 1); \
	fi

	@echo "Compiling Minesweeper..."
	@javac -cp "$(LIB)" -d $(BIN) $(SRC)

run:
	@java -cp "$(BIN):$(LIB)" --enable-native-access=ALL-UNNAMED Main

clean:
	@rm -rf bin
