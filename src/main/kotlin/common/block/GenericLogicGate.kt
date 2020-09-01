package net.dblsaiko.rswires.common.block

import net.dblsaiko.hctm.common.block.WireUtils
import net.dblsaiko.hctm.common.wire.ConnectionDiscoverers
import net.dblsaiko.hctm.common.wire.ConnectionFilter
import net.dblsaiko.hctm.common.wire.NetNode
import net.dblsaiko.hctm.common.wire.NodeView
import net.dblsaiko.hctm.common.wire.PartExt
import net.dblsaiko.hctm.common.wire.WirePartExtType
import net.dblsaiko.hctm.common.wire.find
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

class GenericLogicGateBlock(settings: AbstractBlock.Settings, private val logic: GateLogic) : GateBlock(settings) {

  init {
    defaultState = defaultState
      .with(LogicGateProperties.OUTPUT_POWERED, GateOutputState.OFF)
      .with(LogicGateProperties.LEFT_POWERED, GateInputState.OFF)
      .with(LogicGateProperties.BACK_POWERED, GateInputState.OFF)
      .with(LogicGateProperties.RIGHT_POWERED, GateInputState.OFF)
  }

  override fun appendProperties(builder: Builder<Block, BlockState>) {
    super.appendProperties(builder)
    builder.add(LogicGateProperties.OUTPUT_POWERED)
    builder.add(LogicGateProperties.LEFT_POWERED)
    builder.add(LogicGateProperties.BACK_POWERED)
    builder.add(LogicGateProperties.RIGHT_POWERED)
  }

  override fun scheduledTick(state: BlockState, world: ServerWorld, pos: BlockPos, random: Random) {
    val output = logic.update(
      state[LogicGateProperties.LEFT_POWERED],
      state[LogicGateProperties.BACK_POWERED],
      state[LogicGateProperties.RIGHT_POWERED]
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
    return setOf(
      LogicGatePartExt(side, rotation, GateSide.FRONT),
      LogicGatePartExt(side, rotation, GateSide.LEFT),
      LogicGatePartExt(side, rotation, GateSide.BACK),
      LogicGatePartExt(side, rotation, GateSide.RIGHT)
    )
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

  companion object {
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
    return when (gateSide) {
      FRONT -> world.getBlockState(self.data.pos)[LogicGateProperties.OUTPUT_POWERED] != GateOutputState.OFF
      LEFT -> world.getBlockState(self.data.pos)[LogicGateProperties.LEFT_POWERED] == GateInputState.ON
      BACK -> world.getBlockState(self.data.pos)[LogicGateProperties.BACK_POWERED] == GateInputState.ON
      RIGHT -> world.getBlockState(self.data.pos)[LogicGateProperties.RIGHT_POWERED] == GateInputState.ON
    }
  }

  override fun setState(world: World, self: NetNode, state: Boolean) {
    val blockState = world.getBlockState(self.data.pos)

    when (gateSide) {
      FRONT -> {
        if (blockState[LogicGateProperties.OUTPUT_POWERED] != GateOutputState.ON) {
          world.setBlockState(self.data.pos, blockState.with(LogicGateProperties.OUTPUT_POWERED, if (state) GateOutputState.INPUT else GateOutputState.OFF))
        }
      }
      LEFT -> {
        if (blockState[LogicGateProperties.LEFT_POWERED] != GateInputState.DISABLED) {
          world.setBlockState(self.data.pos, blockState.with(LogicGateProperties.LEFT_POWERED, if (state) GateInputState.ON else GateInputState.OFF))
        }
      }
      BACK -> {
        if (blockState[LogicGateProperties.BACK_POWERED] != GateInputState.DISABLED) {
          world.setBlockState(self.data.pos, blockState.with(LogicGateProperties.BACK_POWERED, if (state) GateInputState.ON else GateInputState.OFF))
        }
      }
      RIGHT -> {
        if (blockState[LogicGateProperties.RIGHT_POWERED] != GateInputState.DISABLED) {
          world.setBlockState(self.data.pos, blockState.with(LogicGateProperties.RIGHT_POWERED, if (state) GateInputState.ON else GateInputState.OFF))
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
        val d = adjustRotation(side, rotation, gateSide.direction())
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

  override fun toTag(): Tag {
    return ByteTag.of(((gateSide.direction()) or (side.id shl 2) or (rotation shl 6)).toByte())
  }

}