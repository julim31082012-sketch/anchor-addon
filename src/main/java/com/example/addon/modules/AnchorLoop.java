package com.example.addon.modules;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * Anchor-Loop: Torch (oben) wird abgebaut, im selben Tick wird an der freien
 * Stelle ein Respawn Anchor gesetzt, geladen und gezündet. Danach wiederholt.
 * Nur für Anarchie-Server (z. B. 10b10t) gedacht.
 *
 * Geschrieben gegen aktuelles Meteor (1.21.x). Methodennamen/Signaturen
 * können je nach Version leicht abweichen, bei Compile-Fehlern anpassen.
 */
public class AnchorLoop extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Double> range = sg.add(new DoubleSetting.Builder()
        .name("range")
        .description("Maximale Distanz zum Ziel.")
        .defaultValue(4.5)
        .min(1)
        .sliderMax(6)
        .build());

    private final Setting<Integer> cycles = sg.add(new IntSetting.Builder()
        .name("cycles-per-tick")
        .description("Wie viele Break/Place/Charge/Explode-Zyklen pro Tick.")
        .defaultValue(1)
        .min(1)
        .sliderMax(4)
        .build());

    private final Setting<Double> minHealth = sg.add(new DoubleSetting.Builder()
        .name("min-health")
        .description("Stoppt, wenn deine eigene Health (inkl. Absorption) darunter fällt.")
        .defaultValue(12)
        .min(0)
        .sliderMax(36)
        .build());

    private final Setting<Boolean> swing = sg.add(new BoolSetting.Builder()
        .name("swing")
        .description("Hand beim Abbauen/Platzieren schwingen.")
        .defaultValue(false)
        .build());

    public AnchorLoop() {
        super(Categories.Combat, "anchor-loop", "Torch-Break, Anchor setzen, laden, zünden, wiederholen.");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() < minHealth.get()) return;

        PlayerEntity target = TargetUtils.getPlayerTarget(range.get(), SortPriority.LowestDistance);
        if (TargetUtils.isBadTarget(target, range.get())) return;

        FindItemResult torch = InvUtils.findInHotbar(Items.TORCH);
        FindItemResult anchor = InvUtils.findInHotbar(Items.RESPAWN_ANCHOR);
        FindItemResult glowstone = InvUtils.findInHotbar(Items.GLOWSTONE);
        if (!torch.found() || !anchor.found() || !glowstone.found()) return;

        // Block direkt über dem Kopfraum des Ziels (oben im Setup liegt die Torch)
        BlockPos pos = target.getBlockPos().up();

        for (int i = 0; i < cycles.get(); i++) {
            // 1) Torch ggf. neu setzen (nach der Explosion ist sie weg)
            if (mc.world.getBlockState(pos).isAir()) {
                place(pos, torch);
            }

            // 2) Torch instant abbauen (Hardness 0)
            mc.interactionManager.attackBlock(pos, Direction.DOWN);
            if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);

            // 3) Anchor in die freie Stelle setzen
            if (!place(pos, anchor)) return;

            // 4) Laden mit Glowstone
            interact(pos, glowstone);

            // 5) Zünden: Torch in der Hand ist kein Glowstone, also Explosion (Overworld)
            interact(pos, torch);
        }
    }

    /** Setzt den Slot-Inhalt an pos, gestützt auf einen festen Nachbarblock. */
    private boolean place(BlockPos pos, FindItemResult item) {
        BlockHitResult hit = findSupport(pos);
        if (hit == null) return false;

        InvUtils.swap(item.slot(), true);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
        InvUtils.swapBack();
        return true;
    }

    /** Rechtsklick auf den Block bei pos mit dem Item im Slot. */
    private void interact(BlockPos pos, FindItemResult item) {
        BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);

        InvUtils.swap(item.slot(), true);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
        if (swing.get()) mc.player.swingHand(Hand.MAIN_HAND);
        InvUtils.swapBack();
    }

    /** Sucht einen soliden Nachbarblock, gegen den platziert werden kann (nicht von unten). */
    private BlockHitResult findSupport(BlockPos pos) {
        Direction[] order = {
            Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
        };

        for (Direction d : order) {
            BlockPos neighbor = pos.offset(d);
            if (mc.world.getBlockState(neighbor).isSolidBlock(mc.world, neighbor)) {
                Vec3d hitPos = Vec3d.ofCenter(neighbor).offset(d.getOpposite(), 0.5);
                return new BlockHitResult(hitPos, d.getOpposite(), neighbor, false);
            }
        }
        return null;
    }
}
