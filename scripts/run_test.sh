#!/usr/bin/env nix-shell
#!nix-shell ../env/testshell.nix -i bash --pure

cd "$SPINALHDL/mytests/TaggedUnion"
sbt -J-Xmx4G "runMain taggedunion.TaggedUnionIndependantTester"