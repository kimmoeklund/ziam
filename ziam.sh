#!/bin/sh
# this should be replaced with native image of the CLI, but that needs bit more love

if [ $# -eq 0 ]; then
    sbt "runMain fi.kimmoeklund.ziam.Cli -- create --help"
  else 
    CMD="runMain fi.kimmoeklund.ziam.Cli $@"
    sbt "$CMD"
fi
