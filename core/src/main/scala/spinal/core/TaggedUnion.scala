/*                                                                           *\
**        _____ ____  _____   _____    __                                    **
**       / ___// __ \/  _/ | / /   |  / /   HDL Core                         **
**       \__ \/ /_/ // //  |/ / /| | / /    (c) Dolu, All rights reserved    **
**      ___/ / ____// // /|  / ___ |/ /___                                   **
**     /____/_/   /___/_/ |_/_/  |_/_____/                                   **
**                                                                           **
**      This library is free software; you can redistribute it and/or        **
**    modify it under the terms of the GNU Lesser General Public             **
**    License as published by the Free Software Foundation; either           **
**    version 3.0 of the License, or (at your option) any later version.     **
**                                                                           **
**      This library is distributed in the hope that it will be useful,      **
**    but WITHOUT ANY WARRANTY; without even the implied warranty of         **
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU      **
**    Lesser General Public License for more details.                        **
**                                                                           **
**      You should have received a copy of the GNU Lesser General Public     **
**    License along with this library.                                       **
\*                                                                           */
package spinal.core

import scala.collection.mutable.ArrayBuffer
import spinal.core.internals._
import spinal.idslplugin.{Location, ValCallback}

import scala.collection.mutable
import spinal.idslplugin.PostInitCallback



class TaggedUnion(var encoding: SpinalEnumEncoding = native) extends MultiData with Nameable with ValCallbackRec with PostInitCallback {

    var hardtype: HardType[_] = null
    var elementsCache = ArrayBuffer[(String, Data)]()

    var tagEnum: SpinalEnum = new SpinalEnum(encoding)
    var tagElementsCache: Map[String, SpinalEnumElement[SpinalEnum]] = Map[String, SpinalEnumElement[SpinalEnum]]()
    var tag: SpinalEnumCraft[SpinalEnum] = null

    var nodir: Bits = null

    def default(): Unit = {
        this.nodir := 0
        this.tag := this.tagElementsCache.head._2
    }

    override def clone: TaggedUnion = {
        if (hardtype != null) {
        val ret = hardtype().asInstanceOf[this.type]
        ret.hardtype = hardtype
        return ret
        }
        super.clone.asInstanceOf[TaggedUnion]
    }

    /** Assign the bundle with an other bundle by name */
    def assignAllByName(that: TaggedUnion): Unit = {
        for ((name, element) <- elements) {
            val other = that.find(name)
            if (other == null)
                LocatedPendingError(s"TaggedUnion assignment is not complete. Missing $name")
            else element match {
                case b: TaggedUnion => b.assignAllByName(other.asInstanceOf[TaggedUnion])
                case _         => element := other
            }
        }
    }

    /** Assign all possible signal fo the bundle with an other bundle by name */
    def assignSomeByName(that: TaggedUnion): Unit = {
        for ((name, element) <- elements) {
            val other = that.find(name)
            if (other != null) {
                element match {
                    case b: TaggedUnion => b.assignSomeByName(other.asInstanceOf[TaggedUnion])
                    case _         => element := other
                }
            }
        }
    }

    def bundleAssign(that : TaggedUnion)(f : (Data, Data) => Unit): Unit ={
        for ((name, element) <- elements) {
            val other = that.find(name)
            if (other == null) {
                LocatedPendingError(s"TaggedUnion assignment is not complete. $this need '$name' but $that doesn't provide it.")
            }
            else {
                f(element, other)
            }
        }
    }

    protected override def assignFromImpl(that: AnyRef, target: AnyRef, kind: AnyRef)(implicit loc: Location): Unit = {
        that match {
            case that: TaggedUnion =>
                if (!this.getClass.isAssignableFrom(that.getClass)) SpinalError("TaggedUnions must have the same final class to" +
                    " be assigned. Either use assignByName or assignSomeByName at \n" + ScalaLocated.long)
                    bundleAssign(that)((to, from) => to.compositAssignFrom(from,to,kind))
            case _ => throw new Exception("Undefined assignment")
        }
    }


    override def valCallbackRec(ref: Any, name: String): Unit = ref match {
        case ref : Data => {
            elementsCache += name -> ref
            //   ref.parent = this
            //   if(OwnableRef.proposal(ref, this)) ref.setPartialName(name, Nameable.DATAMODEL_WEAK)
        }
        case ref =>
    }

    def build(): Unit = {
        assert(elementsCache.size > 0, "TaggedUnion must have at least one element") // TODO, deal with this edge case
        val unionHT = HardType.unionSeq(this.elementsCache.map(_._2))
        nodir = unionHT()
        nodir.setPartialName("nodir")
        nodir := 0 // I don't like that
        
        println("Build! " + this.getDisplayName() + "_nodir: " + nodir.toString())
        
        elementsCache.foreach {
            case (name, element) => {
                val el = tagEnum.newElement(name)
                tagElementsCache += (name -> el)
            }
        }

        tag = tagEnum()
        println("Elements: " + elements.toString())
        println("ElementCaches: " + elementsCache.toString())
        // valCallbackRec(tag, "tag")   
        // valCallbackRec(nodir, "nodir")
    }


    override def postInitCallback() = {
        build()
        this
    }


    override def elements: ArrayBuffer[(String, Data)] = {
        ArrayBuffer(("nodir" -> nodir), ("tag" -> tag))
    }

    private[core] def rejectOlder = true

    def getTypeString = getClass.getSimpleName

    override def toString(): String = s"${component.getPath() + "/" + this.getDisplayName()} : $getTypeString"

    def choose[T <: Data](data: T)(callback: T => Unit): Unit = {
        val chosenElement = this.elementsCache.find(_._2 == data)
        chosenElement match {
            case Some((name, _)) => {
                val variant = tagElementsCache(name)
                this.tag := variant
                callback(this.nodir.aliasAs(data))
            }
            case None => SpinalError(s"$data is not a member of this TaggedUnion")
        }
    }

    // First Data is equal to the variant, second to the actual hardware element
    def among(callback: (Data, Data) => Unit): Unit = {
        switch(this.tag) {
            for((name, enumVariant) <- this.tagElementsCache) {
                is(enumVariant) {
                    val dataVariant = this.elementsCache.find(_._1 == name)
                    dataVariant match {
                        case Some((_, d)) => {
                            val dataHardware = this.nodir.aliasAs(d)
                            callback(d, dataHardware)
                        }
                        case None => SpinalError(s"$name is not a member of this TaggedUnion")
                    }
                    
                }
            }
        }
    }
}

