package net.lukario.frogerealm.particles.fx;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.particles.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A 3D model shown in the world for a while: a summoned weapon, ice crystals on a frozen mob, a floating rune...
 * Like ParticleFx / SlashFx: describe it once, every method returns a NEW copy, nothing to register.
 *
 * ---------- 1. Make the model ----------
 * In Blockbench: File > New > "Java Block/Item". Build it, paint it, then File > Export > "Export Block/Item Model".
 *   - model JSON  ->  src/main/resources/assets/forgerealmmod/models/model_fx/<name>.json
 *   - texture PNG ->  src/main/resources/assets/forgerealmmod/textures/model_fx/<name>.png
 *   - in the JSON, the texture must be "forgerealmmod:model_fx/<name>"  (Blockbench writes the path for you
 *     if you saved the PNG in that folder; otherwise fix the "textures" part by hand).
 * Every model in models/model_fx/ is loaded automatically. Java models can only use cubes, coordinates
 * -16..32 pixels, and per-cube rotations of -45/-22.5/0/22.5/45 — make it small and use .scale(...) for big things.
 *
 * Orientation in Blockbench: the model's FRONT is NORTH (-Z), up is +Y. When played facing a yaw, the front
 * points that way. The pivot (what it turns/scales around and what sits on the spawn position) is the
 * bottom center (8, 0, 8) unless you set .pivot(...) — e.g. the grip of a weapon.
 *
 * ---------- 2. Describe the effect ----------
 *   public static final ModelFx HAMMER = ModelFx.of("order_hammer")   // models/model_fx/order_hammer.json
 *           .scale(1.6f)                  // 1 = Blockbench size (16 px = 1 block)
 *           .pivot(8, -10, 8)             // pixels, in Blockbench coordinates
 *           .glow()                       // full bright
 *           .aura(0xFFFFB82E, 0.14f, 3)   // glowing outline: ARGB color, thickness in blocks, softness (layers)
 *           .lifetime(40).fade(3, 8)      // ticks; fade in / fade out ticks
 *           .key(0,  ModelFx.pose().scale(0.3f).alpha(0f))                 // animation keyframes (see Pose)
 *           .key(6,  ModelFx.pose().pitch(-30), ModelFx.Ease.OUT_BACK)
 *           .key(18, ModelFx.pose().pitch(90), ModelFx.Ease.IN);
 *
 * Other options: .color(argb) tint/opacity, .seeThrough() for textures with transparent pixels (ice, glass),
 * .spin(deg/tick), .delay(ticks), .rotation(yaw, pitch, roll).
 *
 * ---------- 3. Play it (server or client) ----------
 *   ParticleShapes.model(sl, HAMMER, position, player.getYRot(), 0, 0);           // at a spot, facing a yaw
 *   ParticleShapes.modelOn(sl, CRYSTALS, mob, offset, yaw, 0, 0);                // stuck to an entity, gone when it dies
 *   ParticleShapes.clearModels(sl, mob);                                         // remove everything stuck to it
 *
 * ---------- Animation ----------
 * Keyframes are Poses at given ticks (counted from when it appears). Between two keys it moves smoothly using
 * the second key's Ease. Pose = offset (forward/up/right, blocks), rotation (yaw/pitch/roll, degrees), scale, alpha.
 *   pitch + = tips the top forward (a downward swing)   roll + = tips the top to the right   yaw + = turns right
 * All rotations happen around the pivot.
 *
 * Test in game (shows the model at your feet for 100 ticks):
 *   /particle forgerealmmod:model{model:"forgerealmmod:model_fx/order_hammer",glow:1b,lifetime:100} ~ ~ ~ 0 0 0 0 1
 */
public final class ModelFx implements ParticleOptions {

    /** How a keyframe is reached from the one before it. */
    public enum Ease {
        /** constant speed */
        LINEAR,
        /** starts slow, ends fast (falling, slamming) */
        IN,
        /** starts fast, ends slow (landing softly) */
        OUT,
        /** slow - fast - slow */
        IN_OUT,
        /** goes a bit too far, then settles (popping into existence) */
        OUT_BACK,
        /** hits the end and bounces a few times */
        OUT_BOUNCE;

