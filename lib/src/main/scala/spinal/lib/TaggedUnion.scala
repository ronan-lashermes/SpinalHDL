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
package spinal.lib

import spinal.core._
import spinal.core.internals._
import spinal.lib._
import spinal.idslplugin.{Location, ValCallback}

import scala.collection.mutable.ArrayBuffer



// Tagged Union definition
class TaggedUnion extends Nameable with ScalaLocated with ValCallbackRec {
    private val elements = ArrayBuffer[(String, _ <: Data)]()


    def apply() = atRest()
    def asMaster(): TaggedUnionCraft = {
        master (new TaggedUnionCraft(elements.map { case (name, data) => (name, TaggedUnion.asMasterOrClone(data)) }))
    }

    def asSlave(): TaggedUnionCraft = {
        slave (new TaggedUnionCraft(elements.map { case (name, data) => (name, TaggedUnion.asSlaveOrClone(data)) }))
    }

    def atRest(): TaggedUnionCraft = {
        var ret = new TaggedUnionCraft(elements.map { case (name, data) => (name, TaggedUnion.asDirectionLessOrClone(data)) })
        ret.setAsDirectionLess()
        ret
    }

    override def valCallbackRec(ref: Any, name: String): Unit = ref match {
        case ref : Data => {
            // Add the new item to `elements`
            elements += name -> ref
            // If certain conditions are met (as defined by `OwnableRef.proposal(ref, this)`), 
            // set a partial name for the new item
            if(OwnableRef.proposal(ref, this)) {
                ref.setPartialName(name, Nameable.DATAMODEL_WEAK)
            }
            // SpinalInfo(s"TaggedUnionElement: $name, ref direction: ${ref.dir}")
        }
        // If `ref` is not an instance of `Data`, do nothing
        case ref => {
        }
    }
}



object TaggedUnion {

    // There are two dimension to master/slave here:
    // 1. The master/slave of the TaggedUnion itself
    // 2. The master/slave of the elements of the TaggedUnion
    def asMasterOrClone(data: Data): Data = {
        data match {
            case masterSlave: IMasterSlave => {
                masterSlave.asMaster()
                masterSlave
            }
            case _ => data
        }
    }

    def asSlaveOrClone(data: Data): Data = {
        data match {
            case masterSlave: IMasterSlave => {
                masterSlave.asSlave()
                masterSlave
            }
            case _ => data
        }
    }

    def asDirectionLessOrClone(data: Data): Data = {
        data.setAsDirectionLess()
        data
    }
}

class TaggedUnionCraft(elements: ArrayBuffer[(String, _ <: Data)]) extends Bundle with IMasterSlave {
    val selector = UInt(log2Up(elements.length) bits)

    val inputBitWidth = elements.flatMap(_._2.flatten.filter(_.isInput)).map(_.getBitsWidth).reduceOption(Math.max).getOrElse(0)
    val outputBitWidth = elements.flatMap(_._2.flatten.filter(_.isOutput)).map(_.getBitsWidth).reduceOption(Math.max).getOrElse(0)
    val directionlessBitWidth = elements.flatMap(_._2.flatten.filter(_.isDirectionLess)).map(_.getBitsWidth).reduceOption(Math.max).getOrElse(0)

    val dataInMaster = (inputBitWidth > 0) generate (Bits(inputBitWidth bits))
    val dataOutMaster = (outputBitWidth > 0) generate (Bits(outputBitWidth bits))
    val dataDirectionLess = (directionlessBitWidth > 0) generate Bits(directionlessBitWidth bits)
    var isMaster: Boolean = false    

    def default(): Unit = {
        if (isMaster) {
            if(outputBitWidth > 0) {
                dataOutMaster.assignDontCare()
            }
            selector.assignDontCare()
        }
        else {
            if(inputBitWidth > 0) {
                dataInMaster.assignDontCare()
            }
        }
    }


    // def chooseOne(name: String)(callback: Data => Unit): Unit = {

    //     val elementData = elements.find(_._1 == name) match {
    //         case Some((_, data)) => data
    //         case None => throw new IllegalArgumentException(s"No element found with name: $name, legal names are: ${elements.map(_._1)}")
    //     }
        
