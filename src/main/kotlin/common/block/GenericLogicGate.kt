package net.dblsaiko.rswires.common.block

import net.dblsaiko.hctm.common.block.WireUtils
import net.dblsaiko.hctm.common.wire.ConnectionDiscoverers
import net.dblsaiko.hctm.common.wire.ConnectionFilter
import net.dblsaiko.hctm.common.wire.NetNode
import net.dblsaiko.hctm.common.wire.NodeView
import net.dblsaiko.hctm.common.wire.PartExt
import net.dblsaiko.hctm.common.wire.WirePartExtType
import net.dblsaiko.hctm.common.wire.find
import net.dblsaiko.rswires.common.block.GateInputState.DISABLED
import net.dblsaiko.rswires.common.block.GateInputState.OFF
import net.dblsaiko.rswires.common.block.GateInputState.ON
import net.dblsaiko.rswires.common.block.GateSide.BACK
import net.dblsaiko.rswires.common.block.GateSide.FRONT
import net.dblsaiko.rswires.common.block.GateSide.LEFT
import net.dblsaiko.rswires.common.block.GateSide.RIGHT
import net.dblsaiko.rswires.common.util.adjustRotation
import net.dblsaiko.rswires.common.util.rotate
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.ShapeContext
import net.minecraft.nbt.ByteTag
import net.minecraft.nbt.Tag
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.StateManager.Builder
import net.minecraft.state.property.EnumProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.shape.VoxelShape
import net.minecraft.util.shape.VoxelShapes
import net.minecraft.world.BlockView
import net.minecraft.world.World
import java.util.*

class GenericLogicGateBlock private constructor(settings: AbstractBlock.Settings, val logic: GateLogic) : GateBlock(settings) {

  init {
    val defaultOutput = if (logic.update(GateInputState.OFF, GateInputState.OFF, GateInputState.OFF))
      GateOutputState.ON else GateOutputState.OFF

    var newDefaultState = defaultState
      .with(LogicGateProperties.OUTPUT_POWERED, defaultOutput)
    if (logic.hasLeftInput()) {
      newDefaultState = newDefaultState.with(LogicGateProperties.LEFT_POWERED, GateInputState.OFF)
    }
    if (logic.hasBackInput()) {
      newDefaultState = newDefaultState.with(LogicGateProperties.BACK_POWERED, GateInputState.OFF)
    }
    if (logic.hasRightInput()) {
      newDefaultState = newDefaultState.with(LogicGateProperties.RIGHT_POWERED, GateInputState.OFF)
    }

    defaultState = newDefaultState
    tempLogic = null
  }

  override fun appendProperties(builder: Builder<Block, BlockState>) {
    super.appendProperties(builder)
    builder.add(LogicGateProperties.OUTPUT_POWERED)
    val logic = tempLogic ?: error("tempLogic is null? Either a race condition or block was not constructed using create()")
    if (logic.hasLeftInput()) builder.add(LogicGateProperties.LEFT_POWERED)
    if (logic.hasBackInput()) builder.add(LogicGateProperties.BACK_POWERED)
    if (logic.hasRightInput()) builder.add(LogicGateProperties.RIGHT_POWERED)
  }

  override fun neighborUpdate(state: BlockState, world: World, pos: BlockPos, block: Block, neighborPos: BlockPos, moved: Boolean) {
    if (world is ServerWorld) {
      RedstoneWireUtils.scheduleUpdate(world, pos)
    }
  }

  override fun scheduledTick(state: BlockState, world: ServerWorld, pos: BlockPos, random: Random) {
    val output = logic.update(state.getLeftInput(), state.getBackInput(), state.getRightInput()
    )

    if (!output && state[LogicGateProperties.OUTPUT_POWERED] == GateOutputState.ON) {
      world.setBlockState(pos, state.with(LogicGateProperties.OUTPUT_POWERED, GateOutputState.OFF))
      RedstoneWireUtils.scheduleUpdate(world, pos)
    } else if (output && state[LogicGateProperties.OUTPUT_POWERED] != GateOutputState.ON) {
      world.setBlockState(pos, state.with(LogicGateProperties.OUTPUT_POWERED, GateOutputState.ON))
      RedstoneWireUtils.scheduleUpdate(world, pos)
    }
  }

