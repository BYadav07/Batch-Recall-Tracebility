#!/bin/sh
# Compile and run Batch Recall Traceability (needs JDK 8+ and lib/ojdbc8.jar or lib/ojdbc11.jar)
cd "$(dirname "$0")" || exit 1
mkdir -p out
javac -d out $(find src -name "*.java") || exit 1
java -cp "out:lib/*" trace.App db.properties
