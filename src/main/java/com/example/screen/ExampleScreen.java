package com.example.screen;

import com.example.ExampleMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class ExampleScreen extends Screen {
    private static final int PADDING = 2;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen parent;

    public Button button;

    public ExampleScreen(Screen parent) {
        super(Component.literal("Example Screen"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        button = Button.builder(Component.literal("Button"), button -> ExampleMod.LOGGER.info("You clicked a button!"))
                .bounds((width - BUTTON_WIDTH) / 2, height * 3 / 4, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.literal("This is a tooltip.")))
                .build();

        // addWidget adds the functionality (on button click) but does not render the button
        // addRenderable renders the button but does not add the functionality
        // addRenderableWidget adds both
        this.addRenderableWidget(button);
    }

    @Override
    public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float deltaTicks) {
        super.render(guiGraphics, mouseX, mouseY, deltaTicks);

        guiGraphics.drawStringWithBackdrop(
                this.font,
                title,
                width / 2,
                this.font.lineHeight + PADDING,
                10,
                10
        );
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }
}