        public float apply(float t) {
            t = Mth.clamp(t, 0f, 1f);
            switch (this) {
                case IN: return t * t * t;
                case OUT: { float u = 1f - t; return 1f - u * u * u; }
                case IN_OUT: return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
                case OUT_BACK: {
                    float c1 = 1.70158f;
                    float c3 = c1 + 1f;
                    float u = t - 1f;
                    return 1f + c3 * u * u * u + c1 * u * u;
                }
                case OUT_BOUNCE: {
                    float n1 = 7.5625f;
                    float d1 = 2.75f;
                    if (t < 1f / d1) return n1 * t * t;
                    if (t < 2f / d1) { t -= 1.5f / d1; return n1 * t * t + 0.75f; }
                    if (t < 2.5f / d1) { t -= 2.25f / d1; return n1 * t * t + 0.9375f; }
                    t -= 2.625f / d1;
                    return n1 * t * t + 0.984375f;
                }
                default: return t;
            }
        }
    }

    /**
     * Where the model is in one keyframe, relative to where it was played:
     * offset in blocks (forward / up / right of the direction it faces), rotation in degrees, scale, alpha.
     * Build with ModelFx.pose().pitch(90).up(0.5f)... (each method returns a copy).
     */
    public record Pose(float forward, float up, float right, float yaw, float pitch, float roll, float scale, float alpha) {

        public static final Pose REST = new Pose(0, 0, 0, 0, 0, 0, 1, 1);

        public Pose forward(float blocks) { return new Pose(blocks, up, right, yaw, pitch, roll, scale, alpha); }
        public Pose up(float blocks) { return new Pose(forward, blocks, right, yaw, pitch, roll, scale, alpha); }
        public Pose right(float blocks) { return new Pose(forward, up, blocks, yaw, pitch, roll, scale, alpha); }
        public Pose move(float forward, float up, float right) { return new Pose(forward, up, right, yaw, pitch, roll, scale, alpha); }
        /** + turns right */
        public Pose yaw(float degrees) { return new Pose(forward, up, right, degrees, pitch, roll, scale, alpha); }
        /** + tips the top forward */
        public Pose pitch(float degrees) { return new Pose(forward, up, right, yaw, degrees, roll, scale, alpha); }
        /** + tips the top to the right */
        public Pose roll(float degrees) { return new Pose(forward, up, right, yaw, pitch, degrees, scale, alpha); }
        public Pose rotation(float yaw, float pitch, float roll) { return new Pose(forward, up, right, yaw, pitch, roll, scale, alpha); }
        /** multiplies the effect's own scale */
        public Pose scale(float scale) { return new Pose(forward, up, right, yaw, pitch, roll, scale, alpha); }
        /** 0 = invisible, 1 = normal */
        public Pose alpha(float alpha) { return new Pose(forward, up, right, yaw, pitch, roll, scale, alpha); }

        public Pose lerp(Pose to, float t) {
            return new Pose(Mth.lerp(t, forward, to.forward), Mth.lerp(t, up, to.up), Mth.lerp(t, right, to.right),
                    Mth.lerp(t, yaw, to.yaw), Mth.lerp(t, pitch, to.pitch), Mth.lerp(t, roll, to.roll),
                    Mth.lerp(t, scale, to.scale), Mth.lerp(t, alpha, to.alpha));
        }
    }

    /** One keyframe: at 'tick' the model is in 'pose', reached from the previous key with 'ease'. */
    public record Key(int tick, Pose pose, Ease ease) {}

    /** Start for keyframes: no offset, no rotation, scale 1, alpha 1. */
    public static Pose pose() {
        return Pose.REST;
    }

    public static final String FOLDER = "model_fx";
    private static final int MAX_KEYS = 64;

