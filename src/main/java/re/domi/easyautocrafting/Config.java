package re.domi.easyautocrafting;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Config
{
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "eac.properties");
    private static final String CONFIG_COMMENT = "EasyAutoCrafting config file";

    private static final String CONFIG_VERSION = "configVersion";
    private static final String configVersion = "1";

    private static final String ENABLE_3X3_INVENTORY_SEARCHING = "enable3x3InventorySearching";

    public static boolean enable3x3InventorySearching = false;

    static void read()
    {
        try
        {
            if (CONFIG_FILE.createNewFile())
            {
                write();
                return;
            }

            FileInputStream in = new FileInputStream(CONFIG_FILE);
            Properties properties = new Properties();
            properties.load(in);

            String tru = Boolean.toString(true);

            enable3x3InventorySearching = tru.equals(properties.getProperty(ENABLE_3X3_INVENTORY_SEARCHING));

            if (!configVersion.equals(properties.getProperty(CONFIG_VERSION)))
            {
                write();
            }
        }
        catch (IOException e)
        {
            EasyAutoCrafting.LOGGER.error("Cannot read configuration file:", e);
        }
    }

    static void write()
    {
        try
        {
            FileOutputStream outputStream = new FileOutputStream(CONFIG_FILE);
            Properties properties = new Properties();

            properties.setProperty(CONFIG_VERSION, configVersion);
            properties.setProperty(ENABLE_3X3_INVENTORY_SEARCHING, Boolean.toString(enable3x3InventorySearching));

            properties.store(outputStream, CONFIG_COMMENT);
        }
        catch (IOException e)
        {
            EasyAutoCrafting.LOGGER.error("Cannot write configuration file:", e);
        }
    }
}
