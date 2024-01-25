/*
 * File: naxriscv.nix
 * Project: env
 * Created Date: Monday November 14th 2022
 * Author: Ronan (ronan.lashermes@inria.fr)
 * -----
 * Last Modified: Monday, 14th November 2022 3:45:58 pm
 * Modified By: Ronan (ronan.lashermes@inria.fr>)
 * -----
 * Copyright (c) 2022 INRIA
 */

let
    pkgs = import (builtins.fetchGit {
        # Descriptive name to make the store path easier to identify                
        name = "pinned_nix_packages";                                                 
        url = "https://github.com/nixos/nixpkgs/";                       
        ref = "nixos-23.05";                     
        rev = "e11142026e2cef35ea52c9205703823df225c947";                                           
    }) {};
in

with import <nixpkgs> { 
#     crossSystem = {
#         config = "riscv32-unknown-linux-gnu";
#   };
};

# Make a new "derivation" that represents our shell
stdenv.mkDerivation {
    name = "spinalhdl-test";

    SPINALHDL = toString ../.;

    # The packages in the `buildInputs` list will be added to the PATH in our shell
    nativeBuildInputs = with pkgs.python311Packages; [
        pkgs.zlib
        pkgs.verilator
        find-libpython
        cocotb
        pkgs.boost
        pkgs.sbt
        pkgs.verilog
        pkgs.ghdl
    ];



}
