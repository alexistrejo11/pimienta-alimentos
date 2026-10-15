#!/bin/sh
set -eu

# A slow link often cuts a jar off mid-download. Resolve first and retry that
# step so one truncated transfer does not throw away the whole startup.
attempt=1
while [ "$attempt" -le 8 ]; do
  if mvn -B -DskipTests dependency:resolve dependency:resolve-plugins; then
    break
  fi
  if [ "$attempt" -eq 8 ]; then
    echo "Dependency download failed after ${attempt} attempts."
    exit 1
  fi
  echo "Dependency download failed (attempt ${attempt}/8). Retrying in 15s..."
  attempt=$((attempt + 1))
  sleep 15
done

# Recompile on source changes so DevTools can restart the JVM (classpath, not .java files).
(
  while inotifywait -r -e modify,create,delete,move /workspace/src >/dev/null 2>&1; do
    mvn -q compile -DskipTests || true
  done
) &
exec mvn spring-boot:run -Dspring-boot.run.jvmArguments="${JAVA_OPTS:-}"
