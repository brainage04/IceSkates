package com.github.brainage04.iceskates.config.core;

import com.github.brainage04.iceskates.IceSkates;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

// final fields will crash the game when saving
// static fields will not save
@SuppressWarnings("CanBeFinal")
@Config(name = IceSkates.MOD_ID)
public class ModConfig implements ConfigData {
    public boolean test = true;
}
