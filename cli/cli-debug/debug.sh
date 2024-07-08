#!/usr/bin/env bash
#
# Copyright (C) Telicent Ltd
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

SCRIPT_DIR=$(dirname "${BASH_SOURCE[0]}")
SCRIPT_DIR=$(cd "${SCRIPT_DIR}" && pwd)

export CLASS_NAME="io.telicent.smart.cache.cli.commands.debug.DebugCli"
PROJECT_VERSION=${PROJECT_VERSION:-0.21.1-SNAPSHOT}
export JAR_NAME="cli-debug-${PROJECT_VERSION}.jar"
if [ -n "$1" ]; then
  export OTEL_SERVICE_NAME=debug-$1
else
  export OTEL_SERVICE_NAME=debug
fi

if [ -f "${SCRIPT_DIR}/cli-common.sh" ]; then
  exec "${SCRIPT_DIR}/cli-common.sh" "${SCRIPT_DIR}" "$@"
elif [ -f "${SCRIPT_DIR}/../cli-common.sh" ]; then
  exec "${SCRIPT_DIR}/../cli-common.sh" "${SCRIPT_DIR}" "$@"
else
  echo "Failed to locate CLI Launcher script" 1>&2
  exit 255
fi
