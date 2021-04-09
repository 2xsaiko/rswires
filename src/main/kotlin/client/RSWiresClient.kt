package net.dblsaiko.rswires.client

import net.dblsaiko.hctm.client.render.model.CacheKey
import net.dblsaiko.hctm.client.render.model.ModelWrapperHandler
import net.dblsaiko.hctm.client.render.model.UnbakedWireModel
import net.dblsaiko.hctm.client.render.model.WireModelParts
import net.dblsaiko.rswires.MOD_ID
import net.dblsaiko.rswires.RSWires
import net.dblsaiko.rswires.client.render.model.GateModel
import net.dblsaiko.rswires.common.init.Blocks
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry
import net.fabricmc.fabric.api.client.model.ModelVariantProvider
import net.fabricmc.fabric.api.renderer.v1.RendererAccess
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.model.UnbakedModel
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object RSWiresClient : ClientModInitializer {

  override fun onInitializeClient() {
    BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getCutout(),
      Blocks.AND_GATE,
      Blocks.OR_GATE,
      Blocks.NAND_GATE,
      Blocks.NOR_GATE,
      Blocks.XOR_GATE,
      Blocks.XNOR_GATE)

    ModelLoadingRegistry.INSTANCE.registerVariantProvider {
      val r = RendererAccess.INSTANCE.renderer

      if (r != null) {
        val modelStore = ConcurrentHashMap<CacheKey, WireModelParts>()

        val redAlloyOffModel = UnbakedWireModel(r, Identifier(MOD_ID, "block/red_alloy_wire/off"), 0.125f, 0.125f, 32.0f, modelStore)
        val redAlloyOnModel = UnbakedWireModel(r, Identifier(MOD_ID, "block/red_alloy_wire/on"), 0.125f, 0.125f, 32.0f, modelStore)

        val insulatedWireOffModel = DyeColor.values().associate { it to UnbakedWireModel(r, Identifier(MOD_ID, "block/insulated_wire/${it.getName()}/off"), 0.25f, 0.1875f, 32.0f, modelStore) }
        val insulatedWireOnModel = DyeColor.values().associate { it to UnbakedWireModel(r, Identifier(MOD_ID, "block/insulated_wire/${it.getName()}/on"), 0.25f, 0.1875f, 32.0f, modelStore) }

        val colorBundledCableModel = DyeColor.values().associate { it to UnbakedWireModel(r, Identifier(MOD_ID, "block/bundled_cable/${it.getName()}"), 0.375f, 0.25f, 32.0f, modelStore) }
        val plainBundledCableModel = UnbakedWireModel(r, Identifier(MOD_ID, "block/bundled_cable/none"), 0.375f, 0.25f, 32.0f, modelStore)

      ModelVariantProvider { modelId, _ ->
          val props = modelId.variant.split(",")
          when (val id = Identifier(modelId.namespace, modelId.path)) {
            Registry.BLOCK.getId(Blocks.RED_ALLOY_WIRE) -> {
              if ("powered=false" in props) redAlloyOffModel
              else redAlloyOnModel
            }
            in Blocks.INSULATED_WIRES.values.asSequence().map(Registry.BLOCK::getId) -> {
              val (color, _) = Blocks.INSULATED_WIRES.entries.first { (_, block) -> id == Registry.BLOCK.getId(block) }
              if ("powered=false" in props) insulatedWireOffModel.getValue(color)
              else insulatedWireOnModel.getValue(color)
            }
            Registry.BLOCK.getId(Blocks.UNCOLORED_BUNDLED_CABLE) -> {
              plainBundledCableModel
            }
            in Blocks.COLORED_BUNDLED_CABLES.values.asSequence().map(Registry.BLOCK::getId) -> {
              val (color, _) = Blocks.COLORED_BUNDLED_CABLES.entries.first { (_, block) -> id == Registry.BLOCK.getId(block) }
              colorBundledCableModel.getValue(color)
            }
            else -> null
          }
        }
      } else {
        RSWires.logger.error("Could not find Renderer API implementation. Rendering for RSWires wires will not be available.")
        ModelVariantProvider { _, _ -> null }
      }
    }

    ModelWrapperHandler.register {
      val map = IdentityHashMap<UnbakedModel, UnbakedModel>();

      { state, model ->
        when (state.block) {
          Blocks.NULL_CELL,
          Blocks.AND_GATE,
          Blocks.OR_GATE,
          Blocks.NAND_GATE,
          Blocks.NOR_GATE,
          Blocks.XOR_GATE,
          Blocks.XNOR_GATE ->
            map.computeIfAbsent(model, ::GateModel)
          else -> model
        }
      }
    }
  }

}