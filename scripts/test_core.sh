#!/usr/bin/env nix-shell
#!nix-shell ../env/testshell.nix -i bash --pure

sbt 'tester/testOnly spinal.core.* -- -l spinal.tester.formal'