    //     //set selector to index of element
    //     selector := elements.indexWhere(_._1 == name)

    //     var cursors = Map[String, Int](
    //         "in" -> 0,
    //         "out" -> 0,
    //         "directionless" -> 0
    //     )

    //     val dat = cloneOf(elementData)
    //     dat.assignDontCare()
    //     dat.flattenForeach { data =>
    //         val direction = if (data.getDirection == null) { dat.getDirection } else { data.getDirection }
    //         val bitWidth = data.getBitsWidth
    //         data.setAsDirectionLess()
            
    //         direction match {
    //             case `in` if (dataInMaster != null) => 
    //                 if (isMaster) {
    //                     data.assignFromBits(dataInMaster(cursors("in"), bitWidth bits))
    //                 }
    //                 else {
    //                     dataInMaster(cursors("in"), bitWidth bits) := data.asBits
    //                 }
                    
    //                 cursors = cursors.updated("in", cursors("in") + bitWidth)
    //             case `out` if (dataOutMaster != null) =>
    //                 if (isMaster) {
    //                     dataOutMaster(cursors("out"), bitWidth bits) := data.asBits
    //                 }
    //                 else {
    //                     data.assignFromBits(dataOutMaster(cursors("out"), bitWidth bits))
    //                 }

    //                 cursors = cursors.updated("out", cursors("out") + bitWidth)
    //             case _ if (dataDirectionLess != null) =>
    //                 data.assignFromBits(dataDirectionLess(cursors("directionless"), bitWidth bits))
    //                 cursors = cursors.updated("directionless", cursors("directionless") + bitWidth)
    //             case _ =>
    //         }
    //     }

    //     callback(dat)
    // }

    // def oneof(callback: (String, Data) => Unit): Unit = {

    //     for ((name, dataType) <- elements) {

    //         var cursors = Map[String, Int](
    //             "in" -> 0,
    //             "out" -> 0,
    //             "directionless" -> 0
    //         )


    //         when(selector === elements.indexWhere(_._1 == name)) {
    //             val dat = cloneOf(dataType)
    //             dat.flattenForeach { data =>
                    
    //                 val direction = if (data.getDirection == null) { dat.getDirection } else { data.getDirection }
    //                 val bitWidth = data.getBitsWidth
    //                 data.setAsDirectionLess()
                    
    //                 direction match {
    //                     case `in` if (dataInMaster != null) => 
    //                         if (isMaster) {
    //                             data.assignFromBits(dataInMaster(cursors("in"), bitWidth bits))
    //                         }
    //                         else {
    //                             dataInMaster(cursors("in"), bitWidth bits) := data.asBits
    //                         }
                            
    //                         cursors = cursors.updated("in", cursors("in") + bitWidth)
    //                     case `out` if (dataOutMaster != null) =>
    //                         if (isMaster) {
    //                             dataOutMaster(cursors("out"), bitWidth bits) := data.asBits
    //                         }
    //                         else {
    //                             data.assignFromBits(dataOutMaster(cursors("out"), bitWidth bits))
    //                         }

    //                         cursors = cursors.updated("out", cursors("out") + bitWidth)
    //                     case _ if (dataDirectionLess != null) =>
    //                         data.assignFromBits(dataDirectionLess(cursors("directionless"), bitWidth bits))
    //                         cursors = cursors.updated("directionless", cursors("directionless") + bitWidth)
    //                     case _ =>
    //                 }

                     
    //             }

    //             callback(name, dat)
    //         }
    //     }
    // }
    
    override def asMaster(): Unit = {

        isMaster = true
        out(selector)
        in(dataInMaster)
        out(dataOutMaster)
        out(dataDirectionLess)
    }

    override def asSlave(): Unit = {
        isMaster = false
        in(selector)
        out(dataInMaster)
        in(dataOutMaster)
        in(dataDirectionLess)
    }
}

object TaggedUnionCraft {
    private def setAsMasterIfPossible(data: Data): Unit = {
        data match {
            case masterSlave: IMasterSlave => masterSlave.asMaster()
            case _ =>
        }
    }
}



