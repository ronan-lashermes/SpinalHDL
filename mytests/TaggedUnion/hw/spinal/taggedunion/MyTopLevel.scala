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
    taggedUnion.nodir := 0

    println(s"taggedUnion = ${taggedUnion}")
    println(s"taggedUnionBits = ${taggedUnion.nodir}")
    println(s"tag = ${taggedUnion.tag}")

    
    // println(s"tagEnum = ${taggedUnion.tagEnum.elements}")

    when(io.sel) {
        taggedUnion.choose(taggedUnion.a1) { 
            a1: TypeA => {
                a1.x := 1
                a1.y := 2
                a1.z := 3
            }
        }
    }
    .otherwise {
        taggedUnion.choose(taggedUnion.b) {
            b: TypeB => {
                b.l := 4
                b.m := 5
                b.n := 6
                b.r := -1
            }
        }
    }

    taggedUnion.among { 
        case (taggedUnion.a1, ha: TypeA) => {
            println("a1")
            io.res := ha.y
        }
        case (taggedUnion.b, hb: TypeB) => {
            println("b")
            io.res := hb.l.resized
        }
        case (x: Data, _) => {
            println("other " + x.toString())
            io.res := 0
        }
        
    }

}

object MemoryControllerVerilog extends App {
    Config.spinal.generateVerilog(MemoryController())
}
