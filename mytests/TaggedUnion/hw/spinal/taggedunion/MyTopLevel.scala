package taggedunion

import spinal.core._
import spinal.lib._


case class TypeA() extends Bundle {
    val x, y, z = UInt(8 bits)
}

case class TypeB() extends Bundle {
    val l, m, n = UInt(4 bits)
    val r = SInt(2 bits)
}

case class MemoryController() extends Component {
    val io = new Bundle {
      
    }

    val AorB = HardType.union(TypeA(), TypeB())

    val v = AorB()
}

object MemoryControllerVerilog extends App {
    Config.spinal.generateVerilog(MemoryController())
}
