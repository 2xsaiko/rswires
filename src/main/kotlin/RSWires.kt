package net.dblsaiko.rswires

import net.dblsaiko.rswires.common.block.RedstoneWireUtils
import net.dblsaiko.rswires.common.init.BlockEntityTypes
import net.dblsaiko.rswires.common.init.Blocks
import net.dblsaiko.rswires.common.init.ItemGroups
import net.dblsaiko.rswires.common.init.Items
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.world.ServerWorld
import org.apache.logging.log4j.LogManager

const val MOD_ID = "rswires"

object RSWires : ModInitializer {
    internal var logger = LogManager.getLogger(MOD_ID)

    var wiresGivePower = true

    val blockEntityTypes = BlockEntityTypes()
    val blocks = Blocks()
    val itemGroups = ItemGroups()
    val items = Items(blocks, itemGroups)

    override fun onInitialize() {
        this.blockEntityTypes.register()
        this.blocks.register()
        this.items.register()

        ServerTickEvents.END_WORLD_TICK.register(ServerTickEvents.EndWorldTick {
            if (it is ServerWorld) {
                RedstoneWireUtils.flushUpdates(it)
            }
        })
    }
}