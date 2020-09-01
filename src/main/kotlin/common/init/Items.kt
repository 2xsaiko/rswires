package net.dblsaiko.rswires.common.init

import net.dblsaiko.hctm.common.block.BaseWireItem
import net.dblsaiko.hctm.common.util.delegatedNotNull
import net.dblsaiko.hctm.common.util.flatten
import net.dblsaiko.rswires.MOD_ID
import net.minecraft.block.Block
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.Item.Settings
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import kotlin.properties.ReadOnlyProperty

object Items {

  private val tasks = mutableListOf<() -> Unit>()

  private val SETTINGS = Item.Settings().group(ItemGroups.ALL)

  val RED_ALLOY_WIRE by create("red_alloy_wire", BaseWireItem(Blocks.RED_ALLOY_WIRE, SETTINGS))
  val INSULATED_WIRES by Blocks.INSULATED_WIRES.mapValues { (color, block) -> create("${color.getName()}_insulated_wire", BaseWireItem(block, SETTINGS)) }.flatten()
  val UNCOLORED_BUNDLED_CABLE by create("bundled_cable", BaseWireItem(Blocks.UNCOLORED_BUNDLED_CABLE, SETTINGS))
  val COLORED_BUNDLED_CABLES by Blocks.COLORED_BUNDLED_CABLES.mapValues { (color, block) -> create("${color.getName()}_bundled_cable", BaseWireItem(block, SETTINGS)) }.flatten()

  val NULL_CELL by create("null_cell", BlockItem(Blocks.NULL_CELL, SETTINGS))
  val AND_GATE by create("and_gate", BlockItem(Blocks.AND_GATE, SETTINGS))
  val OR_GATE by create("or_gate", BlockItem(Blocks.OR_GATE, SETTINGS))
  val NAND_GATE by create("nand_gate", BlockItem(Blocks.NAND_GATE, SETTINGS))
  val NOR_GATE by create("nor_gate", BlockItem(Blocks.NOR_GATE, SETTINGS))
  val XOR_GATE by create("xor_gate", BlockItem(Blocks.XOR_GATE, SETTINGS))
  val XNOR_GATE by create("xnor_gate", BlockItem(Blocks.XNOR_GATE, SETTINGS))

  val RED_ALLOY_COMPOUND by create("red_alloy_compound", Item(SETTINGS))
  val RED_ALLOY_INGOT by create("red_alloy_ingot", Item(SETTINGS))

  private fun <T : Block> create(name: String, block: T): ReadOnlyProperty<Items, BlockItem> {
    return create(name, BlockItem(block, Settings().group(ItemGroup.REDSTONE)))
  }

  private fun <T : Item> create(name: String, item: T): ReadOnlyProperty<Items, T> {
    var regItem: T? = null
    tasks += { regItem = Registry.register(Registry.ITEM, Identifier(MOD_ID, name), item) }
    return delegatedNotNull { regItem }
  }

  internal fun register() {
    tasks.forEach { it() }
    tasks.clear()
  }

}