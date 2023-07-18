package taggedunion

import spinal.core._
import spinal.lib._

// case class ReadRequest() extends Bundle {
//     val add = UInt(8 bits)
// }

// case class WriteRequest() extends Bundle {
//     val add = UInt(8 bits)
//     val data = Bits(32 bits)
// }

// case class ReadAnswer() extends Bundle {
//     val data = Bits(32 bits)
// }

// case class WriteAnswer() extends Bundle {
//     val ack = Bits(0 bits) // label has no hardware existence
// }

// object ReadWriteRequest extends TaggedUnion with IMasterSlave {
//     // read or write -> TaggedUnion
//     val read = ReadRequest()
//     val write = WriteRequest()

//     def asMaster(): Unit = {
//         out(read, write)
//     }
// }

// object ReadWriteAnswer extends TaggedUnion with IMasterSlave{
//     // read or write -> TaggedUnion
//     val read = ReadAnswer()
//     val write = WriteAnswer()

//     def asMaster(): Unit = {
//         out(read, write)
//     }
// }

// case class ReadWritePort extends Bundle with IMasterSlave{
//     // request and answer can be simultaneous -> Bundle
//     val request = Stream(ReadWriteRequest())
//     // answer with content only if read request
//     val answer = Flow(ReadWriteAnswer())

//     def asMaster(): Unit = {
//         master(request)
//         slave(answer)
//     }
// }

case class ReadPort extends Bundle with IMasterSlave {
    val r1 = in Bits(8 bits)

    def asMaster(): Unit = {
        in(r1)
    }
}

case class WritePort extends Bundle with IMasterSlave {
    val r2 = in Bits(5 bits)
    val r3 = out Bits(3 bits)

    def asMaster(): Unit = {
        in(r2)
        out(r3)
    }
}

object ReadWritePort extends TaggedUnion with IMasterSlave {
    // read or write -> TaggedUnion
    val read = ReadPort()
    val write = WritePort()

    def asMaster(): Unit = {
        master(read, write)
    }
}

case class MemoryController() extends Component {
    val io = new Bundle {
        val rwPort = ReadWritePort.asMaster()
        val rw = Bool()
    }

    when(rw) {
        rwPort.chooseOne {
            
        }
    }

    // val memory = Mem(Bits(32 bits), 256)

    // val request_valid = io.rwPort.request.valid
    // io.rwPort.request.payload.oneof {
    //     case read: ReadRequest => {
            
    //     }
    //     case write: WriteRequest => {
            
    //     }
    //     case _ => {
    //         SpinalInfo(s"other")
    //     }
    // }
}

object MemoryControllerVerilog extends App {
    Config.spinal.generateVerilog(MemoryController())
}

 