  override fun getPartsInBlock(world: World, pos: BlockPos, state: BlockState): Set<PartExt> {
    val side = getSide(state)
    val rotation = state[GateProperties.ROTATION]
    val parts = mutableSetOf<LogicGatePartExt>()
    parts += LogicGatePartExt(side, rotation, GateSide.FRONT)
    if (logic.hasLeftInput()) parts += LogicGatePartExt(side, rotation, GateSide.LEFT)
    if (logic.hasBackInput()) parts += LogicGatePartExt(side, rotation, GateSide.BACK)
    if (logic.hasRightInput()) parts += LogicGatePartExt(side, rotation, GateSide.RIGHT)
    return parts
  }

  override fun createExtFromTag(tag: Tag): PartExt? {
    val data = (tag as? ByteTag)?.int ?: return null
    val gateSide = GateSide.fromEdge(data and 0b11, 0)
    val side = Direction.byId(data shr 2 and 0b111)
    val rotation = data shr 6
    return LogicGatePartExt(side, rotation, gateSide)
  }

  override fun getOutlineShape(state: BlockState, view: BlockView, pos: BlockPos, ePos: ShapeContext): VoxelShape {
    return SELECTION_BOXES.getValue(state[Properties.FACING])
  }

  override fun getCullingShape(state: BlockState, view: BlockView, pos: BlockPos): VoxelShape {
    return CULL_BOX.getValue(state[Properties.FACING])[state[GateProperties.ROTATION]]
  }

  fun BlockState.getLeftInput(): GateInputState =
    if (logic.hasLeftInput()) this[LogicGateProperties.LEFT_POWERED]
    else GateInputState.DISABLED

  fun BlockState.getBackInput(): GateInputState =
    if (logic.hasBackInput()) this[LogicGateProperties.BACK_POWERED]
    else GateInputState.DISABLED

  fun BlockState.getRightInput(): GateInputState =
    if (logic.hasRightInput()) this[LogicGateProperties.RIGHT_POWERED]
    else GateInputState.DISABLED

  fun BlockState.getLeftInput(fallback: Boolean) = when (this.getLeftInput()) {
    OFF -> false
    ON -> true
    DISABLED -> fallback
  }

  fun BlockState.getBackInput(fallback: Boolean) = when (this.getBackInput()) {
    OFF -> false
    ON -> true
    DISABLED -> fallback
  }

  fun BlockState.getRightInput(fallback: Boolean) = when (this.getRightInput()) {
    OFF -> false
    ON -> true
    DISABLED -> fallback
  }

  companion object {
    private var tempLogic: GateLogic? = null

    @JvmStatic
    fun create(settings: AbstractBlock.Settings, logic: GateLogic): GenericLogicGateBlock {
      tempLogic = logic
      return GenericLogicGateBlock(settings, logic)
    }

    val SELECTION_BOXES = WireUtils.generateShapes(2 / 16.0)

    val CULL_BOX =
      VoxelShapes.cuboid(0.0, 0.0, 0.0, 1.0, 2 / 16.0, 1.0).let { box ->
        Direction.values().asIterable().associateWith { face -> Array(4) { rotation -> box.rotate(face, rotation) } }
      }.let(::EnumMap)
  }

}

object LogicGateProperties {
  val OUTPUT_POWERED = EnumProperty.of("output", GateOutputState::class.java)!!
  val LEFT_POWERED = EnumProperty.of("left", GateInputState::class.java)!!
  val BACK_POWERED = EnumProperty.of("back", GateInputState::class.java)!!
  val RIGHT_POWERED = EnumProperty.of("right", GateInputState::class.java)!!
}

enum class GateOutputState : StringIdentifiable {
  OFF,
  ON,
  INPUT;

  override fun asString(): String {
    return when (this) {
      OFF -> "off"
      ON -> "on"
      INPUT -> "input"
    }
  }
}

enum class GateInputState : StringIdentifiable {
  OFF,
  ON,
  DISABLED;

  override fun asString(): String {
    return when (this) {
      OFF -> "off"
      ON -> "on"
      DISABLED -> "disabled"
    }
  }
}

enum class GateSide {
  FRONT,
  LEFT,
  BACK,
  RIGHT;

