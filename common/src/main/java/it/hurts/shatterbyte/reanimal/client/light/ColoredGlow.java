package it.hurts.shatterbyte.reanimal.client.light;

import com.mojang.blaze3d.vertex.PoseStack;
import it.hurts.shatterbyte.reanimal.common.entity.glow_stick.GlowStickEntity;
import it.hurts.shatterbyte.reanimal.common.entity.jellyfish.JellyfishEntity;
import it.hurts.shatterbyte.reanimal.common.item.GlowStickItem;
import it.hurts.shatterbyte.reanimal.init.ReAnimalEntities;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

public final class ColoredGlow {
    public static final int WHITE = 0xFFFFFFFF;

    private static final int REACH = GlowReach.RANGE;
    private static final double NEAR_RADIUS_SQR = (REACH + 1.5D) * (REACH + 1.5D);
    private static final int REDIRTY_DELAY = 4;
    private static final int REFLOOD_INTERVAL = 20;
    private static final int LINGER_TICKS = 8;
    private static final float SMOOTH_MAX = 240.0F;
    private static final float EDGE_FADE = 0.1F;
    private static final GlowSource[] NONE = new GlowSource[0];

    private static final Int2ObjectOpenHashMap<Tracked> TRACKED = new Int2ObjectOpenHashMap<>();

    private static volatile GlowSource[] sources = NONE;
    private static long tick;
    private static boolean lightmapDirty;

    private ColoredGlow() {
    }

    public static void update(ClientLevel level) {
        var camera = Minecraft.getInstance().getCameraEntity();

        if (camera == null) {
            reset(level);

            return;
        }

        tick++;

        var found = new ArrayList<Candidate>();

        for (var entity : level.entitiesForRendering())
            if (entity instanceof GlowStickEntity stick && stick.getItem().getItem() instanceof GlowStickItem item)
                found.add(new Candidate(stick.getId(), GlowSource.at(stick.position().add(0.0D, 0.12D, 0.0D), GlowColors.of(item))));
            else if (entity instanceof JellyfishEntity jellyfish)
                found.add(new Candidate(jellyfish.getId(), GlowSource.at(jellyfish.getBoundingBox().getCenter(), GlowColors.of(jellyfish, 1F), jellyfish.getLightLevel())));

        for (var player : level.players()) {
            var held = heldStick(player);

            if (held != null)
                found.add(new Candidate(player.getId(), GlowSource.at(player.position().add(0.0D, 1.0D, 0.0D), GlowColors.of(held))));
        }

        var alive = new IntOpenHashSet();
        var active = new ArrayList<GlowSource>();

        for (var candidate : found) {
            alive.add(candidate.id());
            active.add(follow(level, candidate));
        }

        forget(level, alive, active);

        sources = active.toArray(NONE);

        if (lightmapDirty && tick % REDIRTY_DELAY == 0) {
            for (var state : TRACKED.values())
                markDirty(level, state.pos);

            lightmapDirty = false;
        }
    }

    public static void onLightmapChanged() {
        lightmapDirty = true;
    }

    public static void onBlockChanged(BlockPos pos, BlockState oldState, BlockState newState) {
        if (oldState.getLightDampening() == newState.getLightDampening())
            return;

        for (var state : TRACKED.values())
            if (state.pos.distSqr(pos) <= REACH * REACH)
                state.lastFlood = tick - REFLOOD_INTERVAL;
    }

    public static boolean isNear(BlockPos pos) {
        var snapshot = sources;

        if (snapshot.length == 0)
            return false;

        for (var source : snapshot)
            if (source.distanceSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= NEAR_RADIUS_SQR)
                return true;

        return false;
    }

    public static int tint(double x, double y, double z, int lightCoords) {
        return tint(x, y, z, lightCoords, null);
    }

    public static int tint(double x, double y, double z, int lightCoords, BlockPos anchor) {
        var snapshot = sources;
        var local = LightCoordsUtil.smoothBlock(lightCoords) / SMOOTH_MAX;

        if (snapshot.length == 0)
            return WHITE;

        var total = 0.0F;
        var red = 0.0F;
        var green = 0.0F;
        var blue = 0.0F;

        for (var source : snapshot) {
            var share = (float) ((source.range() - Math.sqrt(source.distanceSqr(x, y, z))) / source.range());

            if (share <= 0.0F)
                continue;

            var reach = anchor == null ? source.reachAt(x, y, z) : Math.max(source.reachAt(x, y, z), source.reachAt(anchor));

            if (reach <= 0)
                continue;

            share = Math.min(share, (reach + 1.0F) / source.range());

            total += share;
            red += source.red() * share;
            green += source.green() * share;
            blue += source.blue() * share;
        }

        if (total <= 0.0F)
            return WHITE;

        var peak = Math.max(red, Math.max(green, blue));
        var amount = Math.min(1.0F, total / local) * smoothstep(total / EDGE_FADE);

        return GlowLightmap.tint(lightCoords, red / peak, green / peak, blue / peak, amount);
    }

