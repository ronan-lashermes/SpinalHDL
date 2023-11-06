package spinal.lib

import spinal.core._
import spinal.core.internals._
import spinal.lib._
import spinal.idslplugin.{Location, ValCallback}

import scala.collection.mutable.Map
import spinal.idslplugin.PostInitCallback


// Tagged Union definition
class TaggedUnion(var encoding: SpinalEnumEncoding = native) extends Bundle with PostInitCallback {

    // TODO Should restrict the type of tagEnum to a subtype of SpinalEnum

    var tagEnum: SpinalEnum = new SpinalEnum(encoding)
    var tagElementsCache: Map[String, SpinalEnumElement[SpinalEnum]] = Map[String, SpinalEnumElement[SpinalEnum]]()
    var tag: SpinalEnumCraft[SpinalEnum] = null

    var nodir: Bits = null

    def build(): Unit = {
        assert(elementsCache.size > 0, "TaggedUnion must have at least one element") // TODO, deal with this edge case
        val unionHT = HardType.unionSeq(this.elementsCache.map(_._2))
        nodir = unionHT()

        
        elementsCache.foreach {
            case (name, element) => {
                val el = tagEnum.newElement(name)
                tagElementsCache += (name -> el)
            }
        }

        tag = tagEnum()

        // Adding these elements to generated HDL
        valCallbackRec(tag, "tag")   

        valCallbackRec(nodir, "nodir")
    }


    override def postInitCallback() = {
        build()
        this
    }

    def chooseOne[T <: Data](data: T)(callback: T => Unit): Unit = {
        val chosenElement = this.elementsCache.find(_._2 == data)
        chosenElement match {
            case Some((name, _)) => {
                val variant = tagElementsCache(name)
                this.tag := variant
                callback(data)
            }
            case None => SpinalError(s"$data is not a member of this TaggedUnion")
        }
    }
}

// Use Composition Instead of Inheritance: Instead of having TaggedUnion directly extend Bundle, you could have a member of TaggedUnion that is a Bundle. This way, only the members of that inner Bundle would be translated to HDL, and the outer TaggedUnion class can have additional members that don't get translated.

// For example:

// scala

// class TaggedUnion(...) {
//     class TaggedUnionIO extends Bundle {
//         val tag = tagEnum()
//         val nodir = HardType.unionSeq(...)
//     }
//     val io = new TaggedUnionIO
//     // other TaggedUnion code...
// }

// You would then reference tag and nodir as taggedUnion.io.tag and taggedUnion.io.nodir respectively.