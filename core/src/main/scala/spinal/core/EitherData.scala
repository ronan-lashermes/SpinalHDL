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

import spinal.core.internals.Operator
import spinal.idslplugin.Location

import scala.collection.mutable.ArrayBuffer
import scala.collection.Seq

/**
  * Base class for data structures that contain multiple elements, with a OR relation: only one element can be set at a time.
  * These are used to group together related pieces of data.
  */
abstract class EitherData extends Data {

    // This method should be overridden by subclasses to provide the actual elements
    // Each element is a pair of a string (the name of the element) and a Data (the actual data)
    // The elements are stored in an ArrayBuffer, which allows for efficient addition and removal of elements
    def elements: ArrayBuffer[(String, Data)]

    // This method allows for adding a tag to this EitherData and all of its elements
    // Tags are used to attach metadata to the data
    // The method returns the current instance (this) to allow for method chaining
    override def addTag[T <: SpinalTag](spinalTag: T): this.type = {
        // Add the tag to this EitherData
        super.addTag(spinalTag)
        // Add the tag to all elements of this EitherData
        elements.foreach(_._2.addTag(spinalTag))
        // Return the current instance to allow for method chaining
        this
    }

    /**
    * Find an element by its name.
    * @param name The name of the element to find.
    * @return The found element, or null if no element with the given name exists.
    */
    def find(name: String): Data = {
        // Use the find method to find the first element with the given name
        // If no such element exists, find will return None, and getOrElse will return null
        val temp = elements.find((tuple) => tuple._1 == name).getOrElse(null)
        // If temp is null, return null
        if (temp == null) {
            return null
        }
        // Otherwise, return the second element of the tuple, which is the actual data
        temp._2
    }

    /**
    * Convert this EitherData to a Bits instance.
    * @return A Bits instance representing the same data as this EitherData.
    */
    override def asBits: Bits = {
        var ret: Bits = null
        // Iterate over all elements
        for ((_, e) <- elements) {
            // If ret is null, set it to the bits representation of the current element
            // Otherwise, concatenate the bits representation of the current element to ret
            if (ret == null) {
                ret = e.asBits
            } else {
                ret = e.asBits ## ret
            }
        }
        // If ret is still null after the loop, set it to an empty Bits instance
        if (ret == null) {
            ret = Bits(0 bits)
        }
        ret
    }

    /**
    * Get the total width in bits of this EitherData.
    * @return The total width in bits.
    */
    override def getBitsWidth: Int = {
        var accumulateWidth = 0
        // Iterate over all elements
        for ((_, e) <- elements) {
        val width = e.getBitsWidth
        // If the width of the current element is -1, throw an error
        if (width == -1) {
            SpinalError("Can't return bits width")
        } else {
            // Otherwise, add the width of the current element to the total width
            accumulateWidth += width
        }
        }
        accumulateWidth
    }


 
      /**
    * Set the direction of this EitherData and all its elements to input.
    * @return The current instance with its direction set to input.
    */
    override def asInput(): this.type = {
        // Set the direction of this EitherData to input
        super.asInput()
        // Set the direction of all elements to input
        elements.foreach(_._2.asInput())
        // Return the current instance to allow for method chaining
        this
    }

    /**
    * Set the direction of this EitherData and all its elements to output.
    * @return The current instance with its direction set to output.
    */
    override def asOutput(): this.type = {
        // Set the direction of this EitherData to output
        super.asOutput()
        // Set the direction of all elements to output
        elements.foreach(_._2.asOutput())
        // Return the current instance to allow for method chaining
        this
    }

    /**
    * Set the direction of this EitherData and all its elements to in/out.
    * @return The current instance with its direction set to in/out.
    */
    override def asInOut(): this.type = {
        // Set the direction of this EitherData to in/out
        super.asInOut()
        // Set the direction of all elements to in/out
        elements.foreach(_._2.asInOut())
        // Return the current instance to allow for method chaining
        this
    }

