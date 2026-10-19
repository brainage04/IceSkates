package io.github.brainage04.ice_skates;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class IceSkatesGameTest {
    @GameTest
    public void skatesAndSwordsAreRegisteredWithDyeRecipes(GameTestHelper context) {
        IceSkatesGameTests.skatesAndSwordsAreRegisteredWithDyeRecipes(context);
    }

    @GameTest
    public void skatingControlsAndFriction(GameTestHelper context) {
        IceSkatesGameTests.skatingControlsAndFriction(context);
    }

    @GameTest
    public void exhaustionFollowsSkatingState(GameTestHelper context) {
        IceSkatesGameTests.exhaustionFollowsSkatingState(context);
    }

    @GameTest
    public void skatesCanBeDyedAndWashed(GameTestHelper context) {
        IceSkatesGameTests.skatesCanBeDyedAndWashed(context);
    }

    @GameTest
    public void combatAdvancementsSeparateMeleeAndArrows(GameTestHelper context) {
        IceSkatesGameTests.combatAdvancementsSeparateMeleeAndArrows(context);
    }
}
