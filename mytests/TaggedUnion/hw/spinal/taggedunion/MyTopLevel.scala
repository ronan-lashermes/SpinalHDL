object UnionPlay extends App{
    SpinalVerilog(new Component {
        case class TypeA() extends Bundle {
            val x, y, z = UInt(8 bits)
        }

        case class TypeB() extends Bundle {
            val l, m, n = UInt(4 bits)
            val rgb = Rgb(2,3,4)
        }

        case class MyUnion() extends Union{
            val a = newElement(TypeA())
            val b = newElement(TypeB())
        }

        val miaou, wuff = MyUnion()
        wuff := miaou
        miaou.raw := 0
        //    val x = miaou.raw(4, 10 bits)
        //    val y = B"1001"
        //    x := y
        val b = miaou.b.get()
        //    b.m := U"1010"
        //    b.m(2) := True
        b.m(2 downto 1) := U"10"
        val sel = in UInt(2 bits)
        b.rgb.g(1) := False
        b.rgb.b(sel) := True
        miaou.a.get().z(sel, 2 bits) := U"11"

        miaou.a.y := U(4)
        miaou.a.x := U(4, 4 bits)
    })
}