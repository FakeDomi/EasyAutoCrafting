package re.domi.easyautocrafting;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;

public class StubScreenHandler extends ScreenHandler
{
    protected StubScreenHandler()
    {
        super(null, 0);
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index)
    {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player)
    {
        return false;
    }
}
