package kr.ac.jbnu.se.tetris.ui.seongeun.model;

public class ItemData {

    private final String itemId;
    private final String itemName;

    public ItemData(String itemId, String itemName) {
        this.itemId = itemId;
        this.itemName = itemName;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

}
