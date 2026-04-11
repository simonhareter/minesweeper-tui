#!/usr/bin/env sh

mkdir -p bin
find src -name "*.java" -print0 | xargs -0 javac -cp "lib/*" -d bin
java -cp "lib/*:bin" --enable-native-access=ALL-UNNAMED Main