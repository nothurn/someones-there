package com.horrortricks.peripheralfigure;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;
import net.neoforged.neoforge.common.ModConfigSpec.LongValue;

public final class PeripheralFigureConfig {
   private static final Builder BUILDER = new Builder();
   public static final BooleanValue ENABLED = BUILDER.comment("Master switch. If false, the figure never spawns.").define("enabled", true);
   public static final DoubleValue SPAWN_CHANCE_PER_TICK = BUILDER.comment(
         new String[]{
            "Chance, checked once per server tick (20 ticks/second) for each player, that the figure",
            "attempts to spawn near them - gated further by spawn_cooldown_ticks below so this alone",
            "doesn't determine frequency. Default of 0.00005 averages roughly once every 16-17 minutes",
            "of play per player, before the cooldown floor is even considered."
         }
      )
      .defineInRange("spawn_chance_per_tick", 5.0E-5, 0.0, 1.0);
   public static final LongValue SPAWN_COOLDOWN_TICKS = BUILDER.comment(
         new String[]{
            "Minimum ticks that must pass after a figure spawns (whether it was looked at, timed out,",
            "or is still active) before another attempt can succeed for the same player. Default of",
            "6000 is 5 real-world minutes - a floor under spawn_chance_per_tick's randomness, not a",
            "replacement for it."
         }
      )
      .defineInRange("spawn_cooldown_ticks", 6000L, 0L, Long.MAX_VALUE);
   public static final DoubleValue MIN_DISTANCE = BUILDER.comment("Minimum distance (blocks) from the player the figure can be placed.")
      .defineInRange("min_distance", 10.0, 1.0, 128.0);
   public static final DoubleValue MAX_DISTANCE = BUILDER.comment("Maximum distance (blocks) from the player the figure can be placed.")
      .defineInRange("max_distance", 20.0, 1.0, 128.0);
   public static final DoubleValue PERIPHERAL_ANGLE_MIN = BUILDER.comment(
         new String[]{
            "Minimum angular offset (degrees) from the player's exact view-center at the moment of",
            "spawning, applied to a randomly chosen left or right side. Together with",
            "peripheral_angle_max below, this defines the band the figure can appear in - low enough",
            "to stay out of the center of vision, high enough to still be within it (not behind)."
         }
      )
      .defineInRange("peripheral_angle_min", 35.0, 0.0, 179.0);
   public static final DoubleValue PERIPHERAL_ANGLE_MAX = BUILDER.comment("Maximum angular offset (degrees) from view-center - see peripheral_angle_min above.")
      .defineInRange("peripheral_angle_max", 70.0, 0.0, 179.0);
   public static final DoubleValue DISAPPEAR_ANGLE_DEGREES = BUILDER.comment(
         new String[]{
            "The figure is removed the instant the true 3D angle between the player's view direction",
            "and the direction to the figure drops to this many degrees or less - i.e. how precisely",
            "\"looked at directly\" needs to be before it vanishes. Checked every server tick while the",
            "figure exists. Lower = has to be looked at more precisely dead-on before it disappears.",
            "Default of 20 is deliberately forgiving - the player shouldn't need to center their",
            "crosshair with pixel precision for it to count as \"looked at directly\"."
         }
      )
      .defineInRange("disappear_angle_degrees", 20.0, 0.1, 90.0);
   public static final LongValue LOOK_AWAY_TIMEOUT_TICKS = BUILDER.comment(
         new String[]{
            "If the player never looks anywhere near the figure at all, it's removed anyway after", "this many ticks. Default of 200 is 10 real-world seconds."
         }
      )
      .defineInRange("look_away_timeout_ticks", 200L, 1L, Long.MAX_VALUE);
   public static final IntValue MIN_LIGHT_LEVEL = BUILDER.comment(
         new String[]{
            "Minimum combined sky+block light level (0-15, vanilla's usual scale) required at a",
            "candidate spawn position - rejected and retried at a different angle/distance if it's",
            "too dark there to actually perceive the figure even with clear line of sight."
         }
      )
      .defineInRange("min_light_level", 6, 0, 15);
   public static final DoubleValue MIN_DISTANCE_FROM_LAST_SPAWN = BUILDER.comment(
         new String[]{
            "A new spawn candidate is rejected if it's closer than this (blocks) to wherever the",
            "figure last appeared for that same player - persisted across logout/relogin - so it",
            "can't just reappear almost in the same spot. Default of 15 is a good chunk of the",
            "default max_distance (20), without being so large it can rule out every candidate on",
            "small maps/enclosed areas."
         }
      )
      .defineInRange("min_distance_from_last_spawn", 15.0, 0.0, 256.0);
   public static final DoubleValue SECOND_APPEARANCE_CHANCE = BUILDER.comment(
         new String[]{
            "Only rolled when the figure despawns because the player never looked anywhere near it",
            "(the look_away_timeout_ticks path) - NOT when it's looked at directly, and NOT when",
            "line-of-sight gets blocked. On that specific path, this is the chance of scheduling a",
            "second appearance a few minutes later, as if it were the same encounter continuing.",
            "Default of 0.05 is roughly 1-in-20."
         }
      )
      .defineInRange("second_appearance_chance", 0.05, 0.0, 1.0);
   public static final LongValue SECOND_APPEARANCE_DELAY_MIN_TICKS = BUILDER.comment(
         "Minimum delay before a scheduled second appearance attempts to spawn. Default of 2400 is 2 real-world minutes."
      )
      .defineInRange("second_appearance_delay_min_ticks", 2400L, 1L, Long.MAX_VALUE);
   public static final LongValue SECOND_APPEARANCE_DELAY_MAX_TICKS = BUILDER.comment(
         "Maximum delay before a scheduled second appearance attempts to spawn. Default of 6000 is 5 real-world minutes."
      )
      .defineInRange("second_appearance_delay_max_ticks", 6000L, 1L, Long.MAX_VALUE);
   public static final DoubleValue ALONE_SPAWN_CHANCE_MULTIPLIER = BUILDER.comment(
         new String[]{
            "spawn_chance_per_tick is multiplied by this while the player is alone (singleplayer, or",
            "the only player currently online on a server) - default of 3.0 makes the figure noticeably",
            "more frequent when there's nobody else around."
         }
      )
      .defineInRange("alone_spawn_chance_multiplier", 3.0, 0.1, 100.0);
   public static final DoubleValue ALONE_COOLDOWN_MULTIPLIER = BUILDER.comment(
         "spawn_cooldown_ticks is multiplied by this while alone - default of 0.5 halves the cooldown floor."
      )
      .defineInRange("alone_cooldown_multiplier", 0.5, 0.01, 10.0);
   public static final DoubleValue ALONE_DISTANCE_MULTIPLIER = BUILDER.comment(
         "min_distance and max_distance are both multiplied by this while alone - default of 0.85 brings the figure noticeably closer."
      )
      .defineInRange("alone_distance_multiplier", 0.85, 0.1, 2.0);
   public static final BooleanValue LIGHTNING_SILHOUETTE_ENABLED = BUILDER.comment(
         "Whether the figure can ever appear as a brief dark silhouette during a lightning flash."
      )
      .define("lightning_silhouette_enabled", true);
   public static final DoubleValue LIGHTNING_SILHOUETTE_CHANCE = BUILDER.comment(
         new String[]{
            "Chance, rolled once per real nearby lightning strike (see lightning_silhouette_detection_radius",
            "below), that this specific strike also produces a silhouette appearance. Independent of",
            "spawn_chance_per_tick/spawn_cooldown_ticks - this is its own separate, rarer trigger."
         }
      )
      .defineInRange("lightning_silhouette_chance", 0.15, 0.0, 1.0);
   public static final IntValue LIGHTNING_SILHOUETTE_LIFETIME_TICKS = BUILDER.comment(
         "How long the silhouette stays visible - roughly the duration of a lightning flash. Default of 5 is 0.25 seconds."
      )
      .defineInRange("lightning_silhouette_lifetime_ticks", 5, 1, 200);
   public static final DoubleValue LIGHTNING_SILHOUETTE_MIN_DISTANCE = BUILDER.comment(
         "Minimum distance (blocks) the silhouette can appear at - a middle distance, not right next to the player."
      )
      .defineInRange("lightning_silhouette_min_distance", 15.0, 1.0, 128.0);
   public static final DoubleValue LIGHTNING_SILHOUETTE_MAX_DISTANCE = BUILDER.comment("Maximum distance (blocks) the silhouette can appear at.")
      .defineInRange("lightning_silhouette_max_distance", 30.0, 1.0, 128.0);
   public static final DoubleValue LIGHTNING_SILHOUETTE_VIEW_ANGLE_MAX = BUILDER.comment(
         new String[]{
            "Maximum angular offset (degrees) from the player's exact view-center the silhouette can be",
            "placed at - unlike the normal figure's peripheral-vision band, this one has to actually be",
            "in view (visible during the flash), so this should stay well inside a normal field of view."
         }
      )
      .defineInRange("lightning_silhouette_view_angle_max", 30.0, 0.0, 90.0);
   public static final DoubleValue LIGHTNING_SILHOUETTE_DETECTION_RADIUS = BUILDER.comment(
         "How close (blocks) an actual lightning strike has to land to the player for it to be eligible to also produce a silhouette."
      )
      .defineInRange("lightning_silhouette_detection_radius", 64.0, 1.0, 256.0);
   public static final BooleanValue LINGER_VARIANT_ENABLED = BUILDER.comment("Whether the very rare \"doesn't vanish immediately\" variant can ever occur.")
      .define("linger_variant_enabled", true);
   public static final DoubleValue LINGER_VARIANT_CHANCE = BUILDER.comment(
         new String[]{
            "Chance, rolled once per normal figure spawn (subject to linger_variant_cooldown_ticks",
            "below), that this particular appearance is the linger variant. Kept very low by default -",
            "this is meant to be a rare, memorable exception, not a regular occurrence."
         }
      )
      .defineInRange("linger_variant_chance", 0.02, 0.0, 1.0);
   public static final IntValue LINGER_VARIANT_DELAY_TICKS = BUILDER.comment(
         "How long the linger variant stays visible under a direct look before finally vanishing. Default of 10 is 0.5 seconds."
      )
      .defineInRange("linger_variant_delay_ticks", 10, 1, 200);
   public static final LongValue LINGER_VARIANT_COOLDOWN_TICKS = BUILDER.comment(
         new String[]{
            "Minimum ticks that must pass after one linger-variant appearance before another can be",
            "rolled for the same player - independent of, and much longer than, spawn_cooldown_ticks.",
            "Default of 216000 is 3 real-world hours of played time."
         }
      )
      .defineInRange("linger_variant_cooldown_ticks", 216000L, 20L, Long.MAX_VALUE);
   public static final ModConfigSpec SPEC = BUILDER.build();

   private PeripheralFigureConfig() {
   }

   static {
      BUILDER.push("general");
      BUILDER.pop();
   }
}
