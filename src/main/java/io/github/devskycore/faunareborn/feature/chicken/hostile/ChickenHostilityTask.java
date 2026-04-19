package io.github.devskycore.faunareborn.feature.chicken.hostile;

import io.github.devskycore.faunareborn.core.FaunaRebornPlugin;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

final class ChickenHostilityTask {

    private static final double DETECTION_RADIUS = 8.0D;
    private static final double DETECTION_RADIUS_SQ = DETECTION_RADIUS * DETECTION_RADIUS;

    private static final double BABY_RADIUS = 7.0D;

    private static final double CHASE_BREAK_RADIUS = 18.0D;
    private static final double CHASE_BREAK_RADIUS_SQ = CHASE_BREAK_RADIUS * CHASE_BREAK_RADIUS;

    private static final double ATTACK_RANGE = 1.5D;
    private static final double ATTACK_RANGE_SQ = ATTACK_RANGE * ATTACK_RANGE;

    private static final double SPEED = 0.25D;

    private static final long COOLDOWN = 900L;
    private static final long TICK_RATE = 2L;

    private final FaunaRebornPlugin plugin;

    private final Int2LongOpenHashMap lastAttack = new Int2LongOpenHashMap();
    private final List<Engagement> buffer = new ArrayList<>();

    private BukkitTask task;

    ChickenHostilityTask(FaunaRebornPlugin plugin) {
        this.plugin = plugin;
        this.lastAttack.defaultReturnValue(Long.MIN_VALUE);
    }

    void start() {
        if (task != null) return;

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0L, TICK_RATE);
    }

    void stop() {
        if (task != null) task.cancel();
        task = null;

        buffer.clear();
        lastAttack.clear();
    }

    private void tick() {
        buffer.clear();

        scan();
        apply();
    }

    // =========================
    // SCAN PHASE
    // =========================

    private void scan() {
        for (World world : plugin.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {

                if (!(entity instanceof Chicken chicken)) continue;

                if (!chicken.hasAI()) {
                    chicken.setAI(false);
                }

                if (chicken.isDead() || !chicken.isAdult()) {
                    lastAttack.remove(chicken.getEntityId());
                    continue;
                }

                if (!hasBabyNearby(chicken)) continue;

                Player target = findTarget(chicken);
                if (target == null) continue;

                if (distanceSq(chicken, target) > CHASE_BREAK_RADIUS_SQ) continue;

                buffer.add(new Engagement(chicken, target));
            }
        }
    }

    // =========================
    // APPLY PHASE
    // =========================

    private void apply() {
        long now = System.currentTimeMillis();

        for (Engagement e : buffer) {

            Chicken chicken = e.chicken();
            Player player = e.player();

            if (chicken.isDead() || player.isDead()) continue;
            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;

            double distSq = distanceSq(chicken, player);

            if (distSq > CHASE_BREAK_RADIUS_SQ) continue;

            move(chicken, player);

            if (distSq <= ATTACK_RANGE_SQ) {
                attack(chicken, player, now);
            }
        }
    }

    // =========================
    // AI LOGIC
    // =========================

    private Player findTarget(Chicken chicken) {
        Player best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity e : chicken.getNearbyEntities(DETECTION_RADIUS, DETECTION_RADIUS, DETECTION_RADIUS)) {

            if (!(e instanceof Player player)) continue;

            if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;
            if (player.isDead() || !player.isOnline()) continue;

            double dist = distanceSq(chicken, player);

            if (dist > DETECTION_RADIUS_SQ || dist >= bestDist) continue;

            best = player;
            bestDist = dist;

            if (dist <= ATTACK_RANGE_SQ) break;
        }

        return best;
    }

    private boolean hasBabyNearby(Chicken chicken) {
        for (Entity e : chicken.getNearbyEntities(BABY_RADIUS, BABY_RADIUS, BABY_RADIUS)) {
            if (e instanceof Chicken c && !c.isAdult() && !c.isDead()) {
                return true;
            }
        }
        return false;
    }

    // =========================
    // MOVEMENT FIX (CORE FIX)
    // =========================

    private void move(Chicken chicken, Player player) {

        double dx = player.getX() - chicken.getX();
        double dz = player.getZ() - chicken.getZ();

        double lenSq = dx * dx + dz * dz;
        if (lenSq < 0.0001D) return;

        double inv = SPEED / Math.sqrt(lenSq);

        double vx = dx * inv;
        double vz = dz * inv;

        chicken.setVelocity(new Vector(vx, chicken.getVelocity().getY(), vz));
    }

    // =========================
    // ATTACK SYSTEM
    // =========================

    private void attack(Chicken chicken, Player player, long now) {

        int id = chicken.getEntityId();
        long last = lastAttack.get(id);

        if (last != Long.MIN_VALUE && now - last < COOLDOWN) return;

        player.damage(2.0D, chicken);
        lastAttack.put(id, now);
    }

    // =========================
    // UTIL
    // =========================

    private double distanceSq(Entity a, Entity b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private record Engagement(Chicken chicken, Player player) {}
}