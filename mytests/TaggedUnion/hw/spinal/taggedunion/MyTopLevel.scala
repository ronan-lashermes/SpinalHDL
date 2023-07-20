package taggedunion

import spinal.core._
import spinal.lib._

case class ReadRequest() extends Bundle {
    val add = UInt(8 bits)
}

case class WriteRequest() extends Bundle {
    val add = UInt(8 bits)
    val data = Bits(32 bits)
}

case class ReadAnswer() extends Bundle {
    val data = Bits(32 bits)
}

case class WriteAnswer() extends Bundle {
    val ack = Bits(0 bits) // label has no hardware existence
}

object ReadWriteRequest extends TaggedUnion  {
    // read or write -> TaggedUnion
    val read = ReadRequest()
    val write = WriteRequest()

   
}

object ReadWriteAnswer extends TaggedUnion {
    // read or write -> TaggedUnion
    val read = ReadAnswer()
    val write = WriteAnswer()
}

case class ReadWritePort() extends Bundle with IMasterSlave{
    // request and answer can be simultaneous -> Bundle
    val request = Stream(ReadWriteRequest.asMaster())
    // answer with content only if read request
    val answer = Flow(ReadWriteAnswer.asSlave())

    override def asMaster(): Unit = {
        master(request)
        slave(answer)
    }
}

// case class ReadPort() extends Bundle  {
//     val r1 = in Bits(8 bits)

// }

// case class WritePort() extends Bundle {
//     val r2 = in Bits(5 bits)
//     val r3 = out Bits(3 bits)
// }

// object ReadWritePort extends TaggedUnion {
//     // read or write -> TaggedUnion
//     val read = ReadPort()
//     val write = WritePort()
// }

case class MemoryController() extends Component {
    val io = new Bundle {
        val rwPort = slave (ReadWritePort())
        // val rwPort = ReadWritePort.asMaster()
        // val rw = in (Bool())
    }

    // io.rwPort.default()
    // when(io.rw) {
    //     io.rwPort.chooseOne("write") {
    //         case write: WritePort => {
    //             write.r3 := write.r2(2 downto 0)
    //         }
    //         case _ => {
    //             SpinalInfo(s"other")
    //         }
    //     }
    // }

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

 