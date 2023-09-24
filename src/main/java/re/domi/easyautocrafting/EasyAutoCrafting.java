package re.domi.easyautocrafting;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.impl.event.lifecycle.LoadedChunksCache;

public class EasyAutoCrafting implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        Config.read();

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) ->
        {
            if (success)
            {
                //noinspection UnstableApiUsage
                server.getWorlds().forEach(
                    w -> ((LoadedChunksCache)w).fabric_getLoadedChunks().forEach(
                        c -> c.getBlockEntities().values().stream()
                            .filter(DropperCache.class::isInstance)
                            .map(DropperCache.class::cast)
                            .forEach(DropperCache::eac_clearCache)));
            }
        });
    }
}
