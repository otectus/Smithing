package com.otectus.otessmithing.workpiece;

/** FORGED: produced by the forge, awaiting the anvil. SHAPED: hammered (or Faulty), ready to quench. */
public enum WorkpieceState {
    FORGED,
    SHAPED;

    public static WorkpieceState byName(String name) {
        for (WorkpieceState s : values()) {
            if (s.name().equalsIgnoreCase(name)) return s;
        }
        return null;
    }
}
