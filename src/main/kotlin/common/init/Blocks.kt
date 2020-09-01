package net.dblsaiko.rswires.common.init

import net.dblsaiko.hctm.common.util.delegatedNotNull
import net.dblsaiko.hctm.common.util.flatten
import net.dblsaiko.rswires.MOD_ID
import net.dblsaiko.rswires.common.block.AndGateLogic
import net.dblsaiko.rswires.common.block.BundledCableBlock
import net.dblsaiko.rswires.common.block.GenericLogicGateBlock
import net.dblsaiko.rswires.common.block.InsulatedWireBlock
import net.dblsaiko.rswires.common.block.NandGateLogic
import net.dblsaiko.rswires.common.block.NorGateLogic
import net.dblsaiko.rswires.common.block.NullCellBlock
import net.dblsaiko.rswires.common.block.OrGateLogic
import net.dblsaiko.rswires.common.block.RedAlloyWireBlock
import net.dblsaiko.rswires.common.block.XnorGateLogic
import net.dblsaiko.rswires.common.block.XorGateLogic
import net.fabricmc.fabric.api.block.FabricBlockSettings
import net.minecraft.block.Block
import net.minecraft.block.Material
import net.minecraft.util.DyeColor
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import kotlin.properties.ReadOnlyProperty

object Blocks {

  private val tasks = mutableListOf<() -> Unit>()

  private val WIRE_SETTINGS = FabricBlockSettings.of(Material.STONE)
    .breakByHand(true)
    .noCollision()
    .strength(0.05f, 0.05f)
    .build()

  private val GATE_SETTINGS = FabricBlockSettings.of(Material.STONE)
    .breakByHand(true)
    .strength(0.05f, 0.05f)
    .build()

  val RED_ALLOY_WIRE by create("red_alloy_wire", RedAlloyWireBlock(WIRE_SETTINGS))
  val INSULATED_WIRES by DyeColor.values().associate { it to create("${it.getName()}_insulated_wire", InsulatedWireBlock(WIRE_SETTINGS, it)) }.flatten()
  val UNCOLORED_BUNDLED_CABLE by create("bundled_cable", BundledCableBlock(WIRE_SETTINGS, null))
  val COLORED_BUNDLED_CABLES by DyeColor.values().associate { it to create("${it.getName()}_bundled_cable", BundledCableBlock(WIRE_SETTINGS, it)) }.flatten()

  val NULL_CELL by create("null_cell", NullCellBlock(GATE_SETTINGS))
  val AND_GATE by create("and_gate", GenericLogicGateBlock.create(GATE_SETTINGS, AndGateLogic))
  val OR_GATE by create("or_gate", GenericLogicGateBlock.create(GATE_SETTINGS, OrGateLogic))
  val NAND_GATE by create("nand_gate", GenericLogicGateBlock.create(GATE_SETTINGS, NandGateLogic))
  val NOR_GATE by create("nor_gate", GenericLogicGateBlock.create(GATE_SETTINGS, NorGateLogic))
  val XOR_GATE by create("xor_gate", GenericLogicGateBlock.create(GATE_SETTINGS, XorGateLogic))
  val XNOR_GATE by create("xnor_gate", GenericLogicGateBlock.create(GATE_SETTINGS, XnorGateLogic))

  private fun <T : Block> create(name: String, block: T): ReadOnlyProperty<Blocks, T> {
    var regBlock: T? = null
    tasks += { regBlock = Registry.register(Registry.BLOCK, Identifier(MOD_ID, name), block) }
    return delegatedNotNull { regBlock }
  }

  internal fun register() {
    tasks.forEach { it() }
    tasks.clear()
  }

}