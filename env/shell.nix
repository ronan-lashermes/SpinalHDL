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
        ref = "nixos-22.05";                     
        rev = "ce6aa13369b667ac2542593170993504932eb836";                                           
    }) {};

    riscv-toolchain = (import ./riscv-medany-multilib-toolchain-custom.nix);
    # riscv-toolchain = (import ./riscv-medany-multilib-toolchain.nix);

in

with import <nixpkgs> { 
#     crossSystem = {
#         config = "riscv32-unknown-linux-gnu";
#   };
};

# Make a new "derivation" that represents our shell
stdenv.mkDerivation {
    name = "naxriscv-build";

    SPINALHDL = toString ../SpinalHDL;
    NAXRISCV = toString ../.;
    # SIM = ../src/test/cpp/naxriscv/obj_dir/VNaxRiscv;
    RISCV = toString riscv-toolchain;
    RISCV_PATH = toString riscv-toolchain;

    # The packages in the `buildInputs` list will be added to the PATH in our shell
    nativeBuildInputs = with pkgs; [
        spike
        elfio
        verilator
        sbt
        zlib
        SDL2
        dtc
        boost
        python3
        python3.cocotb
        python3.find-libpython
#needed by the gcc install
        isl
        libmpc
        riscv-toolchain
    ];


    shellHook = ''
        export PATH=$PATH:$RISCV/bin
    '';

}
