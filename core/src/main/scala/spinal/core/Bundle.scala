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
  * The 'ValCallbackRec' trait, extending the 'ValCallback' trait, provides methods for recursively handling different Scala types.
  * It plays a pivotal role in SpinalHDL by managing data structure representation in hardware description.
  */
trait ValCallbackRec extends ValCallback {

    /**
    * This method applies the 'valCallbackOn' method to a given 'ref' if it's not null and not already contained in 'refs'.
    * It then adds the 'ref' to 'refs' to avoid duplications in the future.
    */
    private def applyValCallbackOn(ref: Any, name: String, refs: mutable.Set[Any]): Unit = {
        if (ref != null && !refs.contains(ref)) {
            refs += ref
            handleType(ref, name, refs)
            valCallbackRec(ref, name)
        }
    }

    /**
    * This method distinguishes between different types of 'ref' and applies the appropriate function for each type.
    */
    private def handleType(ref: Any, name: String, refs: mutable.Set[Any]): Unit = {
        ref match {
            case range : Range => // handle Range type
            case vec: Vec[_]   => // handle Vec type
            case seq: Seq[_]   => handleSequence(seq, name, refs)
            case arr: Array[_] => handleSequence(arr, name, refs)
            case set: Set[_]   => handleSequence(set, name, refs)
            case set: mutable.LinkedHashSet[_] => handleSequence(set, name, refs)
            case map: mutable.LinkedHashMap[_, _] => handleLinkedHashMap(map, name, refs)
            case prod : Product if !name.contains("$") => handleTuple(prod, name, refs)
            case Some(x) => applyValCallbackOn(x, name, refs) // handle Option type
            case _ => // handle all other types
        }
    }

    /**
    * This method applies 'valCallbackOn' on each element in a sequence data structure (like `Seq`, `Array`, `Set`, `LinkedHashSet`) with an updated name.
    */
    private def handleSequence(seq: Iterable[_], name: String, refs: mutable.Set[Any]): Unit =
        for ((e, i) <- seq.zipWithIndex) applyValCallbackOn(e, name + "_" + i, refs)

    /**
    * This method applies 'valCallbackOn' on each value in a LinkedHashMap with an updated name.
    */
    private def handleLinkedHashMap(map: mutable.LinkedHashMap[_, _], name: String, refs: mutable.Set[Any]): Unit =
        for ((e, i) <- map.zipWithIndex) applyValCallbackOn(e._2, name + "_" + i, refs)

    /**
    * This method applies 'valCallbackOn' on each element in a tuple with an updated name.
    */
    private def handleTuple(tuple: Product, name: String, refs: mutable.Set[Any]): Unit =
        for ((e, i) <- tuple.productIterator.zipWithIndex) applyValCallbackOn(e, name + "_" + i, refs)
    
    def valCallbackRec(ref: Any, name: String): Unit // abstract method to be implemented in a subclass

    /**
    * Overriding the valCallback method from the ValCallback trait.
    * This method initiates the recursion by calling 'applyValCallbackOn'.
    */
    override def valCallback[T](ref: T, name: String): T = {
        val refs = mutable.Set[Any]()
        applyValCallbackOn(ref, name, refs)
        ref
    }
}


/**
  * The Bundle is a composite type that defines a group of named signals (of any SpinalHDL basic type) under a single name.
  * The Bundle can be used to model data structures, buses and interfaces.
  *
  * @example {{{
  *     val cmd = new Bundle{
  *       val init   = in Bool()
  *       val start  = in Bool()
  *       val result = out Bits(32 bits)
  *     }
  * }}}
  *
  * @see  [[http://spinalhdl.github.io/SpinalDoc/spinal/core/types/Bundle Bundle Documentation]]
  */

/**
 * Class Bundle represents a composite data structure that consists of multiple named signals. 
 * This is a key abstraction in SpinalHDL, commonly used to represent hardware data structures, buses, interfaces, etc.
 */
class Bundle extends MultiData with Nameable with ValCallbackRec {

    // 'hardType' is a variable that holds the 'HardType' instance, which can be used as a factory for creating 
    // new instances of this bundle. The 'HardType' is a way of storing the data type or the blueprint of this
    // bundle so that it can be used to produce similar structured bundles. It is used in the 'clone' method
    // to create a new instance of the same structure.
    var hardtype: HardType[_] = null

    /**
    * Overriding the clone method to create a new Bundle instance with the same properties.
    */
    override def clone: Bundle = {
        if (hardtype != null) {
            val ret = hardtype().asInstanceOf[this.type]
            ret.hardtype = hardtype
            return ret
        }
        super.clone.asInstanceOf[Bundle]
    }

