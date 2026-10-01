package io.github.brainage04.ice_skates;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class IceSkatesGameTest {
    @GameTest
    public void skatesAndSwordsAreRegisteredWithDyeRecipes(GameTestHelper context) {
        IceSkatesGameTests.skatesAndSwordsAreRegisteredWithDyeRecipes(context);
    }
}