    /**
    * Copy the direction of another Data to this EitherData and all its elements.
    * @param that The Data to copy the direction from.
    * @return The current instance with its direction copied from the other Data.
    */
    override def copyDirectionOfImpl(that : Data): this.type = {
        // Copy the direction of the other Data to this EitherData
        super.copyDirectionOfImpl(that)
        // Copy the direction of the other Data's elements to this EitherData's elements
        (elements, that.asInstanceOf[EitherData].elements).zipped.foreach{case (t, s) => t._2.copyDirectionOfImpl(s._2)}
        // Return the current instance to allow for method chaining
        this
    }


      /**
    * Set the direction of this EitherData and all its elements to directionless.
    * @return The current instance with its direction set to directionless.
    */
    override def setAsDirectionLess(): this.type = {
        // Set the direction of this EitherData to directionless
        super.setAsDirectionLess()
        // Set the direction of all elements to directionless
        elements.foreach(_._2.setAsDirectionLess())
        // Return the current instance to allow for method chaining
        this
    }

    /**
    * Set the base type of all elements to reg (register).
    * @return The current instance with the base type of all elements set to reg.
    */
    override def setAsReg(): this.type = {
        // Set the base type of all elements to reg
        elements.foreach(_._2.setAsReg())
        // Return the current instance to allow for method chaining
        this
    }

    /**
    * Set the base type of all elements to comb (combinatorial).
    * @return The current instance with the base type of all elements set to comb.
    */
    override def setAsComb(): this.type = {
        // Set the base type of all elements to comb
        elements.foreach(_._2.setAsComb())
        // Return the current instance to allow for method chaining
        this
    }

    /**
    * Freeze this EitherData and all its elements.
    * Freezing prevents further modifications to the data.
    * @return The current instance, frozen.
    */
    override def freeze(): EitherData.this.type = {
        // Freeze all elements
        elements.foreach(_._2.freeze())
        // Return the current instance to allow for method chaining
        this
    }

    /**
    * Unfreeze this EitherData and all its elements.
    * Unfreezing allows further modifications to the data.
    * @return The current instance, unfrozen.
    */
    override def unfreeze(): EitherData.this.type = {
        // Unfreeze all elements
        elements.foreach(_._2.unfreeze())
        // Return the current instance to allow for method chaining
        this
    }


      /**
    * Flatten this EitherData into a sequence of BaseType instances.
    * @return A sequence of BaseType instances.
    */
    def flatten: Seq[BaseType] = {
        // For each element, flatten it into a sequence of BaseType instances
        // Then, concatenate all these sequences into a single sequence
        elements.map(_._2.flatten).foldLeft(List[BaseType]())(_ ++ _)
    }

    /**
    * Apply a function to each BaseType in the flattened version of this EitherData.
    * @param body The function to apply.
    */
    override def flattenForeach(body: (BaseType) => Unit): Unit = {
        // For each element, flatten it and apply the function to each BaseType in the flattened version
        elements.foreach(_._2.flattenForeach(body))
    }

    /**
    * Get the local names of all BaseType instances in the flattened version of this EitherData.
    * @return A sequence of local names.
    */
    override def flattenLocalName: Seq[String] = {
        val result = ArrayBuffer[String]()
        // For each element, get the local names of all BaseType instances in the flattened version
        // Then, prepend the local name of the element to each local name
        for((localName,e) <- elements){
            result ++= e.flattenLocalName.map(name => if(name == "") localName else localName + "_" + name)
        }
        result
        // Alternative implementation using map and reduce:
        // elements.map{case (localName,e) => e.flattenLocalName.map(name => if(name == "") localName else localName + "_" + name)}.reduce(_ ++: _)
    }

    /**
    * Assign values to this EitherData and all its elements from a Bits instance.
    * @param bits The Bits instance to assign values from.
    */
    override def assignFromBits(bits: Bits): Unit = {
        var offset = 0
        // For each element, get its width and assign values from the corresponding bits
        for ((_, e) <- elements) {
            val width = e.getBitsWidth
            e.assignFromBits(bits(offset, width bit))
            offset = offset + width
        }
    }