    /** 
    * A method to assign the values of this Bundle's signals from another Bundle by matching names. 
    */
    def assignAllByName(that: Bundle): Unit = {
        for ((name, element) <- elements) {
            val other = that.find(name)
            if (other == null)
                LocatedPendingError(s"Bundle assignment is not complete. Missing $name")
            else element match {
                case b: Bundle => b.assignAllByName(other.asInstanceOf[Bundle])
                case _         => element := other
            }
        }
    }

    /**
    * A method to assign the values of this Bundle's signals from another Bundle by matching names.
    * Unlike assignAllByName, this will not raise an error if some names are not found in the source Bundle.
    */
    def assignSomeByName(that: Bundle): Unit = {
        for ((name, element) <- elements) {
            val other = that.find(name)
            if (other != null) {
                element match {
                case b: Bundle => b.assignSomeByName(other.asInstanceOf[Bundle])
                case _         => element := other
                }
            }
        }
    }

    /**
    * A generic method that accepts a function 'f' as parameter and applies it to each pair of corresponding elements in 'this' and 'that' Bundles.
    */
    def bundleAssign(that : Bundle)(f : (Data, Data) => Unit): Unit ={
        for ((name, element) <- elements) {
            val other = that.find(name)
            if (other == null) {
                LocatedPendingError(s"Bundle assignment is not complete. $this need '$name' but $that doesn't provide it.")
            }
            else {
                f(element, other)
            }
        }
    }

    /**
    * Overriding the assignFromImpl method from MultiData. 
    * It checks if 'that' is a Bundle and of the same final class as 'this', and calls 'bundleAssign' to assign values.
    * If 'that' is not a Bundle, it throws an Exception.
    */
    private[core] override def assignFromImpl(that: AnyRef, target: AnyRef, kind: AnyRef)(implicit loc: Location): Unit = {
        that match {
        case that: Bundle =>
            if (!this.getClass.isAssignableFrom(that.getClass)) {
                // If 'that' is not of the same class type as 'this', it raises an error.
                SpinalError("Bundles must have the same final class to be assigned. Either use assignByName or assignSomeByName at \n" + ScalaLocated.long)
            } else {
                // If 'that' is a Bundle of the same class type as 'this', it calls the bundleAssign method.
                // This method iterates over the elements in 'this' and 'that' Bundles and assigns values from 'that' to 'this'.
                bundleAssign(that) { (to, from) => 
                    to.assignFrom(from.asInstanceOf[to.This])
                }
            }
        case _ => SpinalError(s"A bundle can't be assigned by something else than a bundle ($this <= $that)")
        }
    }


    // `elementsCache` is an ArrayBuffer of Tuple2 containing a String and a Data. 
    // This array buffer is used to store all elements of the bundle with their associated names. 
    // Each element in the array buffer is a pair, where the first item is the name of the element 
    // and the second item is the Data object that represents the element itself.
    var elementsCache = ArrayBuffer[(String, Data)]()

    // The `valCallbackRec` method is called each time a value is added to the bundle.
    // This method is mainly responsible for updating `elementsCache` and setting the parent of the new item to `this` (the current bundle).
    // It also sets a partial name for the new item if certain conditions are met.
    override def valCallbackRec(ref: Any, name: String): Unit = ref match {
        case ref : Data => {
            // Add the new item to `elementsCache`
            elementsCache += name -> ref
            // Set the parent of the new item to `this` (the current bundle)
            ref.parent = this
            // If certain conditions are met (as defined by `OwnableRef.proposal(ref, this)`), 
            // set a partial name for the new item
            if(OwnableRef.proposal(ref, this)) ref.setPartialName(name, Nameable.DATAMODEL_WEAK)
        }
        // If `ref` is not an instance of `Data`, do nothing
        case ref =>
    }

    // The `elements` method returns the `elementsCache`, which contains all elements of the bundle 
    // along with their associated names.
    override def elements: ArrayBuffer[(String, Data)] = elementsCache

    // The `rejectOlder` method is defined as always returning true. The purpose of this method 
    // depends on its usage elsewhere in the code, which is not shown here. Generally, it could be 
    // used to control the behavior of the bundle when dealing with older or outdated data.
    private[core] def rejectOlder = true

    // The `getTypeString` method returns the simple name of the class of the current object. 
    // This is mainly used for debugging purposes, as it allows you to easily identify the class 
    // of an object at runtime.
    def getTypeString = getClass.getSimpleName

    // The `toString` method returns a string representation of the object. 
    // It contains the path of the component, the name of the bundle, and the simple name of the class. 
    // Again, this is mainly used for debugging purposes.
    override def toString(): String = s"${component.getPath() + "/" + this.getDisplayName()} : $getTypeString"

}

class BundleCase extends Bundle {
    private[core] override def rejectOlder = false
}
