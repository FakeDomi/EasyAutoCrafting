package re.domi.easyautocrafting.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.block.WireOrientation;
import net.minecraft.world.tick.ScheduledTickView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import re.domi.easyautocrafting.CraftingDropper;

@Mixin(value = DropperBlock.class, priority = 1500)
public class DropperBlockMixin extends Block
{
    @Inject(method = "dispense", at = @At("HEAD"), cancellable = true)
    protected void eac_dispense(ServerWorld world, BlockState state, BlockPos pos, CallbackInfo ci)
    {
        CraftingDropper.dispense(world, state, pos, ci);
    }

    @Override
    @Unique(silent = true)
    public int getComparatorOutput(BlockState state, World world, BlockPos pos)
    {
        return super.getComparatorOutput(state, world, pos);
    }

    @SuppressWarnings({ "UnresolvedMixinReference", "MixinAnnotationTarget" })
    @Inject(method = { "getComparatorOutput", "method_9572" }, at = @At("HEAD"), cancellable = true, remap = false)
    public void eac_getComparatorOutput(BlockState state, World world, BlockPos pos, CallbackInfoReturnable<Integer> cir)
    {
        if (world instanceof ServerWorld
            && CraftingDropper.hasTableNextToBlock((ServerWorld)world, pos)
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
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random)
    {
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @SuppressWarnings({ "UnresolvedMixinReference", "MixinAnnotationTarget" })
    @Inject(method = { "getStateForNeighborUpdate", "method_9559" }, at = @At("HEAD"), remap = false)
    public void eac_getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random, CallbackInfoReturnable<BlockState> cir)
    {
        if (neighborState.getBlock() == Blocks.CRAFTING_TABLE && world instanceof World w)
        {
            w.updateComparators(pos, state.getBlock());
        }
    }

    @Override
    @Unique(silent = true)
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, WireOrientation wireOrientation, boolean notify)
    {
        super.neighborUpdate(state, world, pos, sourceBlock, wireOrientation, notify);
    }

    @SuppressWarnings({ "UnresolvedMixinReference", "MixinAnnotationTarget" })
    @Inject(method = { "neighborUpdate", "method_9612" }, at = @At("HEAD"), remap = false)
    public void eac_neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, WireOrientation wireOrientation, boolean notify, CallbackInfo ci)
    {
        if (sourceBlock == Blocks.CRAFTING_TABLE)
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
