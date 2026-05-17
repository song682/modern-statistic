package decok.dfcdvadstf.modernstatistic.mixin;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.stats.StatList;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes two vanilla 1.7.10 stat-tracking gaps:
 * <ol>
 *   <li><b>Air-use items</b> — {@code tryUseItem} never calls
 *       {@code addStat} for right-click-in-air (snowballs, food,
 *       potions, ender pearls, etc.).</li>
 *   <li><b>Wrong-tool / bare-hand mining</b> —
 *       {@code tryHarvestBlock} only increments
 *       {@code mineBlockStatArray} when {@code canHarvestBlock}
 *       is {@code true}. Mining a block with the wrong tool (or
 *       bare hands) removes the block but skips the stat.</li>
 * </ol>
 */
@Mixin(ItemInWorldManager.class)
public abstract class MixinItemInWorldManager {

    @Shadow private World theWorld;
    @Shadow public net.minecraft.entity.player.EntityPlayerMP thisPlayerMP;

    @Shadow public abstract boolean isCreative();

    // ── Gap #1: air-use stat ─────────────────────────────────────

    @Inject(method = "tryUseItem", at = @At("RETURN"))
    private void modernStatistic$onTryUseItem(EntityPlayer player, World world,
                                               ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || stack == null || world.isRemote) return;
        player.addStat(StatList.objectUseStats[Item.getIdFromItem(stack.getItem())], 1);
    }

    // ── Gap #2: wrong-tool mining stat ───────────────────────────

    @Unique
    private Block modernStatistic$pendingBlock;
    @Unique
    private int modernStatistic$pendingMeta;

    /** Snapshot the block and its metadata before the harvest attempt. */
    @Inject(method = "tryHarvestBlock", at = @At("HEAD"))
    private void modernStatistic$captureBlock(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        this.modernStatistic$pendingBlock = this.theWorld.getBlock(x, y, z);
        this.modernStatistic$pendingMeta = this.theWorld.getBlockMetadata(x, y, z);
    }

    /**
     * If the block was removed in survival mode but
     * {@code canHarvestBlock} was false (wrong tool / bare hands),
     * the vanilla {@code harvestBlock} was skipped — add the
     * mining stat here.
     */
    @Inject(method = "tryHarvestBlock", at = @At("RETURN"))
    private void modernStatistic$fillMiningStatGap(int x, int y, int z,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || this.isCreative()) return;
        Block block = this.modernStatistic$pendingBlock;
        if (block != null && !block.canHarvestBlock(this.thisPlayerMP,
                                                     this.modernStatistic$pendingMeta)) {
            this.thisPlayerMP.addStat(
                StatList.mineBlockStatArray[Block.getIdFromBlock(block)], 1);
        }
    }
}
