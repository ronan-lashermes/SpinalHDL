# RFC: Hardware sum types (aka TaggedUnion)

## Introduction 

The modern programming trend is to leverage the type system to ensure program correctness whenever possible.
With SpinalHDL, this trend can be used for hardware type.

The principle P1 can be stated.

> The type of a value must designate its purpose in the most specific way.

A simple example for a signal that carry the information if a request is a read or a write one.

```scala
val read_write = Bool() //incorrect, does not follow P1

when(read_write === True) { // read or write ?
    //...
}
```

A better way to define this variable while satisfying P1 would be

```scala
object ReadWrite extends SpinalEnum {
  val read, write = newElement()
}

val read_write = ReadWrite()

when(read_write === read) { //unambiguous
    //...
}
```

This is not about correctness, both implementations are. But the second one leverages the type system to remove ambiguity, thus lowering the verification work.


## What are sum types / tagged unions

A product type is a composite type that includes a type A **and** a type B. This is your regular SpinalHDL’s Bundle.
A sum type is a composite type that includes a type A **or** a type B.

The most famous implementation of sum types is probably in rust
```rust
enum ReadWriteRequest {
    Read(ReadRequest),
    Write(WriteRequest)
}
```
Our rust example defines a *ReadWriteRequest* type that can either be a *Read* with type *ReadRequest* or a *Write* with *WriteRequest*, but not both at the same time.

A hardware sum type would be the equivalent : for the I/O of a Read/Write port, the type would specify that only one of the two is valid at a given time.
For a data in registers, the type would specify that only one of the two is valid.

The goal is to have the type system describes more precisely the hardware and the associated correct semantics.
There are several possible hardware way to implement a sum type, we will discuss this below.

## Implementation ideas (not working for now)

### Usage

Let’s start with how we would use such a thing in SpinalHDL.
This description will include sleights of hand, to simplify the discussion.


```scala
case class ReadRequest() extends Bundle {
    val add = UInt(8 bits)
}

case class WriteRequest() extends Bundle {
    val add = UInt(8 bits)
    val data = Bits(32 bits)
}

// like a bundle, but we only have one the two fields at a given time
object ReadWriteRequest extends TaggedUnion  {
    val read = ReadRequest()
    val write = WriteRequest()
}

val request = ReadWriteRequest()

// set the value
when(event) {
    request.chooseOne("write") {
        case write: WritePort => {
            // set write signals
            write.data := //...
        }
        case _ => {
        }
    }
}

// read the value
request.oneof {
    case read: ReadRequest => {
        io.address := read.add
    }
    case write: WriteRequest => {
        io.address := write.add
    }
    case _ => {
    }
}


```

### Hardware layouts

There are several possible hardware layouts for a sum type: intersecting, disjoint, and with different enum encoding.
Basically, we need elements to store the values and a selector to choose which one is valid.

#### Intersecting

The intersecting layout uses the same hardware (wires or registers) for all the possible values of the sum type.
This layout minimizes the hardware footprint associated with the hardware element, but requires to use a mux to write the value.

#### Disjoint

Values are not stored in the same elements (e.g. read and write requests are stored in different wires/registers).
Need more hardware for the values, but no mux is required to write the value.

#### Enum encoding

The selector signal can be encoded in different ways. This problem has been addressed in SpinalEnum, and the same solutions can be used here.


### Problems

This approach has several unsolved problems:

1. In *oneof*, the type is not enough to select a variant. We could have a TaggedUnion where all values have the same type. We could bundle in a String, but this is ugly.
2. In *chooseOne*, we select the variant with a String. This is ugly.
3. Last but certainly not least, I left the question of the signal directions. The internal signals of the TaggedUnion should be grouped by direction to determine the size of the signals.
But the direction may be changed after the creation of the TaggedUnion, implying a modification of the size of the signals.

## Questions

So this first try is unsatisfying for several reasons. But I would love to have feedback at this step :

- Do you have ideas to improve the API and its ergonomics ? Getting rid of the String in *chooseOne* ?
- How to modify the size of the TaggedUnion internal signals when the direction changes ?


## Current implementation for reference, not working (this version does not compile)

```scala
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

    // There are two dimensions to master/slave here:
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


    def chooseOne(name: String)(callback: Data => Unit): Unit = {

        val elementData = elements.find(_._1 == name) match {
            case Some((_, data)) => data
            case None => throw new IllegalArgumentException(s"No element found with name: $name, legal names are: ${elements.map(_._1)}")
        }
        
        //set selector to index of element
        selector := elements.indexWhere(_._1 == name)

        var cursors = Map[String, Int](
            "in" -> 0,
            "out" -> 0,
            "directionless" -> 0
        )

        val dat = cloneOf(elementData)
        dat.assignDontCare()
        dat.flattenForeach { data =>
            val direction = if (data.getDirection == null) { dat.getDirection } else { data.getDirection }
            val bitWidth = data.getBitsWidth
            data.setAsDirectionLess()
            
            direction match {
                case `in` if (dataInMaster != null) => 
                    if (isMaster) {
                        data.assignFromBits(dataInMaster(cursors("in"), bitWidth bits))
                    }
                    else {
                        dataInMaster(cursors("in"), bitWidth bits) := data.asBits
                    }
                    
                    cursors = cursors.updated("in", cursors("in") + bitWidth)
                case `out` if (dataOutMaster != null) =>
                    if (isMaster) {
                        dataOutMaster(cursors("out"), bitWidth bits) := data.asBits
                    }
                    else {
                        data.assignFromBits(dataOutMaster(cursors("out"), bitWidth bits))
                    }

                    cursors = cursors.updated("out", cursors("out") + bitWidth)
                case _ if (dataDirectionLess != null) =>
                    data.assignFromBits(dataDirectionLess(cursors("directionless"), bitWidth bits))
                    cursors = cursors.updated("directionless", cursors("directionless") + bitWidth)
                case _ =>
            }
        }

        callback(dat)
    }

    def oneof(callback: (String, Data) => Unit): Unit = {

        for ((name, dataType) <- elements) {

            var cursors = Map[String, Int](
                "in" -> 0,
                "out" -> 0,
                "directionless" -> 0
            )


            when(selector === elements.indexWhere(_._1 == name)) {
                val dat = cloneOf(dataType)
                dat.flattenForeach { data =>
                    
                    val direction = if (data.getDirection == null) { dat.getDirection } else { data.getDirection }
                    val bitWidth = data.getBitsWidth
                    data.setAsDirectionLess()
                    
                    direction match {
                        case `in` if (dataInMaster != null) => 
                            if (isMaster) {
                                data.assignFromBits(dataInMaster(cursors("in"), bitWidth bits))
                            }
                            else {
                                dataInMaster(cursors("in"), bitWidth bits) := data.asBits
                            }
                            
                            cursors = cursors.updated("in", cursors("in") + bitWidth)
                        case `out` if (dataOutMaster != null) =>
                            if (isMaster) {
                                dataOutMaster(cursors("out"), bitWidth bits) := data.asBits
                            }
                            else {
                                data.assignFromBits(dataOutMaster(cursors("out"), bitWidth bits))
                            }

                            cursors = cursors.updated("out", cursors("out") + bitWidth)
                        case _ if (dataDirectionLess != null) =>
                            data.assignFromBits(dataDirectionLess(cursors("directionless"), bitWidth bits))
                            cursors = cursors.updated("directionless", cursors("directionless") + bitWidth)
                        case _ =>
                    }

                     
                }

                callback(name, dat)
            }
        }
    }
    
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



```