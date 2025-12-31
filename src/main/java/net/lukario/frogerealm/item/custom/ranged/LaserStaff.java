package net.lukario.frogerealm.item.custom.ranged;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;


public class LaserStaff extends Item {

    public LaserStaff(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        laserRay(level, player);

        return InteractionResultHolder.success(stack);
    }

    public static void laserRay(Level level, LivingEntity shooter) {
        Vec3 start = shooter.getEyePosition();
        Vec3 dir = shooter.getLookAngle();
        double maxDistance = 30.0;

        Vec3 end = start.add(dir.scale(maxDistance));

        /* ---------- BLOCK RAY ---------- */
        BlockHitResult blockHit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                shooter
        ));

        double maxEntityDistance = maxDistance;
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            maxEntityDistance = start.distanceTo(blockHit.getLocation());
        }

        /* ---------- ENTITY RAY ---------- */
        EntityHitResult entityHit = null;
        double closest = maxEntityDistance;

        for (Entity entity : level.getEntities(
                shooter,
                shooter.getBoundingBox()
                        .expandTowards(dir.scale(maxEntityDistance))
                        .inflate(1.0),
                e -> e instanceof LivingEntity && e != shooter
        )) {
            AABB box = entity.getBoundingBox().inflate(0.3);
            var hit = box.clip(start, end);

            if (hit.isPresent()) {
                double dist = start.distanceTo(hit.get());
                if (dist < closest) {
                    closest = dist;
                    entityHit = new EntityHitResult(entity);
                }
            }
        }

        /* ---------- FINAL HIT POINT ---------- */
        Vec3 hitPoint = entityHit != null
                ? start.add(dir.scale(closest))
                : (blockHit.getType() == HitResult.Type.BLOCK
                ? blockHit.getLocation()
                : end);

        /* ---------- CLIENT: PARTICLES ---------- */
        if (level.isClientSide) {
            spawnBeamParticles(level, start, hitPoint);
        }

        /* ---------- SERVER: DAMAGE ---------- */
        if (!level.isClientSide && entityHit != null) {
            ((LivingEntity) entityHit.getEntity())
                    .hurt(level.damageSources().magic(), 8.0F);
        }
    }

    private static void spawnBeamParticles(Level level, Vec3 start, Vec3 end) {
        Vec3 diff = end.subtract(start);
        double length = diff.length();
        Vec3 step = diff.normalize().scale(0.3);

        Vec3 pos = start;
        for (double traveled = 0; traveled < length; traveled += 0.3) {
            level.addParticle(
                    ParticleTypes.END_ROD,
                    pos.x, pos.y, pos.z,
                    0, 0, 0
            );
            pos = pos.add(step);
        }
    }

}
