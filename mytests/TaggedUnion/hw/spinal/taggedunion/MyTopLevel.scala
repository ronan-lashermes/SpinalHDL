package taggedunion

import spinal.core._
import spinal.lib._

// case class ReadWritePort() extends Bundle with IMasterSlave {
//   val addr = UInt(8 bits)
//   val read_data = Bits(32 bits)
//   val write_data = Bits(32 bits)
//   val readwrite = Bool()

//   def asMaster(): Unit = {
//     out(addr, write_data, readwrite)
//     in(read_data)
//   }
// }

// case class ReadPort() extends Bundle with IMasterSlave {
//   val addr = Stream (UInt(8 bits))
//   val data = Stream (Bits(32 bits))

//   def asMaster(): Unit = {
//     master(addr)
//     slave(data)
//   }
// }

// case class WritePort() extends Bundle with IMasterSlave {
//   val addr = Stream (UInt(8 bits))
//   val data = Stream (Bits(40 bits))

//   def asMaster(): Unit = {
//     master(addr, data)
//   }
// }

// object ReadWritePort extends TaggedUnion {
//     val read = ReadPort()
//     println(s"read width: ${read.getBitsWidth}")
//     val write = WritePort()
//     println(s"write width: ${write.getBitsWidth}")
// }

case class ReadPort() extends Bundle  {
    val r1 = in Bits(3 bits)
    val r2 = out Bits(2 bits)
}

case class WritePort() extends Bundle  {
    val w1 = out Bits(1 bits)
}

object ReadWritePort extends TaggedUnion {
    val read = ReadPort()
    val write = WritePort()
}

// Hardware definition
case class MemoryController() extends Component {
    val io = new Bundle {
        val rwPort = slave (ReadWritePort())
        val addr = out UInt(8 bits)
        val dataRead = in Bits(32 bits)
        val dataWrite = out Bits(32 bits)
    }

    
    io.dataWrite.assignDontCare()
    io.addr.assignDontCare()
    
    io.rwPort.oneof {
        case read: ReadPort => {
            read.r1 := 3
        }
        case write: WritePort => {
            
        }
        case _ => {
            SpinalInfo(s"other")
        }
    }
}

object MemoryControllerVerilog extends App {
    Config.spinal.generateVerilog(MemoryController())
}