    public static int tintOf(Object state, int lightCoords, PoseStack.Pose fallback) {
        if (state instanceof EntityRenderState entity) {
            if (entity.entityType == ReAnimalEntities.GLOW_STICK || entity.entityType == ReAnimalEntities.JELLYFISH)
                return WHITE;

            return tint(entity.x, entity.y + entity.boundingBoxHeight * 0.5D, entity.z, lightCoords);
        }

        if (state instanceof BlockEntityRenderState blockEntity)
            return tint(blockEntity.blockPos.getX() + 0.5D, blockEntity.blockPos.getY() + 0.5D, blockEntity.blockPos.getZ() + 0.5D, lightCoords);

        return tintAtPose(fallback, lightCoords);
    }

    public static int tintAtPose(PoseStack.Pose pose, int lightCoords) {
        if (sources.length == 0)
            return WHITE;

        var camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
        var matrix = pose.pose();

        return tint(camera.x + matrix.m30(), camera.y + matrix.m31(), camera.z + matrix.m32(), lightCoords);
    }

    public static int multiplyAbgr(int abgr, int tint) {
        return (abgr & 0xFF000000)
                | ((abgr >> 16 & 0xFF) * (tint & 0xFF) / 255) << 16
                | ((abgr >> 8 & 0xFF) * (tint >> 8 & 0xFF) / 255) << 8
                | (abgr & 0xFF) * (tint >> 16 & 0xFF) / 255;
    }

    public static int apply(int color, int tint) {
        return tint == WHITE || (color >>> 24) != 0xFF ? color : multiply(color, tint);
    }

    public static int multiply(int color, int tint) {
        return (color & 0xFF000000)
                | ((color >> 16 & 0xFF) * (tint >> 16 & 0xFF) / 255) << 16
                | ((color >> 8 & 0xFF) * (tint >> 8 & 0xFF) / 255) << 8
                | (color & 0xFF) * (tint & 0xFF) / 255;
    }

    private static float smoothstep(float x) {
        var t = Math.clamp(x, 0.0F, 1.0F);

        return t * t * (3.0F - 2.0F * t);
    }

    private static GlowStickItem heldStick(Player player) {
        if (player.getMainHandItem().getItem() instanceof GlowStickItem item)
            return item;

        if (player.getOffhandItem().getItem() instanceof GlowStickItem item)
            return item;

        return null;
    }

    private static GlowSource follow(ClientLevel level, Candidate candidate) {
        var source = candidate.source();
        var block = BlockPos.containing(source.x(), source.y(), source.z());
        var rgb = source.rgb();
        var state = TRACKED.get(candidate.id());

        if (state == null) {
            state = new Tracked(block, rgb, source.range(), tick, GlowReach.flood(level, block, source.range()));

            TRACKED.put(candidate.id(), state);
            markDirty(level, block);
        } else if ((!state.pos.equals(block) || state.rgb != rgb || state.range != source.range()) && tick - state.lastDirty >= REDIRTY_DELAY) {
            markDirty(level, state.pos);
            markDirty(level, block);

            if (!state.pos.equals(block) || state.range != source.range()) {
                state.reach = GlowReach.flood(level, block, source.range());
                state.lastFlood = tick;
            }

            state.pos = block;
            state.rgb = rgb;
            state.range = source.range();
            state.lastDirty = tick;
        } else if (tick - state.lastFlood >= REFLOOD_INTERVAL) {
            var reach = GlowReach.flood(level, state.pos, state.range);

            if (!reach.equals(state.reach))
                markDirty(level, state.pos);

            state.reach = reach;
            state.lastFlood = tick;
        }

        state.last = source.reaching(state.reach);
        state.lastSeen = tick;

        return state.last;
    }

    private static void forget(ClientLevel level, IntOpenHashSet alive, ArrayList<GlowSource> active) {
        var iterator = TRACKED.int2ObjectEntrySet().fastIterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();

            if (alive.contains(entry.getIntKey()))
                continue;

            var state = entry.getValue();

            if (tick - state.lastSeen < LINGER_TICKS) {
                active.add(state.last);

                continue;
            }

            markDirty(level, state.pos);
            iterator.remove();
        }
    }

    private static void reset(ClientLevel level) {
        if (level != null)
            for (var state : TRACKED.values())
                markDirty(level, state.pos);

        TRACKED.clear();
        sources = NONE;
    }

    private static void markDirty(ClientLevel level, BlockPos pos) {
        level.setSectionRangeDirty(
                SectionPos.blockToSectionCoord(pos.getX() - REACH),
                SectionPos.blockToSectionCoord(pos.getY() - REACH),
                SectionPos.blockToSectionCoord(pos.getZ() - REACH),
                SectionPos.blockToSectionCoord(pos.getX() + REACH),
                SectionPos.blockToSectionCoord(pos.getY() + REACH),
                SectionPos.blockToSectionCoord(pos.getZ() + REACH)
        );
    }

    private record Candidate(int id, GlowSource source) {
    }

    private static final class Tracked {
        private BlockPos pos;
        private int rgb;
        private int range;
        private Long2ByteMap reach;
        private long lastDirty;
        private long lastFlood;
        private long lastSeen;
        private GlowSource last;

        private Tracked(BlockPos pos, int rgb, int range, long now, Long2ByteMap reach) {
            this.pos = pos;
            this.rgb = rgb;
            this.range = range;
            this.reach = reach;
            this.lastDirty = now;
            this.lastFlood = now;
            this.lastSeen = now;
        }
    }
}