    /**
    * Assign values to this EitherData and all its elements from a range of a Bits instance.
    * @param bits The Bits instance to assign values from.
    * @param hi The high end of the range.
    * @param lo The low end of the range.
    */
    override def assignFromBits(bits: Bits, hi: Int, lo: Int): Unit = {
        var offset = 0
        var bitsOffset = 0

        // For each element, get its width and assign values from the corresponding range of bits
        // Iterate over each element in the EitherData
        for ((_, e) <- elements) {
            // Get the width in bits of the current element
            val width = e.getBitsWidth
                    
            // Check if the range specified by hi and lo overlaps with the range of bits assigned to the current element
            if (hi >= offset && lo < offset + width) {
                // Calculate the high end of the range within the current element
                // This is either the high end of the overall range minus the offset, or the width of the element minus 1, whichever is smaller
                val high = Math.min(hi-offset,width-1)

                // Calculate the low end of the range within the current element
                // This is either the low end of the overall range minus the offset, or 0, whichever is larger
                val low  = Math.max(lo-offset,0)

                // Calculate the number of bits used from the Bits instance
                // This is the difference between the high and low ends of the range within the current element, plus 1
                val bitUsage = high - low + 1

                // Assign values to the current element from the range of the Bits instance specified by bitsOffset and bitUsage
                e.assignFromBits(bits(bitsOffset,bitUsage bit), high,low)

                // Increase bitsOffset by bitUsage to move to the next range of bits for the next element
                bitsOffset += bitUsage
            }

            // Increase offset by the width of the current element to move to the next range of bits for the next element
            offset = offset + width
        }

    }

      /**
    * Check if this EitherData is equal to another object.
    * @param that The object to compare with.
    * @return A Bool indicating whether this EitherData is equal to the other object.
    */
    private[core] def isEqualTo(that: Any): Bool = {
        that match {
            // If the other object is a EitherData, compare each pair of corresponding elements
            case that: EitherData => {
                val checks = zippedMap(that, _ === _)
                // If there are any checks, return the logical AND of all checks
                // If there are no checks, return True
                if(checks.nonEmpty) {
                    checks.reduce(_ && _) 
                }
                else {
                    True
                }
            }
            // If the other object is not a EitherData, throw an error
            case _ => SpinalError(s"Function isEqualTo is not implemented between $this and $that")
        }
    }

    /**
        * Check if this EitherData is not equal to another object.
        * @param that The object to compare with.
        * @return A Bool indicating whether this EitherData is not equal to the other object.
        */
    private[core] def isNotEqualTo(that: Any): Bool = {
        that match {
            // If the other object is a EitherData, compare each pair of corresponding elements
            case that: EitherData => {
                val checks = zippedMap(that, _ =/= _)
                // If there are any checks, return the logical OR of all checks
                // If there are no checks, return False
                if(checks.nonEmpty) {
                    checks.reduce(_ || _)
                } 
                else {
                    False
                }
            }
            // If the other object is not a EitherData, throw an error
            case _ => SpinalError(s"Function isNotEqualTo is not implemented between $this and $that")
        }
    }


    /**
    * Automatically connect this EitherData to another Data.
    * @param that The Data to connect to.
    * @param loc The location where the autoConnect is called.
    */
    private[core] override def autoConnect(that: Data)(implicit loc: Location): Unit = {
        that match {
            // If the other Data is a EitherData, auto-connect each pair of corresponding elements
            case that: EitherData => zippedMap(that, _ autoConnect _)
            // If the other Data is not a EitherData, throw an error
            case _ => SpinalError(s"Function autoConnect is not implemented between $this and $that")
        }
    }

    /**
    * Get a string representation of all elements in this EitherData.
    * @return A string representation of all elements.
    */
    def elementsString = this.elements.map(_.toString()).reduce(_ + "\n" + _)
    // SpinalInfo(s"elementsString = $elementsString")

