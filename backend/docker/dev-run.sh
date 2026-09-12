#!/bin/sh
set -eu
# Recompile on source changes so DevTools can restart the JVM (classpath, not .java files).
(
  while inotifywait -r -e modify,create,delete,move /workspace/src >/dev/null 2>&1; do
    mvn -q compile -DskipTests || true
  done
) &
exec mvn spring-boot:run -Dspring-boot.run.jvmArguments="${JAVA_OPTS:-}"
