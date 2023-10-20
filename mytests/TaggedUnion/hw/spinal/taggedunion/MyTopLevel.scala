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

case class MemoryController() extends Component {
    val io = new Bundle {
        val sel = in Bool()
        val res = out UInt(8 bits)
    }

    val AorB = HardType.union(TypeA(), TypeB())

    val v = AorB()
    v := 0

    val a: TypeA = v.aliasAs(TypeA())
    val b: TypeB = v.aliasAs(TypeB())

    when(io.sel) {
        a.y := U"8'hFF"
        b.m := U"6'hF"
    }

    io.res := a.x
}

object MemoryControllerVerilog extends App {
    Config.spinal.generateVerilog(MemoryController())
}
