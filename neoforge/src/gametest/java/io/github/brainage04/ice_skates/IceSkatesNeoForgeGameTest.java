package io.github.brainage04.ice_skates;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Registers the shared GameTests as NeoForge test functions. Each function needs a matching
 * {@code data/<mod_id>/test_instance/<name>.json} in this source set's resources.
 */
@EventBusSubscriber(modid = IceSkatesCommon.MOD_ID)
public final class IceSkatesNeoForgeGameTest {
    private IceSkatesNeoForgeGameTest() { }

    @SubscribeEvent
    public static void registerTestFunctions(RegisterEvent event) {
        event.register(BuiltInRegistries.TEST_FUNCTION.key(),
                Identifier.fromNamespaceAndPath(IceSkatesCommon.MOD_ID, "skates_and_swords_are_registered_with_dye_recipes"),
                () -> IceSkatesGameTests::skatesAndSwordsAreRegisteredWithDyeRecipes);
        event.register(BuiltInRegistries.TEST_FUNCTION.key(),
                IceSkatesCommon.id("skating_controls_and_friction"),
                () -> IceSkatesGameTests::skatingControlsAndFriction);
        event.register(BuiltInRegistries.TEST_FUNCTION.key(),
                IceSkatesCommon.id("exhaustion_follows_skating_state"),
                () -> IceSkatesGameTests::exhaustionFollowsSkatingState);
        event.register(BuiltInRegistries.TEST_FUNCTION.key(),
                IceSkatesCommon.id("skates_can_be_dyed_and_washed"),
                () -> IceSkatesGameTests::skatesCanBeDyedAndWashed);
    }
}
