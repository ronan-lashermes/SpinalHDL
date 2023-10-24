package spinal.core

import spinal.core.internals._
import spinal.idslplugin.PostInitCallback

class TaggedUnion extends Bundle with PostInitCallback{
    var tag : UInt = null
    // val union = new Union(selfBuild = false)
    val union = new Union()

    def newElement[T <: Data](t: HardType[T]) = {
        union.newElement(t)
    }

    def build(): Unit = {
        union.build()
        tag = UInt(log2Up(union.uTypes.size) bits)
        valCallbackRec(tag, "tag")
    }

    override def postInitCallback() = {
        build()
        this
    }

    def setTag(e : UnionElement[_ <: Data]) = {
        tag := union.uTypes.indexOf(e)
    }
    
    def sMatch(body : Any => Unit): Unit = {
        switch(tag){
            for((e, i) <- union.uTypes.zipWithIndex) is(i){
                body(e)
            }
        }
    }
}