package re.domi.easyautocrafting.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.DropperBlock;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import re.domi.easyautocrafting.CraftingDropper;

@Mixin(DropperBlock.class)
public class DropperBlockMixin extends DispenserBlock
{
    protected DropperBlockMixin(Settings settings)
    {
        super(settings);
    }

    @Inject(method = "dispense", at = @At("HEAD"), cancellable = true)
    protected void dispense(ServerWorld world, BlockPos pos, CallbackInfo ci)
    {
        CraftingDropper.dispense(world, pos, ci);
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos)
    {
        if (world instanceof ServerWorld && CraftingDropper.hasTableNextToBlock((ServerWorld)world, pos))
        {
            DropperBlockEntity dropper = (DropperBlockEntity)world.getBlockEntity(pos);

            int stackCount = 0;

            //noinspection ConstantConditions
            for (int i = 0; i < dropper.size(); i++)
            {
                if (!dropper.getStack(i).isEmpty())
                {
                    stackCount++;
                }
            }

            return stackCount;
        }

        return super.getComparatorOutput(state, world, pos);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify)
    {
        //noinspection ConstantConditions
        world.getBlockEntity(pos).markDirty();

        super.neighborUpdate(state, world, pos, block, fromPos, notify);
    }
}
