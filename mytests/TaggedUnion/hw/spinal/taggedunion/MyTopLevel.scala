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

case class ReadWritePortTaggedUnion() extends TaggedUnion with IMasterSlave {
  val read = new ReadPort()
  val write = new WritePort()

  def asMaster(): Unit = {
    master(read, write)
  }
}

// Hardware definition
case class MyTopLevel() extends Component {
  val io = new Bundle {
    val cond0 = in  Bool()
    val cond1 = in  Bool()
    val flag  = out Bool()
    val state = out UInt(8 bits)
    val rw = master (ReadWritePortTaggedUnion())
  }

  val counter = Reg(UInt(8 bits)) init 0

  when(io.cond0) {
    counter := counter + 1
  }

  io.state := counter
  io.flag := (counter === 0) | io.cond1
  // io.rw.addr := counter
  // io.rw.write_data := 0
  // io.rw.readwrite := True

  val writePort = new WritePort()
  writePort.addr := counter
  writePort.data := 0
  io.rw.write := writePort

  io.rw.read.addr := counter
}

object MyTopLevelVerilog extends App {
  Config.spinal.generateVerilog(MyTopLevel())
}

object MyTopLevelVhdl extends App {
  Config.spinal.generateVhdl(MyTopLevel())
}
