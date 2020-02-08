package net.dblsaiko.rswires.client.render.model

import com.mojang.datafixers.util.Pair
import net.dblsaiko.rswires.common.block.GateProperties
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
import net.minecraft.block.BlockState
import net.minecraft.client.render.model.BakedModel
import net.minecraft.client.render.model.BakedQuad
import net.minecraft.client.render.model.ModelBakeSettings
import net.minecraft.client.render.model.ModelLoader
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.client.render.model.json.ModelItemPropertyOverrideList
import net.minecraft.client.render.model.json.ModelTransformation
import net.minecraft.client.texture.Sprite
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.item.ItemStack
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Direction.DOWN
import net.minecraft.util.math.Direction.EAST
import net.minecraft.util.math.Direction.NORTH
import net.minecraft.util.math.Direction.SOUTH
import net.minecraft.util.math.Direction.UP
import net.minecraft.util.math.Direction.WEST
import net.minecraft.world.BlockRenderView
import therealfarfetchd.qcommon.croco.Mat4
import therealfarfetchd.qcommon.croco.Vec3
import java.util.*
import java.util.function.Function
import java.util.function.Supplier

class GateModel(val wrapped: UnbakedModel) : UnbakedModel {

  val map = IdentityHashMap<BakedModel, Baked>()

  override fun bake(loader: ModelLoader, textureGetter: Function<SpriteIdentifier, Sprite>, rotationContainer: ModelBakeSettings, modelId: Identifier): BakedModel? {
    return map.computeIfAbsent(wrapped.bake(loader, textureGetter, rotationContainer, modelId) ?: return null, ::Baked)
  }

  override fun getModelDependencies(): Collection<Identifier> {
    return wrapped.modelDependencies
  }

  override fun getTextureDependencies(unbakedModelGetter: Function<Identifier, UnbakedModel>, unresolvedTextureReferences: Set<Pair<String, String>>): Collection<SpriteIdentifier> {
    return wrapped.getTextureDependencies(unbakedModelGetter, unresolvedTextureReferences)
  }

  class Baked(val wrapped: BakedModel) : FabricBakedModel, BakedModel {

    override fun emitBlockQuads(blockView: BlockRenderView, state: BlockState, pos: BlockPos, randomSupplier: Supplier<Random>, context: RenderContext) {
      val side = state[Properties.FACING]
      val rotation = state[GateProperties.Rotation]
      val (mat, rotationMat) = matrixMap.getValue(side)[rotation]

      context.pushTransform { quad ->
        for (idx in 0..3) {
          val newPos = mat.mul(Vec3(quad.posByIndex(idx, 0), quad.posByIndex(idx, 1), quad.posByIndex(idx, 2)))
          quad.pos(idx, newPos.x, newPos.y, newPos.z)
        }
        val vec3 = quad.faceNormal().let { Vec3(it.x, it.y, it.z) }
        for (idx in 0..3) {
          // always (0,0,0) ???
          // val vec3 = Vec3(quad.normalX(idx), quad.normalY(idx), quad.normalZ(idx))
          val newNormal = rotationMat.mul(vec3)
          // FIXME this seems to have no effect
          quad.normal(idx, newNormal.x, newNormal.y, newNormal.z)
        }
        // FIXME this seems to have no effect
        quad.cullFace()?.also {
          quad.cullFace(rotationMat.mul(Vec3.from(it.vector)).let { Direction.getFacing(it.x, it.y, it.z) })
        }
        true
      }
      context.fallbackConsumer().accept(wrapped)
      context.popTransform()
    }

    override fun emitItemQuads(stack: ItemStack, randomSupplier: Supplier<Random>, context: RenderContext) {
      context.fallbackConsumer().accept(wrapped)
    }

    override fun isVanillaAdapter(): Boolean {
      return false
    }

    override fun getItemPropertyOverrides(): ModelItemPropertyOverrideList {
      return wrapped.itemPropertyOverrides
    }

    override fun getQuads(state: BlockState?, face: Direction?, random: Random): List<BakedQuad> {
      return emptyList()
    }

    override fun getSprite(): Sprite {
      return wrapped.sprite
    }

    override fun useAmbientOcclusion(): Boolean {
      return wrapped.useAmbientOcclusion()
    }

    override fun hasDepth(): Boolean {
      return wrapped.hasDepth()
    }

    override fun getTransformation(): ModelTransformation {
      return wrapped.transformation
    }

    override fun isSideLit(): Boolean {
      return wrapped.isSideLit
    }

    override fun isBuiltin(): Boolean {
      return wrapped.isBuiltin
    }

    companion object {
      val matrixMap = Direction.values().asIterable().associateWith { face ->
        Array(4) { rotation ->
          val matrix = getRotationFor(face, rotation)
          val rotMatrix = matrix.toArray().also { it[3] = 0.0f; it[7] = 0.0f; it[11] = 0.0f }.let { Mat4.fromArray(it) }
          matrix to rotMatrix
        }
      }

      fun getRotationFor(face: Direction, rotation: Int): Mat4 {
        val m1 = Mat4.IDENTITY
          .translate(0.5f, 0.5f, 0.5f)

        return when (face) {
          DOWN -> m1
          UP -> m1.rotate(1.0f, 0.0f, 0.0f, 180.0f).rotate(0.0f, 1.0f, 0.0f, 180.0f)
          NORTH -> m1.rotate(1.0f, 0.0f, 0.0f, -90.0f).rotate(0.0f, 1.0f, 0.0f, 90.0f)
          SOUTH -> m1.rotate(0.0f, 1.0f, 0.0f, 180.0f).rotate(1.0f, 0.0f, 0.0f, -90.0f).rotate(0.0f, 1.0f, 0.0f, 90.0f)
          WEST -> m1.rotate(0.0f, 0.0f, 1.0f, 90.0f)
          EAST -> m1.rotate(0.0f, 1.0f, 0.0f, 180.0f).rotate(0.0f, 0.0f, 1.0f, 90.0f)
          else -> m1
        }
          .rotate(0.0f, 1.0f, 0.0f, rotation * 90.0f)
          .translate(-0.5f, -0.5f, -0.5f)
      }

    }

  }

}