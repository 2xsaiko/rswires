package net.dblsaiko.rswires.common.init

import net.dblsaiko.hctm.common.util.flatten
import net.dblsaiko.hctm.init.BlockRegistry
import net.dblsaiko.rswires.MOD_ID
import net.dblsaiko.rswires.common.block.BundledCableBlock
import net.dblsaiko.rswires.common.block.InsulatedWireBlock
import net.dblsaiko.rswires.common.block.RedAlloyWireBlock
import net.fabricmc.fabric.api.`object`.builder.v1.block.FabricBlockSettings
import net.minecraft.block.Material
import net.minecraft.util.DyeColor

class Blocks {
    private val reg = BlockRegistry(MOD_ID)

    private val wireSettings = FabricBlockSettings.of(Material.STONE)
        .noCollision()
        .strength(0.05f, 0.05f)

    val redAlloyWireObject = this.reg.create("red_alloy_wire", RedAlloyWireBlock(this.wireSettings))
    val insulatedWireObjects = DyeColor.values().associate { it to this.reg.create("${it.getName()}_insulated_wire", InsulatedWireBlock(this.wireSettings, it)) }
    val uncoloredBundledCableObject = this.reg.create("bundled_cable", BundledCableBlock(this.wireSettings, null))
    val coloredBundledCableObjects = DyeColor.values().associate { it to this.reg.create("${it.getName()}_bundled_cable", BundledCableBlock(this.wireSettings, it)) }

    val redAlloyWire by this.redAlloyWireObject
    val insulatedWires by this.insulatedWireObjects.flatten()
    val uncoloredBundledCable by this.uncoloredBundledCableObject
    val coloredBundledCables by this.coloredBundledCableObjects.flatten()

    fun getInsulatedWire(color: DyeColor): InsulatedWireBlock {
        return this.insulatedWires.getValue(color)
    }

    fun getBundledCable(color: DyeColor): BundledCableBlock {
        return this.coloredBundledCables.getValue(color)
    }

    fun register() {
        this.reg.register()
    }
}