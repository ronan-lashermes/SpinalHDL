package spinal.lib

import spinal.core._

object BufferCC {
  def apply[T <: Data](input: T, init: => T = null, bufferDepth: Option[Int] = None, randBoot : Boolean = false): T = {
    val c = new BufferCC(input, init, bufferDepth, randBoot)
    c.setCompositeName(input, "buffercc", true)
    c.io.dataIn := input

    val ret = cloneOf(c.io.dataOut)
    ret := c.io.dataOut
    return ret
  }

  def apply[T <: Data](input: T, init: => T, bufferDepth: Int, randBoot : Boolean): T =
    apply(input, init, Some(bufferDepth), randBoot)
  def apply[T <: Data](input: T, init: => T, bufferDepth: Int): T =
    apply(input, init, Some(bufferDepth))
  def apply[T <: Data](input: T, bufferDepth: Int, randBoot : Boolean): T =
    apply(input, null.asInstanceOf[T], Some(bufferDepth), randBoot)
  def apply[T <: Data](input: T, bufferDepth: Int): T =
    apply(input, null.asInstanceOf[T], Some(bufferDepth))



  val defaultDepth = ScopeProperty(2)
  def defaultDepthOptioned(cd : ClockDomain, option : Option[Int]) : Int = {
    option match {
      case Some(x) => return x
      case None =>
    }
    ClockDomain.current.getTags().foreach{
      case t : CrossClockBufferDepth => {
        return t.value
      }
      case _ =>
    }
    ClockDomain.current.clock.getTags().foreach{
      case t : CrossClockBufferDepth => {
        return t.value
      }
      case _ =>
    }
    return defaultDepth.get
  }
}

class BufferCC[T <: Data](val dataType: T, init :  => T, val bufferDepth: Option[Int], val  randBoot : Boolean = false) extends Component {
  def getInit() : T = init
  val finalBufferDepth = BufferCC.defaultDepthOptioned(ClockDomain.current, bufferDepth)
  assert(finalBufferDepth >= 1)

  val io = new Bundle {
    val dataIn = in(cloneOf(dataType))
    val dataOut = out(cloneOf(dataType))
  }

  val buffers = Vec(Reg(dataType, init),finalBufferDepth)
  if(randBoot) buffers.foreach(_.randBoot())

  buffers(0) := io.dataIn
  buffers(0).addTag(crossClockDomain)
  for (i <- 1 until finalBufferDepth) {
    buffers(i) := buffers(i - 1)
    buffers(i).addTag(crossClockBuffer)
  }

  io.dataOut := buffers.last
}


object PulseCCByToggle {
  def apply(input: Bool,
            clockIn: ClockDomain,
            clockOut: ClockDomain): Bool = {
    val c = new PulseCCByToggle(clockIn,clockOut)
    c.io.pulseIn := input
    return c.io.pulseOut
  }
}


class PulseCCByToggle(clockIn: ClockDomain, clockOut: ClockDomain, withOutputBufferedReset : Boolean = ClockDomain.crossClockBufferPushToPopResetGen.get) extends Component{
  val io = new Bundle{
    val pulseIn = in Bool()
    val pulseOut = out Bool()
  }

  val inArea = clockIn on new Area {
    val target = RegInit(False) toggleWhen(io.pulseIn)
  }


  val finalOutputClock = clockOut.withOptionalBufferedResetFrom(withOutputBufferedReset)(clockIn)
  val outArea = finalOutputClock on new Area {
    val target = BufferCC(inArea.target, False)

    io.pulseOut := target.edge(False)
  }
}


object ResetCtrl{
  /**
   *
   * @param input Input reset signal
   * @param clockDomain ClockDomain which will use the synchronised reset.
   * @param inputPolarity (HIGH/LOW)
   * @param outputPolarity (HIGH/LOW)
   * @param bufferDepth Number of register stages used to avoid metastability (default=2)
   * @param inputSync Active high, will set reset high in a sync way
   * @return Filtred Bool which is asynchronously asserted synchronously deaserted
   */
  def asyncAssertSyncDeassert(input : Bool,
                              clockDomain : ClockDomain,
                              inputPolarity : Polarity = HIGH,
                              outputPolarity : Polarity = null, //null => inferred from the clockDomain
                              bufferDepth : Option[Int] = None,
                              inputSync : Bool = False) : Bool = {
    val samplerCD = clockDomain.copy(
      reset = input,
      config = clockDomain.config.copy(
        resetKind = ASYNC,
        resetActiveLevel = inputPolarity
      )
    )

    val solvedOutputPolarity = if(outputPolarity == null) clockDomain.config.resetActiveLevel else outputPolarity
    samplerCD(BufferCC(
      input       = (if(solvedOutputPolarity == HIGH) False else True) ^ inputSync,
      init        = (if(solvedOutputPolarity == HIGH) True  else False),
      bufferDepth = bufferDepth)
    )
  }

  /**
   * As asyncAssertSyncDeassert but directly drive the clockDomain reset with the synchronised signal.
   */
  def asyncAssertSyncDeassertDrive(input : Bool,
                                   clockDomain : ClockDomain,
                                   inputPolarity : Polarity = HIGH,
                                   outputPolarity : Polarity = null, //null => inferred from the clockDomain
                                   bufferDepth : Option[Int] = None) : Unit = clockDomain.reset := asyncAssertSyncDeassert(
    input = input ,
    clockDomain = clockDomain ,
    inputPolarity = inputPolarity ,
    outputPolarity = outputPolarity ,
    bufferDepth = bufferDepth
  )

  //Return a new clockdomain which use all the properties of clockCd but use as reset source a syncronized value from resetCd
  def asyncAssertSyncDeassertCreateCd(resetCd : ClockDomain,
                                      clockCd : ClockDomain = ClockDomain.current,
                                      bufferDepth : Option[Int] = None) : ClockDomain = {
    clockCd.copy(
      clock = clockCd.clock,
      reset = ResetCtrl.asyncAssertSyncDeassert(
        input = resetCd.reset,
        clockDomain = clockCd,
        inputPolarity = resetCd.config.resetActiveLevel,
        outputPolarity = clockCd.config.resetActiveLevel,
        bufferDepth = bufferDepth
      ).setCompositeName(resetCd.reset, "syncronized", true)
    )
  }
}
