package net.dblsaiko.rswires.common.init

import net.dblsaiko.hctm.common.util.ext.makeStack
import net.dblsaiko.rswires.MOD_ID
import net.dblsaiko.rswires.RSWires
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder
import net.minecraft.item.ItemGroup
import net.minecraft.util.Identifier

class ItemGroups {
    val all: ItemGroup = FabricItemGroupBuilder.create(Identifier(MOD_ID, "all"))
        .icon { RSWires.items.redAlloyWire.makeStack() }
        .build()
}