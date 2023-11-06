package taggedunion

import spinal.core._
import spinal.lib._


case class TypeA() extends Bundle {
    val x, y, z = UInt(8 bits)
}

case class TypeB() extends Bundle {
    val l, m, n = UInt(6 bits)
    val r = SInt(2 bits)
}

case class TypeAorB() extends TaggedUnion {
    val a1 = TypeA()
    val a2 = TypeA()
    val b = TypeB()
}

case class MemoryController() extends Component {
    val io = new Bundle {
        val sel = in Bool()
        // val d = in TypeAorB()
        val res = out UInt(8 bits)
    }

   

    val taggedUnion = Reg(TypeAorB())

    println(s"taggedUnion = ${taggedUnion}")
    println(s"taggedUnionBits = ${taggedUnion.nodir}")
    println(s"tag = ${taggedUnion.tag}")

    
    // println(s"tagEnum = ${taggedUnion.tagEnum.elements}")

    when(io.sel) {
        taggedUnion.chooseOne(taggedUnion.a1) { 
            a1: TypeA => {
                a1.x := 1
                a1.y := 2
                a1.z := 3
            }
        }
    }
    // .otherwise {
    //     taggedUnion.chooseOne(taggedUnion.b) {
    //         b: TypeB => {
    //             b.l := 4
    //             b.m := 5
    //             b.n := 6
    //             b.r := -1
    //         }
    //     }
    // }

    val uint_union = taggedUnion.nodir.asUInt
    io.res := uint_union.resized
}

object MemoryControllerVerilog extends App {
    Config.spinal.generateVerilog(MemoryController())
}
