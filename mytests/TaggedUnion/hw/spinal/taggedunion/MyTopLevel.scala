package taggedunion

import spinal.core._
import spinal.lib._

case class ReadRequest() extends Bundle {
    val address = UInt(32 bits)
}

case class ReadResponse() extends Bundle {
    val value = Bits(32 bits)
}

case class WriteRequest() extends Bundle {
    val address = UInt(32 bits)
    val value = Bits(32 bits)
}

case class RWRequest() extends TaggedUnion {
    val read = ReadRequest()
    val write = WriteRequest()
}

case class RWResponse() extends TaggedUnion {
    val read = ReadResponse()
}



case class MemoryController() extends Component {
    val io = new Bundle {
        val request = master(Stream(RWRequest()))
        val response = slave(Flow(RWResponse()))

        val doReq = in Bool()
        val rw = in Bool()

        val answer = out Bits(32 bits)
    }

    io.request.payload.assignDontCare()


    io.request.valid := False
    when(io.doReq) {
        io.request.valid := True
        when(io.rw) { //bad !
            io.request.payload.choose(io.request.payload.write) {
                wReq: WriteRequest => {
                    wReq.address := 2
                    wReq.value := 0
                }
            }
        }
        .otherwise {
            io.request.payload.choose(io.request.payload.read) {
                rReq: ReadRequest => {
                    rReq.address := 1
                }
            }
        }
    }

    io.answer.assignDontCare()

    when(io.response.valid) {
        io.response.payload.among {
            case (io.response.payload.read, rRes: ReadResponse) => {
                io.answer := rRes.value
            }
        }
    }
    
}

object MemoryControllerVerilog extends App {
    Config.spinal.generateVerilog(MemoryController())
}
