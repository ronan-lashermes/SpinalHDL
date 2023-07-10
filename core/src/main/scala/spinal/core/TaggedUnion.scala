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

// Importing the necessary packages
import scala.collection.mutable.ArrayBuffer
import spinal.core.internals._
import spinal.idslplugin.{Location, ValCallback}

import scala.collection.mutable


/**
 * Class TaggedUnion represents a composite data structure that consists of multiple named signals, where only one signal can be active at a time. 
 * This is a key abstraction in SpinalHDL, commonly used to represent hardware data structures, buses, interfaces, etc.
 */
class TaggedUnion extends EitherData with Nameable with ValCallbackRec {

    // 'hardType' is a variable that holds the 'HardType' instance, which can be used as a factory for creating 
    // new instances of this taggedUnion. The 'HardType' is a way of storing the data type or the blueprint of this
    // taggedUnion so that it can be used to produce similar structured taggedUnions. It is used in the 'clone' method
    // to create a new instance of the same structure.
    var hardtype: HardType[_] = null

    /**
    * Overriding the clone method to create a new TaggedUnion instance with the same properties.
    */
    override def clone: TaggedUnion = {
        if (hardtype != null) {
            val ret = hardtype().asInstanceOf[this.type]
            ret.hardtype = hardtype
            return ret
        }
        super.clone.asInstanceOf[TaggedUnion]
    }

    /** 
    * A method to assign the values of this TaggedUnion's signals from another TaggedUnion by matching names. 
    */
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

    /**
    * A method to assign the values of this TaggedUnion's signals from another TaggedUnion by matching names.
    * Unlike assignAllByName, this will not raise an error if some names are not found in the source TaggedUnion.
    */
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

    /**
    * A generic method that accepts a function 'f' as parameter and applies it to each pair of corresponding elements in 'this' and 'that' TaggedUnions.
    */
    def taggedUnionAssign(that : TaggedUnion)(f : (Data, Data) => Unit): Unit ={
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

    /**
    * Overriding the assignFromImpl method from MultiData. 
    * It checks if 'that' is a TaggedUnion and of the same final class as 'this', and calls 'taggedUnionAssign' to assign values.
    * If 'that' is not a TaggedUnion, it throws an Exception.
    */
    private[core] override def assignFromImpl(that: AnyRef, target: AnyRef, kind: AnyRef)(implicit loc: Location): Unit = {
        that match {
        case that: TaggedUnion =>
            if (!this.getClass.isAssignableFrom(that.getClass)) {
                // If 'that' is not of the same class type as 'this', it raises an error.
                SpinalError("TaggedUnions must have the same final class to be assigned. Either use assignByName or assignSomeByName at \n" + ScalaLocated.long)
            } else {
                // If 'that' is a TaggedUnion of the same class type as 'this', it calls the taggedUnionAssign method.
                // This method iterates over the elements in 'this' and 'that' TaggedUnions and assigns values from 'that' to 'this'.
                taggedUnionAssign(that) { (to, from) => 
                    to.compositAssignFrom(from,to,kind)
                }
            }
        case _ => SpinalError(s"A tagged union can't be assigned by something else than a tagged union ($this <= $that)")
        }
    }


    // `elementsCache` is an ArrayBuffer of Tuple2 containing a String and a Data. 
    // This array buffer is used to store all elements of the tagged union with their associated names. 
    // Each element in the array buffer is a pair, where the first item is the name of the element 
    // and the second item is the Data object that represents the element itself.
    var elementsCache = ArrayBuffer[(String, Data)]()

    // The `valCallbackRec` method is called each time a value is added to the tagged union.
    // This method is mainly responsible for updating `elementsCache` and setting the parent of the new item to `this` (the current tagged union).
    // It also sets a partial name for the new item if certain conditions are met.
    override def valCallbackRec(ref: Any, name: String): Unit = ref match {
        case ref : Data => {
            SpinalInfo(s"!!!!!!!!!!!! valCallbackRec $ref $name")
            // Add the new item to `elementsCache`
            elementsCache += name -> ref
            // Set the parent of the new item to `this` (the current tagged union)
            ref.parent = this
            // If certain conditions are met (as defined by `OwnableRef.proposal(ref, this)`), 
            // set a partial name for the new item
            if(OwnableRef.proposal(ref, this)) ref.setPartialName(name, Nameable.DATAMODEL_WEAK)
        }
        // If `ref` is not an instance of `Data`, do nothing
        case ref =>
    }

    // The `elements` method returns the `elementsCache`, which contains all elements of the tagged union 
    // along with their associated names.
    override def elements: ArrayBuffer[(String, Data)] = {
        SpinalInfo(s"!!!!!!!!!!!! elements $elementsCache")
        elementsCache
    } 

    // The `rejectOlder` method is defined as always returning true. 
    private[core] def rejectOlder = true

    // The `getTypeString` method returns the simple name of the class of the current object. 
    def getTypeString = getClass.getSimpleName

    // The `toString` method returns a string representation of the object. 
    // It contains the path of the component, the name of the tagged union, and the simple name of the class. 
    override def toString(): String = s"${component.getPath() + "/" + this.getDisplayName()} : $getTypeString"

}

class TaggedUnionCase extends TaggedUnion {
    private[core] override def rejectOlder = false
}
