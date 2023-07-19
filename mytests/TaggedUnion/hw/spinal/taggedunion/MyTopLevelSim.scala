package taggedunion

import spinal.core._
import spinal.core.sim._

object MyTopLevelSim extends App {
    Config.sim.compile(MemoryController()).doSim { dut =>
        // Fork a process to generate the reset and the clock on the dut
        dut.clockDomain.forkStimulus(period = 10)

        // var modelState = 0
        // for (idx <- 0 to 99) {
        //     // Drive the dut inputs with random values
        //     dut.io.rwPort.discriminant #= 0
        //     dut.io.rwPort.dataOutMaster.randomize()

        //     // Wait a rising edge on the clock
        //     dut.clockDomain.waitRisingEdge()


        //     assert(dut.io.rwPort.dataInMaster.toBigInt == (dut.io.rwPort.dataOutMaster.toBigInt * 2 + 1), "dataInMaster != dataOutMaster ## 1")
        //     // println(s"dataIn: ${dut.io.rwPort.dataInMaster.toBigInt}")
        //     // println(s"dataOut: ${dut.io.rwPort.dataOutMaster.toBigInt}")
        // }
    }
}
