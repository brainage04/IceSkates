package com.github.brainage04.iceskates.datagen;

import com.github.brainage04.iceskates.IceSkates;
import com.github.brainage04.iceskates.config.core.ModConfig;
import com.github.brainage04.iceskates.util.StringUtils;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

public class EnglishLangProvider extends FabricLanguageProvider {
    protected EnglishLangProvider(FabricDataOutput dataOutput, CompletableFuture<HolderLookup.Provider> registryLookup) {
        super(dataOutput, registryLookup);
    }

    private final String autoConfigPrefix = "text.autoconfig.%s.option".formatted(IceSkates.MOD_ID);

    private void generateReflectedTranslations(Class<?> clazz, String baseKey, TranslationBuilder translationBuilder) {
        for (Field field : clazz.getFields()) {
            String newBaseKey = "%s.%s".formatted(baseKey, field.getName());

            translationBuilder.add(newBaseKey, StringUtils.pascalCaseToHumanReadable(field.getName()));

            if (field.getType().isPrimitive()) continue;
            if (field.getType().isEnum()) continue;
            if (field.getType() == String.class) continue;

            generateReflectedTranslations(field.getType(), newBaseKey, translationBuilder);
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void addAutomaticTranslations(String[] keys, String packageName, TranslationBuilder translationBuilder) {
        for (String key : keys) {
            translationBuilder.add("%s.%s.%s".formatted(packageName, IceSkates.MOD_ID, key), StringUtils.pascalCaseToHumanReadable(key));
        }
    }

    @Override
    public void generateTranslations(HolderLookup.@NonNull Provider provider, TranslationBuilder translationBuilder) {
        // key categories
        translationBuilder.add(
                "key.category.%s.main".formatted(IceSkates.MOD_ID),
                IceSkates.MOD_NAME
        );

        // keys
        addAutomaticTranslations(
                new String[]{
                        "testKey"
                },
                "key",
                translationBuilder
        );

        // config
        generateReflectedTranslations(ModConfig.class, autoConfigPrefix, translationBuilder);
    }
}