    private ResourceLocation model;
    private int color;
    private boolean glow;
    private boolean seeThrough;
    private int auraColor;
    private float auraSize;
    private int auraLayers;
    private float scaleX, scaleY, scaleZ;
    private float pivotX, pivotY, pivotZ;   // pixels
    private float yaw, pitch, roll;         // degrees
    private int lifetime;
    private int delay;
    private int fadeIn;
    private int fadeOut;
    private float spin;                     // degrees per tick, around the model's up
    private int followId;                   // -1 = stays where it was played
    private Vec3 followOffset;
    private boolean clear;                  // special: removes models stuck to followId
    private List<Key> keys;

    private ModelFx() {}

    private ModelFx copy() {
        ModelFx c = new ModelFx();
        c.model = model; c.color = color; c.glow = glow; c.seeThrough = seeThrough;
        c.auraColor = auraColor; c.auraSize = auraSize; c.auraLayers = auraLayers;
        c.scaleX = scaleX; c.scaleY = scaleY; c.scaleZ = scaleZ;
        c.pivotX = pivotX; c.pivotY = pivotY; c.pivotZ = pivotZ;
        c.yaw = yaw; c.pitch = pitch; c.roll = roll;
        c.lifetime = lifetime; c.delay = delay; c.fadeIn = fadeIn; c.fadeOut = fadeOut; c.spin = spin;
        c.followId = followId; c.followOffset = followOffset; c.clear = clear;
        c.keys = keys;
        return c;
    }

