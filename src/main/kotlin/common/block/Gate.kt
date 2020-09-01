package net.dblsaiko.rswires.common.block

import net.dblsaiko.hctm.common.block.WireUtils
import net.dblsaiko.hctm.common.init.Items
import net.dblsaiko.hctm.common.wire.BlockPartProvider
import net.dblsaiko.hctm.common.wire.getWireNetworkState
import net.dblsaiko.rswires.common.util.reverseAdjustRotation
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.block.ShapeContext
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.StateManager.Builder
import net.minecraft.state.property.IntProperty
import net.minecraft.state.property.Properties
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResult.PASS
import net.minecraft.util.ActionResult.SUCCESS
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Direction.Axis.X
import net.minecraft.util.math.Direction.Axis.Y
import net.minecraft.util.math.Direction.Axis.Z
import net.minecraft.util.shape.VoxelShape
import net.minecraft.world.BlockView
import net.minecraft.world.World
import net.minecraft.world.WorldAccess
import net.minecraft.world.WorldView

abstract class GateBlock(settings: AbstractBlock.Settings) : Block(settings), BlockPartProvider {

  override fun appendProperties(builder: Builder<Block, BlockState>) {
    super.appendProperties(builder)
    builder.add(Properties.FACING)
    builder.add(GateProperties.ROTATION)
  }

  override fun prepare(state: BlockState, world: WorldAccess, pos: BlockPos, flags: Int, maxUpdateDepth: Int) {
    if (!world.isClient && world is ServerWorld)
      world.getWireNetworkState().controller.onBlockChanged(world, pos, state)
  }

  override fun getPlacementState(ctx: ItemPlacementContext): BlockState {
    val facing = ctx.side.opposite
    val rotation = ctx.player?.rotationVector?.let {
      val axis = facing.axis
      val edge = Direction.getFacing(
        if (axis == X) 0.0 else it.x,
        if (axis == Y) 0.0 else it.y,
        if (axis == Z) 0.0 else it.z
      )
      reverseAdjustRotation(facing, edge)
    } ?: 0
    return defaultState
      .with(Properties.FACING, facing)
      .with(GateProperties.ROTATION, rotation)
  }

  fun getSide(state: BlockState) = state[Properties.FACING]

  override fun getStateForNeighborUpdate(state: BlockState, facing: Direction, neighborState: BlockState, world: WorldAccess, pos: BlockPos, neighborPos: BlockPos): BlockState {
    return if (state.canPlaceAt(world, pos)) state else Blocks.AIR.defaultState
  }

  override fun canPlaceAt(state: BlockState, world: WorldView, pos: BlockPos): Boolean {
    val dir = state[Properties.FACING]
    val neighborPos = pos.offset(dir)
    return world.getBlockState(neighborPos).isSideSolidFullSquare(world, neighborPos, dir.opposite)
  }

  override fun calcBlockBreakingDelta(state: BlockState, player: PlayerEntity, world: BlockView, pos: BlockPos): Float {
    val f = state.getHardness(world, pos)
    return if (f == -1.0f) {
      0.0f
    } else {
      1.0f / f / 100.0f
    }
  }

  override fun onUse(state: BlockState, world: World, pos: BlockPos, player: PlayerEntity, hand: Hand, hit: BlockHitResult): ActionResult {
    if (player.getStackInHand(hand).item == Items.SCREWDRIVER) {
      world.setBlockState(pos, world.getBlockState(pos).cycle(GateProperties.ROTATION))
      return SUCCESS
    }
    return PASS
  }

  override fun getCollisionShape(state: BlockState, view: BlockView, pos: BlockPos, ePos: ShapeContext): VoxelShape {
    return COLLISION.getValue(state[Properties.FACING])
  }

  override fun getOutlineShape(state: BlockState, view: BlockView, pos: BlockPos, ePos: ShapeContext): VoxelShape {
    return COLLISION.getValue(state[Properties.FACING])
  }

  companion object {
    val COLLISION = WireUtils.generateShapes(2 / 16.0)
  }

}

object GateProperties {

  val ROTATION = IntProperty.of("rotation", 0, 3)

}