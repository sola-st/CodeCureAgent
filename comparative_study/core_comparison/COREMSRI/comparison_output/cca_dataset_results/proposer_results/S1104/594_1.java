```java
package cn.nukkit.inventory.transaction.data;

import cn.nukkit.item.Item;
import cn.nukkit.math.Vector3;
import lombok.ToString;

/**
 * @author CreeperFace
 */
@ToString
public class ReleaseItemData implements TransactionData {

    private int actionType;
    private int hotbarSlot;
    private Item itemInHand;
    private Vector3 headRot;

    public int getActionType() {
        return actionType;
    }

    public void setActionType(int actionType) {
        this.actionType = actionType;
    }

    public int getHotbarSlot() {
        return hotbarSlot;
    }

    public void setHotbarSlot(int hotbarSlot) {
        this.hotbarSlot = hotbarSlot;
    }

    public Item getItemInHand() {
        return itemInHand;
    }

    public void setItemInHand(Item itemInHand) {
        this.itemInHand = itemInHand;
    }

    public Vector3 getHeadRot() {
        return headRot;
    }

    public void setHeadRot(Vector3 headRot) {
        this.headRot = headRot;
    }
}
