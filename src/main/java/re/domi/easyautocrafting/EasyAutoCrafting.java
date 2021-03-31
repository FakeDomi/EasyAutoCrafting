package re.domi.easyautocrafting;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class EasyAutoCrafting implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) ->
        {
            if (success)
            {
                server.getWorlds().forEach(w -> w.blockEntities.stream().filter(DropperRecipeCache.class::isInstance).forEach(d -> ((DropperRecipeCache)d).set(null)));
            }
        });
    }
}
