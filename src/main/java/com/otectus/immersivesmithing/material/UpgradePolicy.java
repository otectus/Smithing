package com.otectus.immersivesmithing.material;

import com.google.gson.JsonParseException;

/**
 * How an upgrade into this family is priced when its base is another piece of equipment (a smithing-table
 * upgrade, or a crafting recipe that takes a finished piece plus this metal).
 */
public enum UpgradePolicy {
    /** The cost is the base item's whole shape in this metal; the base is not needed. Vanilla netherite behaves so. */
    SHAPE("shape"),
    /** The cost is just the addition (the ingots the original recipe asked for) and the base piece is consumed. */
    ADDITION("addition");

    private final String id;

    UpgradePolicy(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static UpgradePolicy byId(String id) {
        for (UpgradePolicy p : values()) {
            if (p.id.equals(id)) return p;
        }
        throw new JsonParseException("Unknown upgrade_policy '" + id + "' (expected shape or addition)");
    }
}
