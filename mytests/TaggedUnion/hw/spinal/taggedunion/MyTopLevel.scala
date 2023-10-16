package taggedunion

import spinal.core._
import spinal.lib._

  case class MyTaggedUnion() extends TaggedUnion{
      val a = newElement(TypeA())
      val b = newElement(TypeB())
    }


    val mtu = MyTaggedUnion()
    mtu.setTag(mtu.b)
    mtu.union.raw := 0
    mtu.b.m := 12

    val rawrr = UInt(10 bits)
    mtu.sMatch{
      case mtu.a => rawrr := mtu.a.z.resized
      case mtu.b => rawrr := mtu.b.n.resized
    }

object MemoryControllerVerilog extends App {
    Config.spinal.generateVerilog(MemoryController())
}
