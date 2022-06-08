package re.domi.easyautocrafting.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import re.domi.easyautocrafting.CraftingDropper;

@SuppressWarnings("deprecation")
@Mixin(value = DropperBlock.class, priority = 1500)
public class DropperBlockMixin extends Block
{
    @Inject(method = "dispense", at = @At("HEAD"), cancellable = true)
    protected void eac_dispense(ServerWorld world, BlockPos pos, CallbackInfo ci)
    {
        CraftingDropper.dispense(world, pos, ci);
    }

    @Override
    @Unique(silent = true)
    public int getComparatorOutput(BlockState state, World world, BlockPos pos)
    {
        return super.getComparatorOutput(state, world, pos);
    }

    @Inject(method = { "getComparatorOutput", "method_9572" }, at = @At("HEAD"), cancellable = true, remap = false)
    public void eac_getComparatorOutput(BlockState state, World world, BlockPos pos, CallbackInfoReturnable<Integer> cir)
    {
        if (world instanceof ServerWorld && CraftingDropper.hasTableNextToBlock((ServerWorld)world, pos)
            && world.getBlockEntity(pos) instanceof DropperBlockEntity dropper)
        {
            int stackCount = 0;

            for (int i = 0; i < dropper.size(); i++)
            {
                if (!dropper.getStack(i).isEmpty())
                {
                    stackCount++;
                }
            }

            cir.setReturnValue(stackCount);
        }
    }

    @Override
    @Unique(silent = true)
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify)
    {
        super.neighborUpdate(state, world, pos, sourceBlock, sourcePos, notify);
    }

    @Inject(method = { "neighborUpdate", "method_9612" }, at = @At("HEAD"), remap = false)
    public void eac_neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify, CallbackInfo ci)
    {
        if (block == Blocks.CRAFTING_TABLE || world.getBlockState(fromPos).getBlock() == Blocks.CRAFTING_TABLE)
        {
            world.updateComparators(pos, state.getBlock());
        }
    }

    @SuppressWarnings("ConstantConditions")
    public DropperBlockMixin()
    {
        super(null);
    }
}
