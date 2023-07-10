package taggedunion

import spinal.core._
import spinal.lib._

case class ReadWritePort() extends Bundle with IMasterSlave {
  val addr = UInt(8 bits)
  val read_data = Bits(32 bits)
  val write_data = Bits(32 bits)
  val readwrite = Bool()

  def asMaster(): Unit = {
    out(addr, write_data, readwrite)
    in(read_data)
  }
}

case class ReadPort() extends Bundle with IMasterSlave {
  val addr = UInt(8 bits)
  val data = Bits(32 bits)

  def asMaster(): Unit = {
    out(addr)
    in(data)
  }
}

case class WritePort() extends Bundle with IMasterSlave {
  val addr = UInt(8 bits)
  val data = Bits(32 bits)

  def asMaster(): Unit = {
    out(addr, data)
  }
}

object ReadWriteEnum extends TaggedUnion {
  val read = newElement()
  val write = newElement()
}

// Hardware definition
case class MyTopLevel() extends Component {
  val io = new Bundle {
    val cond0 = in  Bool()
    val cond1 = in  Bool()
    val flag  = out Bool()
    val state = out UInt(8 bits)
    val rw_enum = out (ReadWriteEnum())
  }

  val counter = Reg(UInt(8 bits)) init 0

  when(io.cond0) {
    counter := counter + 1
  }

  io.state := counter
  io.flag := (counter === 0) | io.cond1

  io.rw_enum := ReadWriteEnum.read
}

object MyTopLevelVerilog extends App {
  Config.spinal.generateVerilog(MyTopLevel())
}

object MyTopLevelVhdl extends App {
  Config.spinal.generateVhdl(MyTopLevel())
}
