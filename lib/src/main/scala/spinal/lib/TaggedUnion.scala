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


    def apply() = new TaggedUnionCraft(elements)


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

// class TaggedUnionCraft(elements: ArrayBuffer[(String, _ <: Data)]) extends Bundle {
//     val discriminant = UInt(log2Up(elements.length) bits)
//     val data = Bits(elements.map(_._2.getBitsWidth).max bits)
    
    
    // def oneof(callback: PartialFunction[Data, Unit]): Unit = {
    //     for ((name, dataType) <- elements) {
    //         when(discriminant === elements.indexWhere(_._1 == name)) {
    //             val dat = cloneOf(dataType)
    //             dat.assignFromBits(data)
    //             callback(dat)
    //         }
    //     }
    // }
// }

class TaggedUnionCraft(elements: ArrayBuffer[(String, _ <: Data)]) extends Bundle with IMasterSlave {
    val discriminant = UInt(log2Up(elements.length) bits)

    val inputBitWidth = elements.flatMap(_._2.flatten.filter(_.isInput)).map(_.getBitsWidth).reduceOption(Math.max).getOrElse(0)
    val outputBitWidth = elements.flatMap(_._2.flatten.filter(_.isOutput)).map(_.getBitsWidth).reduceOption(Math.max).getOrElse(0)
    val directionlessBitWidth = elements.flatMap(_._2.flatten.filter(_.isDirectionLess)).map(_.getBitsWidth).reduceOption(Math.max).getOrElse(0)

    val dataIn = (inputBitWidth > 0) generate (Bits(inputBitWidth bits))
    val dataOut = (outputBitWidth > 0) generate (Bits(outputBitWidth bits))
    val dataDirectionLess = (directionlessBitWidth > 0) generate Bits(directionlessBitWidth bits)
    var isMaster: Boolean = false

    SpinalInfo(s"inputBitWidth: ${inputBitWidth}, outputBitWidth: ${outputBitWidth}, directionlessBitWidth: ${directionlessBitWidth}")

    if (isMaster) {
        dataOut.assignDontCare()
        // dataOut := 0
    }
    else {
        dataIn.assignDontCare()
        // dataIn := 0
    }

    // def oneof(callback: PartialFunction[Data, Unit]): Unit = {
    def oneof(callback: Data => Unit): Unit = {
        


        // if (isMaster) {
        //     dataOut.assignDontCare()
        //     // dataOut := 0
        // }
        // else {
        //     dataIn.assignDontCare()
        //     // dataIn := 0
        // }

        for ((name, dataType) <- elements) {

            var cursors = Map[String, Int](
                "in" -> 0,
                "out" -> 0,
                "directionless" -> 0
            )

            val dat = cloneOf(dataType)

            when(discriminant === elements.indexWhere(_._1 == name)) {
            
                dat.flattenForeach { data =>
                    
                    val direction = if (data.getDirection == null) { dat.getDirection } else { data.getDirection }
                    val bitWidth = data.getBitsWidth
                    data.setAsDirectionLess()
                    
                    // SpinalInfo(s"TaggedUnionElement: $name, width: ${bitWidth}, ref direction: $direction, parent direction: ${dataType.getDirection}")
                    direction match {
                        case `in` if (dataIn != null) => 
                            if (isMaster) {
                                data.assignFromBits(dataIn(cursors("in"), bitWidth bits))
                            }
                            else {
                                dataIn(cursors("in"), bitWidth bits) := data.asBits
                            }
                            
                            SpinalInfo(s"dataIn assign for ${name}, cursor: ${cursors("in")}, bitWidth: ${bitWidth}")
                            cursors = cursors.updated("in", cursors("in") + bitWidth)
                        case `out` if (dataOut != null) =>
                            if (isMaster) {
                                dataOut(cursors("out"), bitWidth bits) := data.asBits
                            }
                            else {
                                data.assignFromBits(dataOut(cursors("out"), bitWidth bits))
                            }

                            // data.assignFromBits(dataOut(cursors("out"), bitWidth bits))
                            // dataOut(cursors("out"), bitWidth bits) := data.asBits
                            SpinalInfo(s"dataOut assign for ${name}, cursor: ${cursors("out")}, bitWidth: ${bitWidth}")
                            cursors = cursors.updated("out", cursors("out") + bitWidth)
                        case _ if (dataDirectionLess != null) =>
                            data.assignFromBits(dataDirectionLess(cursors("directionless"), bitWidth bits))
                            cursors = cursors.updated("directionless", cursors("directionless") + bitWidth)
                        case _ =>
                    }

                     
                }

                // if (isMaster) {
                //         // write 0 in dataOut until end of dataOut width
                //     if (dataOut != null) {
                //         dataOut(cursors("out"), (outputBitWidth - cursors("out")) bits) := 0
                //         SpinalInfo(s"dataOut assign end for ${name}, cursor: ${cursors("out")}, bitWidth: ${outputBitWidth - cursors("out")}")
                //     }
                // }
                // else {
                //     // write 0 in dataIn until end of dataIn width
                //     if (dataIn != null) {
                //         dataIn(cursors("in"), (inputBitWidth - cursors("in")) bits) := 0
                //         SpinalInfo(s"dataIn assign end for ${name}, cursor: ${cursors("in")}, bitWidth: ${inputBitWidth - cursors("in")}")
                //     }
                // }

                callback(dat)
            }
        }
    }
    
    override def asMaster(): Unit = {
        isMaster = true
        out(discriminant)
        in(dataIn)
        out(dataOut)
        out(dataDirectionLess)
    }

    override def asSlave(): Unit = {
        isMaster = false
        in(discriminant)
        out(dataIn)
        in(dataOut)
        in(dataDirectionLess)
    }
}



