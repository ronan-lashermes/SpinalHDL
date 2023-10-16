#!/usr/bin/env nix-shell
#!nix-shell ../env/testshell.nix -i bash --pure

cd "$SPINALHDL/mytests/TaggedUnion"
sbt "runMain taggedunion.MemoryControllerVerilog"