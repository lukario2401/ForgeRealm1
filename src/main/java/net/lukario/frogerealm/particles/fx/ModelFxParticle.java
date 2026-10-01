package net.lukario.frogerealm.particles.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side life of one ModelFx: counts its ticks and follows its entity.
 * It is a NO_RENDER particle — ModelFxRenderer draws all of them in one go.
 * You shouldn't need to edit this to make new effects — only to add new ModelFx options.
 */
public class ModelFxParticle extends Particle {

    private final ModelFx fx;
    private final ClientLevel clientLevel;
    private long lastTickTime;

    protected ModelFxParticle(ClientLevel level, double x, double y, double z, ModelFx fx) {
        super(level, x, y, z);
        this.fx = fx;
        this.clientLevel = level;
        this.lifetime = fx.delayTicks() + fx.lifetimeTicks();
        this.gravity = 0f;
        this.hasPhysics = false;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.lastTickTime = level.getGameTime();

        if (fx.isClear()) {
            ModelFxRenderer.clearFollowing(fx.followId());
            this.remove();
            return;
        }
        if (fx.followsEntity()) {
            Entity entity = level.getEntity(fx.followId());
            if (entity == null || !entity.isAlive()) {
                this.remove();
                return;
            }
            moveTo(entity.position().add(fx.followOffset()));
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
        }
        ModelFxRenderer.track(this);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.lastTickTime = clientLevel.getGameTime();
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        if (fx.followsEntity()) {
            Entity entity = clientLevel.getEntity(fx.followId());
            if (entity == null || !entity.isAlive()) {
                this.remove();
                return;
            }
            moveTo(entity.position().add(fx.followOffset()));
        }
    }

    private void moveTo(Vec3 position) {
        this.setPos(position.x, position.y, position.z);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        // drawn by ModelFxRenderer
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.NO_RENDER;
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    // ---------- used by ModelFxRenderer ----------

    ModelFx fx() {
        return fx;
    }

    /** Ticks since it appeared (after the delay); negative = not visible yet. */
    float time(float partialTick) {
        return this.age + partialTick - fx.delayTicks();
    }

    /** Where the pivot is this frame. */
    Vec3 anchor(float partialTick) {
        if (fx.followsEntity()) {
            Entity entity = clientLevel.getEntity(fx.followId());
            if (entity != null) return entity.getPosition(partialTick).add(fx.followOffset());
        }
        return new Vec3(this.xo + (this.x - this.xo) * partialTick,
                this.yo + (this.y - this.yo) * partialTick,
                this.zo + (this.z - this.zo) * partialTick);
    }

    /** True when the particle engine stopped ticking it (level changed, particles cleared...). */
    boolean isStale(ClientLevel currentLevel) {
        return currentLevel != clientLevel || currentLevel.getGameTime() - lastTickTime > 5;
    }

    void discard() {
        this.remove();
    }

    /** Registered in ClientParticleHandler. */
    public static Particle create(ModelFx fx, ClientLevel level,
                                  double x, double y, double z, double xd, double yd, double zd) {
        return new ModelFxParticle(level, x, y, z, fx);
    }
}
