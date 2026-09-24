package com.otectus.immersivesmithing.minigame;

import com.google.gson.JsonObject;
import com.otectus.immersivesmithing.ImmersiveSmithing;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

/**
 * Parameters of the Forge "forming rhythm" minigame. A marker sweeps back and forth along a track, speeding
 * up the longer the smith waits; one input per phase is scored by its distance from the zone centre.
 */
public record ForgePattern(ResourceLocation id, int basePhases, int unitsPerExtraPhase, int minPhases, int maxPhases,
                           float halfWidth, float minSpeed, float maxSpeed, float acceleration, int transitionMs) {

    public static final ResourceLocation STANDARD = ImmersiveSmithing.id("standard");
    public static final ForgePattern DEFAULT = new ForgePattern(STANDARD, 3, 24, 3, 7, 0.085F, 0.45F, 0.8F, 0.35F, 450);

    public int phaseCount(int metalUnits) {
        int extra = unitsPerExtraPhase > 0 ? metalUnits / unitsPerExtraPhase : 0;
        return Mth.clamp(basePhases + extra, minPhases, maxPhases);
    }

    public static ForgePattern fromJson(ResourceLocation id, JsonObject json) {
        ForgePattern d = DEFAULT;
        int min = Math.max(1, GsonHelper.getAsInt(json, "min_phases", d.minPhases));
        int max = Math.max(min, GsonHelper.getAsInt(json, "max_phases", d.maxPhases));
        float minSpeed = Math.max(0.05F, GsonHelper.getAsFloat(json, "min_speed", d.minSpeed));
        return new ForgePattern(id,
                Math.max(1, GsonHelper.getAsInt(json, "base_phases", d.basePhases)),
                Math.max(0, GsonHelper.getAsInt(json, "units_per_extra_phase", d.unitsPerExtraPhase)),
                min, max,
                Mth.clamp(GsonHelper.getAsFloat(json, "zone_half_width", d.halfWidth), 0.01F, 0.5F),
                minSpeed,
                Math.max(minSpeed, GsonHelper.getAsFloat(json, "max_speed", d.maxSpeed)),
                Math.max(0F, GsonHelper.getAsFloat(json, "acceleration", d.acceleration)),
                Math.max(0, GsonHelper.getAsInt(json, "transition_ms", d.transitionMs)));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(id);
        buf.writeVarInt(basePhases);
        buf.writeVarInt(unitsPerExtraPhase);
        buf.writeVarInt(minPhases);
        buf.writeVarInt(maxPhases);
        buf.writeFloat(halfWidth);
        buf.writeFloat(minSpeed);
        buf.writeFloat(maxSpeed);
        buf.writeFloat(acceleration);
        buf.writeVarInt(transitionMs);
    }

    public static ForgePattern read(FriendlyByteBuf buf) {
        return new ForgePattern(buf.readResourceLocation(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt());
    }
}
