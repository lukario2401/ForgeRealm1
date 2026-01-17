package net.lukario.frogerealm.item.custom.ranged;

import net.lukario.frogerealm.item.custom.detection.ClickState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class TarotDeck extends Item {
    public TarotDeck(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;

        if (ClickState.leftClickPressed){
            if (!player.level().isClientSide && !player.getCooldowns().isOnCooldown(this)) {

                Level level = player.level();

                shootVanillaSnowballProjectile(player,level,2f,0.5f);

                player.getCooldowns().addCooldown(this, 2);

                player.sendSystemMessage(Component.literal("Trig Left: " + ClickState.leftClickPressed));
            }
        }
        return true;
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        if (!level.isClientSide) {

            if (ClickState.rightClickPressed && !player.getCooldowns().isOnCooldown(this) && player.isShiftKeyDown()){

                shootVanillaSmallFireball(player,level,4f,0f);

                player.getCooldowns().addCooldown(this, 5);
            }
            if (ClickState.rightClickPressed && !player.getCooldowns().isOnCooldown(this) && !player.isShiftKeyDown()){

                shootLargeFireball(player,level,12f,0f,6);

                player.getCooldowns().addCooldown(this, 5);
            }

        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    private static void shootVanillaSnowballProjectile(Player player, Level level, Float velocity, Float inaccuracy){

        Snowball snowball = new Snowball(level, player);
        snowball.setPos(
                player.getX(),
                player.getEyeY() - 0.1,
                player.getZ()
        );

// direction + speed + inaccuracy
        snowball.shootFromRotation(
                player,
                player.getXRot(), // pitch
                player.getYRot(), // yaw
                0.0F,
                velocity, // speed
                inaccuracy  // inaccuracy
        );

        level.addFreshEntity(snowball);

    }

    private static void shootVanillaSmallFireball(Player player, Level level, float velocity, float inaccuracy) {
        if (level.isClientSide) return; // only run on server

        Vec3 look = player.getLookAngle();

        // Add Gaussian noise for inaccuracy
        RandomSource rand = level.getRandom();

        double nx = look.x + rand.nextGaussian() * inaccuracy;
        double ny = look.y + rand.nextGaussian() * inaccuracy;
        double nz = look.z + rand.nextGaussian() * inaccuracy;

        Vec3 motion = new Vec3(nx, ny, nz).normalize().scale(velocity);

        // spawn fireball slightly in front of player
        Vec3 spawnPos = player.getEyePosition(1.0f).add(look.x * 0.5, look.y * 0.5, look.z * 0.5);

        SmallFireball fireball = new SmallFireball(level, player, motion);
        fireball.setPos(spawnPos);

        level.addFreshEntity(fireball);
    }

    private static void shootLargeFireball(Player player, Level level, float velocity, float inaccuracy, int explosionPower) {
        if (level.isClientSide) return; // only run on server

        // Get player's look direction
        Vec3 look = player.getLookAngle();

        // Add Gaussian noise for inaccuracy
        RandomSource rand = level.getRandom();
        double nx = look.x + rand.nextGaussian() * inaccuracy;
        double ny = look.y + rand.nextGaussian() * inaccuracy;
        double nz = look.z + rand.nextGaussian() * inaccuracy;

        // normalize and scale by velocity
        Vec3 motion = new Vec3(nx, ny, nz).normalize().scale(velocity);

        // spawn fireball slightly in front of player
        Vec3 spawnPos = player.getEyePosition(1.0f).add(look.x * 0.5, look.y * 0.5, look.z * 0.5);

        // Create custom LargeFireball
        LargeFireball fireball = new LargeFireball(level, player, motion, explosionPower);
        fireball.setPos(spawnPos);

        // spawn entity
        level.addFreshEntity(fireball);
    }


}
