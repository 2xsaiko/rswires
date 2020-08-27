package net.dblsaiko.rswires.common.block

interface GateLogic {

  fun update(left: GateInputState, back: GateInputState, right: GateInputState): Boolean

}

object AndGateLogic : GateLogic {

  override fun update(left: GateInputState, back: GateInputState, right: GateInputState): Boolean {
    return left != GateInputState.OFF && back != GateInputState.OFF && right != GateInputState.OFF
  }

}

object OrGateLogic : GateLogic {

  override fun update(left: GateInputState, back: GateInputState, right: GateInputState): Boolean {
    return left == GateInputState.ON || back == GateInputState.ON || right == GateInputState.ON
  }

}

object NandGateLogic : GateLogic {

  override fun update(left: GateInputState, back: GateInputState, right: GateInputState): Boolean {
    return !(left != GateInputState.OFF && back != GateInputState.OFF && right != GateInputState.OFF)
  }

}

object NorGateLogic : GateLogic {

  override fun update(left: GateInputState, back: GateInputState, right: GateInputState): Boolean {
    return !(left == GateInputState.ON || back == GateInputState.ON || right == GateInputState.ON)
  }

}
