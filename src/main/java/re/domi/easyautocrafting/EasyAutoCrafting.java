package re.domi.easyautocrafting;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.impl.event.lifecycle.LoadedChunksCache;

import java.util.ArrayList;

public class EasyAutoCrafting implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) ->
        {
            if (success)
            {
                server.getWorlds().forEach(
                    w -> ((LoadedChunksCache)w).fabric_getLoadedChunks().forEach(
                        c -> {
	                        c.getBlockEntities().values().stream().filter(DropperRecipeCache.class::isInstance).forEach(
		                        d -> {
			                        ((DropperRecipeCache) d).set(null);
		                        }
	                        );
	                        c.getBlockEntities().values().stream().filter(DropperItemsCache.class::isInstance).forEach(
		                        d -> {
			                        ((DropperItemsCache) d).setCachedList(new ArrayList<>(9));
		                        }
	                        );
                        }
						));
            }
        });
    }
}
