package spinal.lib

import spinal.core._
import spinal.core.internals._
import spinal.lib._
import spinal.idslplugin.{Location, ValCallback}

import scala.collection.mutable.ArrayBuffer
import spinal.idslplugin.PostInitCallback


// Tagged Union definition
class TaggedUnion(var encoding: SpinalEnumEncoding = native) extends Bundle with PostInitCallback {

    val tag: SpinalEnum = new SpinalEnum(encoding)

    var nodir: Bits = null

    def build(): Unit = {
        assert(elementsCache.size > 0, "TaggedUnion must have at least one element") // TODO, deal with this edge case
        val unionHT = HardType.union(this.elementsCache.map(_._2): _*)
        nodir = unionHT()

        elementsCache.foreach {
            case (name, element) => {
                tag.newElement(name)
            }
        }
            
    }

    override def postInitCallback() = {
        build()
        this
    }
}




// class TaggedUnionCraft(var encoding: SpinalEnumEncoding = native) extends Bundle with PostInitCallback {

//     val tag: SpinalEnum = new SpinalEnum(encoding)

//     val nodir: Bits = null

//     def build(): Unit = {
//         // tag = UInt(log2Up(union.uTypes.size) bits)
//         // valCallbackRec(tag, "tag")
//         // val unionHT = HardType.union(tu.elements.map(_._2): _*)
//     }

//     override def postInitCallback() = {
//         // build()
//         this
//     }

// }