  fun direction(): Int {
    return when (this) {
      FRONT -> 0
      RIGHT -> 1
      BACK -> 2
      LEFT -> 3
    }
  }

  companion object {
    fun fromEdge(rotation: Int, targetOut: Int): GateSide {
      return when ((rotation + targetOut) % 4) {
        0 -> FRONT
        1 -> RIGHT
        2 -> BACK
        3 -> LEFT
        else -> error("unreachable")
      }
    }
  }

}

data class LogicGatePartExt(override val side: Direction, val rotation: Int, val gateSide: GateSide) : PartExt, WirePartExtType, PartRedstoneCarrier {
  override val type = RedstoneWireType.RedAlloy

  override fun getState(world: World, self: NetNode): Boolean {
    val state = world.getBlockState(self.data.pos)
    with(state.block as GenericLogicGateBlock) {
      return when (gateSide) {
        FRONT -> state[LogicGateProperties.OUTPUT_POWERED] != GateOutputState.OFF
        LEFT -> state.getLeftInput(false)
        BACK -> state.getBackInput(false)
        RIGHT -> state.getRightInput(false)
      }
    }
  }

  override fun setState(world: World, self: NetNode, state: Boolean) {
    val blockState = world.getBlockState(self.data.pos)

    with (blockState.block as GenericLogicGateBlock) {
      when (gateSide) {
        FRONT -> {
          if (blockState[LogicGateProperties.OUTPUT_POWERED] != GateOutputState.ON) {
            world.setBlockState(self.data.pos, blockState.with(LogicGateProperties.OUTPUT_POWERED, if (state) GateOutputState.INPUT else GateOutputState.OFF))
          }
        }
        LEFT -> {
          if (logic.hasLeftInput() && blockState.getLeftInput() != GateInputState.DISABLED) {
            world.setBlockState(self.data.pos, blockState.with(LogicGateProperties.LEFT_POWERED, if (state) GateInputState.ON else GateInputState.OFF))
          }
        }
        BACK -> {
          if (logic.hasBackInput() && blockState.getBackInput() != GateInputState.DISABLED) {
            world.setBlockState(self.data.pos, blockState.with(LogicGateProperties.BACK_POWERED, if (state) GateInputState.ON else GateInputState.OFF))
          }
        }
        RIGHT -> {
          if (logic.hasRightInput() && blockState.getRightInput() != GateInputState.DISABLED) {
            world.setBlockState(self.data.pos, blockState.with(LogicGateProperties.RIGHT_POWERED, if (state) GateInputState.ON else GateInputState.OFF))
          }
        }
      }
    }

    world.blockTickScheduler.schedule(self.data.pos, blockState.block, 1)
  }

  override fun getInput(world: World, self: NetNode): Boolean {
    val blockState = world.getBlockState(self.data.pos)

    return when (gateSide) {
      FRONT -> blockState[LogicGateProperties.OUTPUT_POWERED] == GateOutputState.ON
      LEFT, BACK, RIGHT -> {
        val d = adjustRotation(side, rotation, gateSide.direction()).opposite
        world.getEmittedRedstonePower(self.data.pos.offset(d), d) != 0
      }
    }
  }

  override fun tryConnect(self: NetNode, world: ServerWorld, pos: BlockPos, nv: NodeView): Set<NetNode> {
    val direction = adjustRotation(side, rotation, gateSide.direction())
    return find(ConnectionDiscoverers.WIRE, RedstoneCarrierFilter and ConnectionFilter { self, other ->
      self.data.pos.subtract(other.data.pos)
        .let { Direction.fromVector(it.x, it.y, it.z) }
        ?.let { it == direction }
      ?: false
    }, self, world, pos, nv)
  }

  override fun canConnectAt(world: BlockView, pos: BlockPos, edge: Direction): Boolean {
    val d = adjustRotation(side, rotation, gateSide.direction())
    return edge == d.opposite
  }

  override fun onChanged(self: NetNode, world: ServerWorld, pos: BlockPos) {
    RedstoneWireUtils.scheduleUpdate(world, pos)
  }

  override fun toTag(): Tag {
    return ByteTag.of(((gateSide.direction()) or (side.id shl 2) or (rotation shl 6)).toByte())
  }

}