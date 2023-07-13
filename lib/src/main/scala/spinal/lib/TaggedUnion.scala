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
    
    
//     def oneof(callback: PartialFunction[Data, Unit]): Unit = {
//         for ((name, dataType) <- elements) {
//             when(discriminant === elements.indexWhere(_._1 == name)) {
//                 val dat = cloneOf(dataType)
//                 dat.assignFromBits(data)
//                 callback(dat)
//             }
//         }
//     }
// }

class TaggedUnionCraft(elements: ArrayBuffer[(String, _ <: Data)]) extends Bundle {
    val discriminant = UInt(log2Up(elements.length) bits)

    val inputBitWidth = elements.flatMap(_._2.flatten.filter(_.isInput)).map(_.getBitsWidth).reduceOption(Math.max).getOrElse(0)
    val outputBitWidth = elements.flatMap(_._2.flatten.filter(_.isOutput)).map(_.getBitsWidth).reduceOption(Math.max).getOrElse(0)
    val inoutBitWidth = elements.flatMap(_._2.flatten.filter(_.isInOut)).map(_.getBitsWidth).reduceOption(Math.max).getOrElse(0)
    val directionlessBitWidth = elements.flatMap(_._2.flatten.filter(_.isDirectionLess)).map(_.getBitsWidth).reduceOption(Math.max).getOrElse(0)

    val dataIn = (inputBitWidth > 0) generate (in Bits(inputBitWidth bits))
    val dataOut = (outputBitWidth > 0) generate (out Bits(outputBitWidth bits))
    val dataInOut = (inoutBitWidth > 0) generate (inout Bits(inoutBitWidth bits))
    val dataDirectionLess = (directionlessBitWidth > 0) generate Bits(directionlessBitWidth bits)

    def oneof(callback: PartialFunction[Data, Unit]): Unit = {
        var cursors = Map[String, Int](
            "in" -> 0,
            "out" -> 0,
            "inout" -> 0,
            "directionless" -> 0
        )

        for ((name, dataType) <- elements) {
            when(discriminant === elements.indexWhere(_._1 == name)) {
                val dat = cloneOf(dataType)
                val flatData = dat.flatten
                for (data <- flatData) {
                    val direction = data.getDirection()
                    val bitWidth = data.getBitsWidth
                    direction match {
                        case d if d.isInput && dataIn.isDefined => 
                            data.assignFromBits(dataIn.get(cursors("in"), cursors("in") + bitWidth - 1))
                            cursors = cursors.updated("in", cursors("in") + bitWidth)
                        case d if d.isOutput && dataOut.isDefined =>
                            data.assignFromBits(dataOut.get(cursors("out"), cursors("out") + bitWidth - 1))
                            cursors = cursors.updated("out", cursors("out") + bitWidth)
                        case d if d.isInOut && dataInOut.isDefined => 
                            data.assignFromBits(dataInOut.get(cursors("inout"), cursors("inout") + bitWidth - 1))
                            cursors = cursors.updated("inout", cursors("inout") + bitWidth)
                        case d if d.isDirectionLess && dataDirectionLess.isDefined =>
                            data.assignFromBits(dataDirectionLess.get(cursors("directionless"), cursors("directionless") + bitWidth - 1))
                            cursors = cursors.updated("directionless", cursors("directionless") + bitWidth)
                        case _ =>
                    }
                }
                callback(dat)
            }
        }
    }
}



