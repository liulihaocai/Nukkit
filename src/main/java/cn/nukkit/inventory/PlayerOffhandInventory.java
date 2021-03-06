package cn.nukkit.inventory;

import cn.nukkit.Player;
import cn.nukkit.network.protocol.InventorySlotPacket;
import cn.nukkit.network.protocol.types.ContainerIds;

/**
 * @author CreeperFace
 */
public class PlayerOffhandInventory extends BaseInventory {

    public PlayerOffhandInventory(Player holder) {
        super(holder, InventoryType.OFFHAND);
    }

    public String getName() {
        return "Offhand";
    }

    public int getSize() {
        return 1;
    }

    public void setSize(int size) {
        throw new RuntimeException("Offhand can only carry one item at a time");
    }

    public void sendSlot(int index, Player... target) {
        InventorySlotPacket pk = new InventorySlotPacket();
        pk.slot = index;
        pk.item = this.getItem(index);

        for (Player p : target) {
            if (p == this.getHolder()) {
                pk.inventoryId = ContainerIds.OFFHAND;
                p.dataPacket(pk);
            } else {
                int id;

                if ((id = p.getWindowId(this)) == ContainerIds.NONE) {
                    this.close(p);
                    continue;
                }
                pk.inventoryId = id;
                p.dataPacket(pk);
            }
        }
    }

    /**
     * This override is here for documentation and code completion purposes only.
     *
     * @return Player
     */
    public Player getHolder() {
        return (Player) this.holder;
    }
}
