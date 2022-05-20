# EasyAutoCrafting

**Requires [Fabric API](https://modrinth.com/mod/fabric-api).** This mod improves automation in Minecraft by allowing you to construct automatic crafting machines utilizing vanilla blocks.
If you want to use this on a server, the clients **don't** have to install the mod.

It's as simple as placing a crafting table next to a dropper!*  
<sup><sub>* more steps required for it to actually do anything</sub></sub>

There are two ways to get automatic crafting going. The easier option to start with is *pattern crafting*. You use this mode by placing an inventory **behind** the dropper. You then put a pattern of items inside the dropper, forming the recipe you want it to craft.  
When you give it a redstone pulse now, it will try to craft the recipe using items from the inventory behind. The items inside the dropper will only act as a reference and **won't** be used up.

The second, more advanced option is *immediate crafting*. To use it, make sure that there is **no** inventory behind the dropper (hoppers also count as inventories). In this mode, you fill the dropper one-by-one with items starting from the top-left slot. For empty slots you'll have to use filler items that you extract again before sending the crafting redstone pulse. You might see that this mode can be very powerful if you manage to tame it. Or if you want to take it even further, you could try replacing the items of a dropper in pattern mode.

But fear not, this mod will assist you should you decide to build such a complex crafting machine:
- Hoppers can only insert one item per slot into crafting droppers.
- Comparators reading from a crafting dropper will output a signal strength equal to the amount of filled slots.

Both of these changes won't apply to normal droppers that don't touch a crafting table.

In case you don't like to see droppers spitting items around, there's one more thing for you:  
Just placing an inventory **in front** of the dropper will make it place the crafted items and any remainders like empty buckets or bottles inside without spilling anything. This also means that if there's not enough space inside the inventory, nothing will be crafted.

One minute showcase of this mod: https://youtu.be/ZwLmG1849W0

This mod took inspiration from gnembon's [Carpet Mod](https://github.com/gnembon/fabric-carpet) with its auto crafting feature, picking up the idea and making it more useful for different scenarios.