    /**
     * A model from models/model_fx/ (file name without .json), e.g. "order_hammer".
     * "namespace:path" uses any model, e.g. "forgerealmmod:model_fx/sub/thing".
     * Defaults: white, scale 1, pivot (8, 0, 8), 20 ticks, no glow, no aura.
     */
    public static ModelFx of(String name) {
        ModelFx fx = new ModelFx();
        fx.model = name.contains(":")
                ? ResourceLocation.parse(name)
                : ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, FOLDER + "/" + name);
        fx.color = 0xFFFFFFFF;
        fx.glow = false;
        fx.seeThrough = false;
        fx.auraColor = 0;
        fx.auraSize = 0.1f;
        fx.auraLayers = 3;
        fx.scaleX = fx.scaleY = fx.scaleZ = 1f;
        fx.pivotX = 8f; fx.pivotY = 0f; fx.pivotZ = 8f;
        fx.lifetime = 20;
        fx.followId = -1;
        fx.followOffset = Vec3.ZERO;
        fx.keys = List.of();
        return fx;
    }

    /** Special "effect" that removes every model stuck to this entity (see ParticleShapes.clearModels). */
    public static ModelFx clearing(Entity entity) {
        ModelFx fx = of("clear").following(entity).lifetime(1);
        fx.clear = true;
        return fx;
    }

    // ---------- look ----------

    /** Tint (multiplies the texture) and opacity, ARGB. 0x80FFFFFF = half see-through. */
    public ModelFx color(int argb) { ModelFx c = copy(); c.color = argb; return c; }

    /** Full brightness: visible in the dark, not affected by the world's light. */
    public ModelFx glow() { ModelFx c = copy(); c.glow = true; return c; }

    /**
     * For textures with see-through pixels (ice, glass, energy shells): drawn after the other models
     * so whatever is inside it stays visible.
     */
    public ModelFx seeThrough() { ModelFx c = copy(); c.seeThrough = true; return c; }

    /**
     * Glowing outline around the model (light added on top, like the hammer's golden glow).
     * @param argb   color; alpha = strength
     * @param blocks how far the glow reaches out from the model
     */
    public ModelFx aura(int argb, float blocks) { return aura(argb, blocks, auraLayers); }

    /** layers = how soft the glow is (1 = one hard shell, 3-4 = soft). Each layer draws the model again. */
    public ModelFx aura(int argb, float blocks, int layers) {
        ModelFx c = copy(); c.auraColor = argb; c.auraSize = blocks; c.auraLayers = Mth.clamp(layers, 1, 6); return c;
    }

    public ModelFx noAura() { ModelFx c = copy(); c.auraColor = 0; return c; }

    // ---------- shape ----------

    /** 1 = the size it has in Blockbench (16 px = 1 block). */
    public ModelFx scale(float scale) { return scale(scale, scale, scale); }

    /** Different scale per axis (x = width, y = height, z = depth/length). */
    public ModelFx scale(float x, float y, float z) {
        ModelFx c = copy(); c.scaleX = x; c.scaleY = y; c.scaleZ = z; return c;
    }

    /** The point (in Blockbench pixels) that sits on the spawn position and everything turns around. */
    public ModelFx pivot(float x, float y, float z) {
        ModelFx c = copy(); c.pivotX = x; c.pivotY = y; c.pivotZ = z; return c;
    }

    /** Orientation in degrees (yaw = facing like an entity, pitch + = tipped forward, roll + = tipped right). */
    public ModelFx rotation(float yaw, float pitch, float roll) {
        ModelFx c = copy(); c.yaw = yaw; c.pitch = pitch; c.roll = roll; return c;
    }

    /** Add to the current orientation. */
    public ModelFx rotated(float yaw, float pitch, float roll) {
        return rotation(this.yaw + yaw, this.pitch + pitch, this.roll + roll);
    }

    // ---------- timing ----------

    /** Ticks it stays (after the delay). */
    public ModelFx lifetime(int ticks) { ModelFx c = copy(); c.lifetime = Math.max(1, ticks); return c; }

    /** Wait this many ticks before appearing. */
    public ModelFx delay(int ticks) { ModelFx c = copy(); c.delay = Math.max(0, ticks); return c; }

    /** Fade in over the first 'in' ticks and out over the last 'out' ticks (0 = pop). */
    public ModelFx fade(int in, int out) { ModelFx c = copy(); c.fadeIn = Math.max(0, in); c.fadeOut = Math.max(0, out); return c; }

    public ModelFx fadeIn(int ticks) { return fade(ticks, fadeOut); }

    public ModelFx fadeOut(int ticks) { return fade(fadeIn, ticks); }

    /** Keeps turning around its own up axis, degrees per tick (negative = other way). */
    public ModelFx spin(float degreesPerTick) { ModelFx c = copy(); c.spin = degreesPerTick; return c; }

    /** Add a keyframe reached with Ease.LINEAR. */
    public ModelFx key(int tick, Pose pose) { return key(tick, pose, Ease.LINEAR); }

    /** Add a keyframe (replaces one at the same tick). */
    public ModelFx key(int tick, Pose pose, Ease ease) {
        List<Key> list = new ArrayList<>(keys);
        list.removeIf(k -> k.tick() == tick);
        list.add(new Key(Math.max(0, tick), pose, ease));
        list.sort((a, b) -> Integer.compare(a.tick(), b.tick()));
        ModelFx c = copy();
        c.keys = List.copyOf(list);
        return c;
    }

    /** Remove all keyframes (stays still in its rest pose). */
    public ModelFx noKeys() { ModelFx c = copy(); c.keys = List.of(); return c; }

    // ---------- sticking to an entity ----------

    /** Moves along with the entity (offset from its feet) and disappears when the entity dies or unloads. */
    public ModelFx following(Entity entity, Vec3 offset) {
        ModelFx c = copy(); c.followId = entity.getId(); c.followOffset = offset; return c;
    }

    public ModelFx following(Entity entity) { return following(entity, Vec3.ZERO); }

    public ModelFx notFollowing() { ModelFx c = copy(); c.followId = -1; c.followOffset = Vec3.ZERO; return c; }

    // ---------- animation math (client renderer uses this, the server can too) ----------

    /** The keyframed pose 'ticks' after it appeared (partial ticks allowed). */
    public Pose poseAt(float ticks) {
        if (keys.isEmpty()) return Pose.REST;
        Key first = keys.get(0);
        if (ticks <= first.tick()) return first.pose();
        for (int i = 1; i < keys.size(); i++) {
            Key next = keys.get(i);
            if (ticks < next.tick()) {
                Key previous = keys.get(i - 1);
                float span = Math.max(1, next.tick() - previous.tick());
                return previous.pose().lerp(next.pose(), next.ease().apply((ticks - previous.tick()) / span));
            }
        }
        return keys.get(keys.size() - 1).pose();
    }

    /** Opacity from fade in / fade out only (0..1). */
    public float fadeAt(float ticks) {
        float in = fadeIn > 0 ? Mth.clamp(ticks / fadeIn, 0f, 1f) : 1f;
        float out = fadeOut > 0 ? Mth.clamp((lifetime - ticks) / fadeOut, 0f, 1f) : 1f;
        return in * out;
    }

    // ---------- getters ----------

    public ResourceLocation model() { return model; }
    public int tintColor() { return color; }
    public boolean glows() { return glow; }
    public boolean isSeeThrough() { return seeThrough; }
    public int auraColor() { return auraColor; }
    public float auraBlocks() { return auraSize; }
    public int auraLayerCount() { return auraLayers; }
    public float scaleX() { return scaleX; }
    public float scaleY() { return scaleY; }
    public float scaleZ() { return scaleZ; }
    public float pivotX() { return pivotX; }
    public float pivotY() { return pivotY; }
    public float pivotZ() { return pivotZ; }
    public float yaw() { return yaw; }
    public float pitch() { return pitch; }
    public float roll() { return roll; }
    public int lifetimeTicks() { return lifetime; }
    public int delayTicks() { return delay; }
    public int fadeInTicks() { return fadeIn; }
    public int fadeOutTicks() { return fadeOut; }
    public float spinDegrees() { return spin; }
    public int followId() { return followId; }
    public boolean followsEntity() { return followId >= 0; }
    public Vec3 followOffset() { return followOffset; }
    public boolean isClear() { return clear; }
    public List<Key> keys() { return keys; }

    @Override
    public ParticleType<?> getType() {
        return ModParticles.MODEL.get();
    }

    // ---------- saving / networking (you don't need to touch this) ----------

    private static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type) {
        return Codec.STRING.comapFlatMap(
                name -> {
                    try {
                        return DataResult.success(Enum.valueOf(type, name.toUpperCase(Locale.ROOT)));
                    } catch (IllegalArgumentException e) {
                        return DataResult.error(() -> "Unknown " + type.getSimpleName() + ": " + name);
                    }
                },
                value -> value.name().toLowerCase(Locale.ROOT));
    }

    private static final MapCodec<Pose> POSE_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.FLOAT.optionalFieldOf("forward", 0f).forGetter(Pose::forward),
            Codec.FLOAT.optionalFieldOf("up", 0f).forGetter(Pose::up),
            Codec.FLOAT.optionalFieldOf("right", 0f).forGetter(Pose::right),
            Codec.FLOAT.optionalFieldOf("yaw", 0f).forGetter(Pose::yaw),
            Codec.FLOAT.optionalFieldOf("pitch", 0f).forGetter(Pose::pitch),
            Codec.FLOAT.optionalFieldOf("roll", 0f).forGetter(Pose::roll),
            Codec.FLOAT.optionalFieldOf("scale", 1f).forGetter(Pose::scale),
            Codec.FLOAT.optionalFieldOf("alpha", 1f).forGetter(Pose::alpha)
    ).apply(i, Pose::new));

    private static final Codec<Key> KEY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("tick").forGetter(Key::tick),
            POSE_CODEC.forGetter(Key::pose),
            enumCodec(Ease.class).optionalFieldOf("ease", Ease.LINEAR).forGetter(Key::ease)
    ).apply(i, Key::new));

    // The command/NBT format is split into groups only because a codec can hold at most 16 fields;
    // the fields themselves are all at the same level: {model:"...", glow:1b, lifetime:40, ...}
    private record Look(ResourceLocation model, int color, boolean glow, boolean seeThrough,
                        int auraColor, float auraSize, int auraLayers) {}
    private record Shape(Vec3 scale, Vec3 pivot, Vec3 rotation) {}
    private record Timing(int lifetime, int delay, int fadeIn, int fadeOut, float spin) {}
    private record Link(int followId, Vec3 followOffset, boolean clear) {}

    private static final ResourceLocation DEFAULT_MODEL =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, FOLDER + "/order_hammer");

    private static final MapCodec<Look> LOOK_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ResourceLocation.CODEC.optionalFieldOf("model", DEFAULT_MODEL).forGetter(Look::model),
            ParticleFx.COLOR_CODEC.optionalFieldOf("color", 0xFFFFFFFF).forGetter(Look::color),
            Codec.BOOL.optionalFieldOf("glow", false).forGetter(Look::glow),
            Codec.BOOL.optionalFieldOf("see_through", false).forGetter(Look::seeThrough),
            ParticleFx.COLOR_CODEC.optionalFieldOf("aura_color", 0).forGetter(Look::auraColor),
            Codec.FLOAT.optionalFieldOf("aura_size", 0.1f).forGetter(Look::auraSize),
            Codec.INT.optionalFieldOf("aura_layers", 3).forGetter(Look::auraLayers)
    ).apply(i, Look::new));

    private static final MapCodec<Shape> SHAPE_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Vec3.CODEC.optionalFieldOf("scale", new Vec3(1, 1, 1)).forGetter(Shape::scale),
            Vec3.CODEC.optionalFieldOf("pivot", new Vec3(8, 0, 8)).forGetter(Shape::pivot),
            Vec3.CODEC.optionalFieldOf("rotation", Vec3.ZERO).forGetter(Shape::rotation)
    ).apply(i, Shape::new));

    private static final MapCodec<Timing> TIMING_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("lifetime", 20).forGetter(Timing::lifetime),
            Codec.INT.optionalFieldOf("delay", 0).forGetter(Timing::delay),
            Codec.INT.optionalFieldOf("fade_in", 0).forGetter(Timing::fadeIn),
            Codec.INT.optionalFieldOf("fade_out", 0).forGetter(Timing::fadeOut),
            Codec.FLOAT.optionalFieldOf("spin", 0f).forGetter(Timing::spin)
    ).apply(i, Timing::new));

    private static final MapCodec<Link> LINK_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("follow", -1).forGetter(Link::followId),
            Vec3.CODEC.optionalFieldOf("follow_offset", Vec3.ZERO).forGetter(Link::followOffset),
            Codec.BOOL.optionalFieldOf("clear", false).forGetter(Link::clear)
    ).apply(i, Link::new));

    private static ModelFx fromParts(Look look, Shape shape, Timing timing, Link link, List<Key> keys) {
        ModelFx fx = new ModelFx();
        fx.model = look.model(); fx.color = look.color(); fx.glow = look.glow(); fx.seeThrough = look.seeThrough();
        fx.auraColor = look.auraColor(); fx.auraSize = look.auraSize(); fx.auraLayers = Mth.clamp(look.auraLayers(), 1, 6);
        fx.scaleX = (float) shape.scale().x; fx.scaleY = (float) shape.scale().y; fx.scaleZ = (float) shape.scale().z;
        fx.pivotX = (float) shape.pivot().x; fx.pivotY = (float) shape.pivot().y; fx.pivotZ = (float) shape.pivot().z;
        fx.yaw = (float) shape.rotation().x; fx.pitch = (float) shape.rotation().y; fx.roll = (float) shape.rotation().z;
        fx.lifetime = Math.max(1, timing.lifetime()); fx.delay = Math.max(0, timing.delay());
        fx.fadeIn = Math.max(0, timing.fadeIn()); fx.fadeOut = Math.max(0, timing.fadeOut()); fx.spin = timing.spin();
        fx.followId = link.followId(); fx.followOffset = link.followOffset(); fx.clear = link.clear();
        List<Key> sorted = new ArrayList<>(keys);
        sorted.sort((a, b) -> Integer.compare(a.tick(), b.tick()));
        fx.keys = List.copyOf(sorted);
        return fx;
    }

    public static final MapCodec<ModelFx> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            LOOK_CODEC.forGetter((ModelFx fx) -> new Look(fx.model, fx.color, fx.glow, fx.seeThrough,
                    fx.auraColor, fx.auraSize, fx.auraLayers)),
            SHAPE_CODEC.forGetter((ModelFx fx) -> new Shape(new Vec3(fx.scaleX, fx.scaleY, fx.scaleZ),
                    new Vec3(fx.pivotX, fx.pivotY, fx.pivotZ), new Vec3(fx.yaw, fx.pitch, fx.roll))),
            TIMING_CODEC.forGetter((ModelFx fx) -> new Timing(fx.lifetime, fx.delay, fx.fadeIn, fx.fadeOut, fx.spin)),
            LINK_CODEC.forGetter((ModelFx fx) -> new Link(fx.followId, fx.followOffset, fx.clear)),
            KEY_CODEC.listOf().optionalFieldOf("keys", List.of()).forGetter(ModelFx::keys)
    ).apply(i, ModelFx::fromParts));

    public static final StreamCodec<FriendlyByteBuf, ModelFx> STREAM_CODEC =
            StreamCodec.ofMember(ModelFx::write, ModelFx::read);

    private void write(FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(model);
        buffer.writeInt(color);
        buffer.writeBoolean(glow);
        buffer.writeBoolean(seeThrough);
        buffer.writeInt(auraColor);
        buffer.writeFloat(auraSize);
        buffer.writeVarInt(auraLayers);
        buffer.writeFloat(scaleX);
        buffer.writeFloat(scaleY);
        buffer.writeFloat(scaleZ);
        buffer.writeFloat(pivotX);
        buffer.writeFloat(pivotY);
        buffer.writeFloat(pivotZ);
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
        buffer.writeFloat(roll);
        buffer.writeVarInt(lifetime);
        buffer.writeVarInt(delay);
        buffer.writeVarInt(fadeIn);
        buffer.writeVarInt(fadeOut);
        buffer.writeFloat(spin);
        buffer.writeInt(followId);
        buffer.writeDouble(followOffset.x);
        buffer.writeDouble(followOffset.y);
        buffer.writeDouble(followOffset.z);
        buffer.writeBoolean(clear);
        int count = Math.min(keys.size(), MAX_KEYS);
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            Key key = keys.get(i);
            buffer.writeVarInt(key.tick());
            buffer.writeEnum(key.ease());
            Pose p = key.pose();
            buffer.writeFloat(p.forward());
            buffer.writeFloat(p.up());
            buffer.writeFloat(p.right());
            buffer.writeFloat(p.yaw());
            buffer.writeFloat(p.pitch());
            buffer.writeFloat(p.roll());
            buffer.writeFloat(p.scale());
            buffer.writeFloat(p.alpha());
        }
    }

    private static ModelFx read(FriendlyByteBuf buffer) {
        ModelFx fx = new ModelFx();
        fx.model = buffer.readResourceLocation();
        fx.color = buffer.readInt();
        fx.glow = buffer.readBoolean();
        fx.seeThrough = buffer.readBoolean();
        fx.auraColor = buffer.readInt();
        fx.auraSize = buffer.readFloat();
        fx.auraLayers = Mth.clamp(buffer.readVarInt(), 1, 6);
        fx.scaleX = buffer.readFloat();
        fx.scaleY = buffer.readFloat();
        fx.scaleZ = buffer.readFloat();
        fx.pivotX = buffer.readFloat();
        fx.pivotY = buffer.readFloat();
        fx.pivotZ = buffer.readFloat();
        fx.yaw = buffer.readFloat();
        fx.pitch = buffer.readFloat();
        fx.roll = buffer.readFloat();
        fx.lifetime = Math.max(1, buffer.readVarInt());
        fx.delay = buffer.readVarInt();
        fx.fadeIn = buffer.readVarInt();
        fx.fadeOut = buffer.readVarInt();
        fx.spin = buffer.readFloat();
        fx.followId = buffer.readInt();
        fx.followOffset = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        fx.clear = buffer.readBoolean();
        int count = Math.min(buffer.readVarInt(), MAX_KEYS); // safety cap
        List<Key> keys = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int tick = buffer.readVarInt();
            Ease ease = buffer.readEnum(Ease.class);
            Pose pose = new Pose(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
            keys.add(new Key(tick, pose, ease));
        }
        fx.keys = List.copyOf(keys);
        return fx;
    }
}
