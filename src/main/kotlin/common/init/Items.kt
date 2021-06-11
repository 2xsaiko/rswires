package net.dblsaiko.rswires.common.init

import net.dblsaiko.hctm.common.block.BaseWireItem
import net.dblsaiko.hctm.common.util.flatten
import net.dblsaiko.hctm.init.ItemRegistry
import net.dblsaiko.rswires.MOD_ID
import net.dblsaiko.rswires.RSWires
import net.minecraft.item.Item
import net.minecraft.item.Item.Settings

class Items(blocks: Blocks, itemGroups: ItemGroups) {
    private val reg = ItemRegistry(MOD_ID)

    val redAlloyWire by this.reg.createThen("red_alloy_wire") { BaseWireItem(blocks.redAlloyWire, Settings().group(itemGroups.all)) }
    val insulatedWires by RSWires.blocks.insulatedWireObjects.mapValues { (color, block) -> this.reg.createThen("${color.getName()}_insulated_wire") { BaseWireItem(block.get(), Settings().group(itemGroups.all)) } }.flatten()
    val uncoloredBundledCable by this.reg.createThen("bundled_cable") { BaseWireItem(blocks.uncoloredBundledCable, Settings().group(itemGroups.all)) }
    val coloredBundledCables by RSWires.blocks.coloredBundledCableObjects.mapValues { (color, block) -> this.reg.createThen("${color.getName()}_bundled_cable") { BaseWireItem(block.get(), Settings().group(itemGroups.all)) } }.flatten()

    val redAlloyCompound by this.reg.create("red_alloy_compound", Item(Item.Settings().group(itemGroups.all)))
    val redAlloyIngot by this.reg.create("red_alloy_ingot", Item(Item.Settings().group(itemGroups.all)))

    fun register() {
        this.reg.register()
    }
}