    /**
    * Apply a function to each pair of corresponding elements in this EitherData and another EitherData.
    * @param that The other EitherData.
    * @param task The function to apply.
    * @return A sequence of the results of the function.
    */
    private[core] def zippedMap[T](that: EitherData, task: (Data, Data) => T): Seq[T] = {
        // If the other EitherData doesn't have the same number of elements as this EitherData, throw an error
        if (that.elements.length != this.elements.length) {
            SpinalError(s"Can't zip [$this] with [$that]  because they don't have the same number of elements.\nFirst one has :\n${this.elementsString}\nSecond one has :\n${that.elementsString}\n")
        }
        // For each element in this EitherData, find the corresponding element in the other EitherData and apply the function to them
        this.elements.map(x => {
            val (n, e) = x
            val other = that.find(n)
            // If the other EitherData doesn't have an element with the same name as the current element, throw an error
            if (other == null) {
                SpinalError(s"Can't zip [$this] with [$that] because the element named '${n}' is missing in the second one")
            }
            task(e, other)
        })
    }

      /**
    * Get a zero value of this EitherData.
    * @return A new EitherData of the same type as this EitherData, with all elements set to zero.
    */
    override def getZero: this.type = {
        // Create a clone of this EitherData
        val ret = cloneOf(this)
        // Set each element of the clone to zero
        ret.elements.foreach(e => {
            e._2 := e._2.getZero
        })
        // Return the clone
        ret.asInstanceOf[this.type]
    }

    /**
    * Flip the direction of this EitherData and all its elements.
    * @return The current instance with its direction flipped.
    */
    override def flip(): this.type  = {
        // Flip the direction of each element
        for ((_,e) <- elements) {
            e.flip()
        }

        // Flip the direction of this EitherData
        dir match {
            case `in`  => dir = out
            case `out` => dir = in
            case _     => // Do nothing if the direction is not in or out
        }
        // Return the current instance to allow for method chaining
        this
    }

      /**
    * Assign values to unassigned elements of this EitherData from corresponding elements of another EitherData.
    * @param that The other EitherData to assign values from.
    */
    def assignUnassignedByName(that: EitherData): Unit = {
        // For each pair of corresponding elements in this EitherData and the other EitherData
        // If the element in this EitherData is unassigned and its direction is either directionless, output in the current component, or input in the parent component
        // Assign the value of the element in the other EitherData to the element in this EitherData
        this.zipByName(that).filter(!_._1.hasDataAssignment).foreach{
        case (dst, src) if dst.isDirectionLess || dst.isOutput && dst.component == Component.current || dst.isInput && dst.component.parent == Component.current =>
            dst := src
        case _ =>
        }
    }

    /**
    * Pair up elements of this EitherData and another EitherData by name.
    * @param that The other EitherData to pair up with.
    * @param rec An ArrayBuffer to store the pairs in.
    * @return An ArrayBuffer of pairs of corresponding elements.
    */
    def zipByName(that: EitherData, rec : ArrayBuffer[(BaseType, BaseType)] = ArrayBuffer()): ArrayBuffer[(BaseType, BaseType)] = {
        // For each element in this EitherData, find the corresponding element in the other EitherData by name
        // If the element in this EitherData is a EitherData, recursively pair up its elements with the elements of the corresponding EitherData in the other EitherData
        // If the element in this EitherData is a BaseType, add a pair of it and the corresponding BaseType in the other EitherData to the ArrayBuffer
        for ((name, element) <- elements) {
        val other = that.find(name)
        if (other != null) {
            element match {
            case b  : EitherData => b.zipByName(other.asInstanceOf[EitherData], rec)
            case bt : BaseType => rec += (bt -> other.asInstanceOf[BaseType])
            }
        }
        }
        rec
    }

    /**
    * Assign a formal random value to each element of this EitherData.
    * @param kind The kind of random expression to assign.
    */
    override def assignFormalRandom(kind: Operator.Formal.RandomExpKind) = elements.foreach(_._2.assignFormalRandom(kind))

}
