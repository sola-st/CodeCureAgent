```java
@Override
public Item[] removeItem(Item... slots) {
    List<Item> itemSlots = new ArrayList<>();
    for (Item slot : slots) {
        if (slot.getId() != 0 && slot.getCount() > 0) {
            itemSlots.add(slot.clone());
        }
    }

    for (int i = 0; i < this.size; ++i) {
        Item item = this.getItem(i);
        if (item.getId() == Item.AIR || item.getCount() <= 0) {
            continue;
        }

        removeFromItemSlots(itemSlots, item, i);

        if (itemSlots.isEmpty()) {
            break;
        }
    }

    return itemSlots.toArray(new Item[0]);
}

private void removeFromItemSlots(List<Item> itemSlots, Item item, int index) {
    Iterator<Item> iterator = itemSlots.iterator();
    while (iterator.hasNext()) {
        Item slot = iterator.next();
        if (slot.equals(item, item.hasMeta(), item.getCompoundTag() != null)) {
            int amount = Math.min(item.getCount(), slot.getCount());
            slot.setCount(slot.getCount() - amount);
            item.setCount(item.getCount() - amount);
            this.setItem(index, item);
            if (slot.getCount() <= 0) {
                iterator.remove();
            }
        }
    }
}
