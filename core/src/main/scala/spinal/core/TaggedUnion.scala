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

import spinal.core.internals._
import spinal.idslplugin.Location

import scala.collection.mutable.ArrayBuffer

class TaggedUnion() extends Nameable with ScalaLocated {
    
    var elements = ArrayBuffer[(String, HardType[_])]()

    def newData[T <: Data](data: T): Unit = {
        newData(null, data)
    }

    def newData[T <: Data](name: String, data: HardType[T]): Unit = {
        elements += name -> data
        SpinalInfo(s"newData: $name, width ${data.getBitsWidth}")
    }

    def newLabel(name: String): Unit = {
        elements += name -> Bits(0 bits)
    }

    def craft(): TaggedUnionCraft = {
        val ret = new TaggedUnionCraft(this)
        ret
    }

    def apply() = craft()
}

class TaggedUnionCraft(taggedunion: TaggedUnion) extends Bundle {

    // discriminant is the index of the element to choose among possible elements
    val discriminant = Bits(log2Up(taggedunion.elements.length) bits)
    // data is a Bits that can contain any of the possible elements
    val data = Bits(taggedunion.elements.map(_._2.getBitsWidth).max bits)

    SpinalInfo(s"data width: ${data.getWidth}")
}

