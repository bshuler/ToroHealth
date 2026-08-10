package net.torocraft.torohealth.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.torocraft.torohealth.ToroHealth;
import net.torocraft.torohealth.config.Config;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private final Config config;

    private EditBox hudDistanceBox;
    private EditBox hudXBox;
    private EditBox hudYBox;
    private EditBox hudScaleBox;
    private EditBox hudHideDelayBox;
    private Checkbox showEntityCheckbox;
    private Checkbox showBarCheckbox;
    private Checkbox showSkinCheckbox;
    private Checkbox onlyWhenHurtCheckbox;

    private Checkbox particleShowCheckbox;
    private EditBox particleDistanceBox;

    private EditBox inWorldDistanceBox;
    private Checkbox onlyWhenLookingAtCheckbox;
    private Checkbox inWorldOnlyWhenHurtCheckbox;

    private Button inWorldModeButton;
    private Button damageNumberTypeButton;
    private Button anchorPointButton;

    public ConfigScreen(Screen parent) {
        super(Component.literal("ToroHealth Configuration"));
        this.parent = parent;
        this.config = ToroHealth.CONFIG;
    }

    @Override
    protected void init() {
        int col1 = this.width / 6;
        int col2 = this.width / 2;
        int col3 = this.width - this.width / 6;
        int topRow = 25;
        int spacing = 18;
        int sectionSpacing = 10;

        this.addRenderableWidget(Button.builder(Component.literal("HUD Settings"),
            button -> {}).bounds(col1 - 40, topRow - 12, 80, 12).build()).active = false;

        hudDistanceBox = new EditBox(this.font, col1 - 20, topRow, 40, 16, Component.literal("HUD Distance"));
        hudDistanceBox.setValue(String.valueOf(config.hud.distance));
        this.addRenderableWidget(hudDistanceBox);

        topRow += spacing;
        hudXBox = new EditBox(this.font, col1 - 20, topRow, 40, 16, Component.literal("HUD X Position"));
        hudXBox.setValue(String.valueOf(config.hud.x));
        this.addRenderableWidget(hudXBox);

        topRow += spacing;
        hudYBox = new EditBox(this.font, col1 - 20, topRow, 40, 16, Component.literal("HUD Y Position"));
        hudYBox.setValue(String.valueOf(config.hud.y));
        this.addRenderableWidget(hudYBox);

        topRow += spacing;
        hudScaleBox = new EditBox(this.font, col1 - 20, topRow, 40, 16, Component.literal("HUD Scale"));
        hudScaleBox.setValue(String.valueOf(config.hud.scale));
        this.addRenderableWidget(hudScaleBox);

        topRow += spacing;
        hudHideDelayBox = new EditBox(this.font, col1 - 20, topRow, 40, 16, Component.literal("Hide Delay"));
        hudHideDelayBox.setValue(String.valueOf(config.hud.hideDelay));
        this.addRenderableWidget(hudHideDelayBox);

        topRow += spacing;
        anchorPointButton = this.addRenderableWidget(Button.builder(
            Component.literal("Anchor: " + config.hud.anchorPoint.name()),
            button -> {
                Config.AnchorPoint[] values = Config.AnchorPoint.values();
                int currentIndex = config.hud.anchorPoint.ordinal();
                int nextIndex = (currentIndex + 1) % values.length;
                config.hud.anchorPoint = values[nextIndex];
                button.setMessage(Component.literal("Anchor: " + config.hud.anchorPoint.name()));
            }
        ).bounds(col1 - 40, topRow, 80, 18).build());

        int col2Row = 25;
        this.addRenderableWidget(Button.builder(Component.literal("HUD Options"),
            button -> {}).bounds(col2 - 40, col2Row - 12, 80, 12).build()).active = false;

        showEntityCheckbox = this.addRenderableWidget(
            Checkbox.builder(Component.literal("Show Entity"), this.font)
                .selected(config.hud.showEntity)
                .build());
        showEntityCheckbox.setPosition(col2 - 40, col2Row);

        col2Row += spacing;
        showBarCheckbox = this.addRenderableWidget(
            Checkbox.builder(Component.literal("Show Health Bar"), this.font)
                .selected(config.hud.showBar)
                .build());
        showBarCheckbox.setPosition(col2 - 40, col2Row);

        col2Row += spacing;
        showSkinCheckbox = this.addRenderableWidget(
            Checkbox.builder(Component.literal("Show Skin"), this.font)
                .selected(config.hud.showSkin)
                .build());
        showSkinCheckbox.setPosition(col2 - 40, col2Row);

        col2Row += spacing;
        onlyWhenHurtCheckbox = this.addRenderableWidget(
            Checkbox.builder(Component.literal("Only When Hurt"), this.font)
                .selected(config.hud.onlyWhenHurt)
                .build());
        onlyWhenHurtCheckbox.setPosition(col2 - 40, col2Row);

        col2Row += spacing + sectionSpacing;
        this.addRenderableWidget(Button.builder(Component.literal("Particles"),
            button -> {}).bounds(col2 - 40, col2Row - 12, 80, 12).build()).active = false;

        particleShowCheckbox = this.addRenderableWidget(
            Checkbox.builder(Component.literal("Show Particles"), this.font)
                .selected(config.particle.show)
                .build());
        particleShowCheckbox.setPosition(col2 - 40, col2Row);

        col2Row += spacing;
        particleDistanceBox = new EditBox(this.font, col2 - 20, col2Row, 40, 16, Component.literal("Particle Distance"));
        particleDistanceBox.setValue(String.valueOf(config.particle.distance));
        this.addRenderableWidget(particleDistanceBox);

        int col3Row = 25;
        this.addRenderableWidget(Button.builder(Component.literal("In-World"),
            button -> {}).bounds(col3 - 40, col3Row - 12, 80, 12).build()).active = false;

        inWorldModeButton = this.addRenderableWidget(Button.builder(
            Component.literal("Mode: " + config.inWorld.mode.name()),
            button -> {
                Config.Mode[] values = Config.Mode.values();
                int currentIndex = config.inWorld.mode.ordinal();
                int nextIndex = (currentIndex + 1) % values.length;
                config.inWorld.mode = values[nextIndex];
                button.setMessage(Component.literal("Mode: " + config.inWorld.mode.name()));
            }
        ).bounds(col3 - 40, col3Row, 80, 18).build());

        col3Row += spacing;
        inWorldDistanceBox = new EditBox(this.font, col3 - 20, col3Row, 40, 16, Component.literal("In-World Distance"));
        inWorldDistanceBox.setValue(String.valueOf(config.inWorld.distance));
        this.addRenderableWidget(inWorldDistanceBox);

        col3Row += spacing;
        onlyWhenLookingAtCheckbox = this.addRenderableWidget(
            Checkbox.builder(Component.literal("Only When Looking At"), this.font)
                .selected(config.inWorld.onlyWhenLookingAt)
                .build());
        onlyWhenLookingAtCheckbox.setPosition(col3 - 40, col3Row);

        col3Row += spacing;
        inWorldOnlyWhenHurtCheckbox = this.addRenderableWidget(
            Checkbox.builder(Component.literal("Only When Hurt"), this.font)
                .selected(config.inWorld.onlyWhenHurt)
                .build());
        inWorldOnlyWhenHurtCheckbox.setPosition(col3 - 40, col3Row);

        col3Row += spacing;
        damageNumberTypeButton = this.addRenderableWidget(Button.builder(
            Component.literal("Damage Numbers: " + config.bar.damageNumberType.name()),
            button -> {
                Config.NumberType[] values = Config.NumberType.values();
                int currentIndex = config.bar.damageNumberType.ordinal();
                int nextIndex = (currentIndex + 1) % values.length;
                config.bar.damageNumberType = values[nextIndex];
                button.setMessage(Component.literal("Damage Numbers: " + config.bar.damageNumberType.name()));
            }
        ).bounds(col3 - 40, col3Row, 80, 18).build());

        int maxRow = Math.max(topRow, Math.max(col2Row, col3Row));

        int minBottomY = Math.max(maxRow + 35, this.height - 50);
        this.addRenderableWidget(Button.builder(Component.literal("Save"), button -> this.save())
            .bounds(this.width / 2 - 155, minBottomY, 70, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> {
            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        }).bounds(this.width / 2 - 75, minBottomY, 70, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Reset to Defaults"), button -> this.resetToDefaults())
            .bounds(this.width / 2 + 5, minBottomY, 120, 20).build());
    }

    private void save() {
        try {
            config.hud.distance = Integer.parseInt(hudDistanceBox.getValue());
            config.hud.x = Float.parseFloat(hudXBox.getValue());
            config.hud.y = Float.parseFloat(hudYBox.getValue());
            config.hud.scale = Float.parseFloat(hudScaleBox.getValue());
            config.hud.hideDelay = Integer.parseInt(hudHideDelayBox.getValue());
            config.hud.showEntity = showEntityCheckbox.selected();
            config.hud.showBar = showBarCheckbox.selected();
            config.hud.showSkin = showSkinCheckbox.selected();
            config.hud.onlyWhenHurt = onlyWhenHurtCheckbox.selected();

            config.particle.show = particleShowCheckbox.selected();
            config.particle.distance = Integer.parseInt(particleDistanceBox.getValue());

            config.inWorld.distance = Float.parseFloat(inWorldDistanceBox.getValue());
            config.inWorld.onlyWhenLookingAt = onlyWhenLookingAtCheckbox.selected();
            config.inWorld.onlyWhenHurt = inWorldOnlyWhenHurtCheckbox.selected();

            config.update();

            ToroHealth.saveConfig();

            if (this.minecraft != null) {
                this.minecraft.setScreen(this.parent);
            }
        } catch (NumberFormatException e) {
            ToroHealth.LOGGER.error("Invalid number format in config screen", e);
        }
    }

    private void resetToDefaults() {
        Config defaultConfig = new Config();

        hudDistanceBox.setValue(String.valueOf(defaultConfig.hud.distance));
        hudXBox.setValue(String.valueOf(defaultConfig.hud.x));
        hudYBox.setValue(String.valueOf(defaultConfig.hud.y));
        hudScaleBox.setValue(String.valueOf(defaultConfig.hud.scale));
        hudHideDelayBox.setValue(String.valueOf(defaultConfig.hud.hideDelay));

        particleDistanceBox.setValue(String.valueOf(defaultConfig.particle.distance));

        inWorldDistanceBox.setValue(String.valueOf(defaultConfig.inWorld.distance));

        config.hud.anchorPoint = defaultConfig.hud.anchorPoint;
        config.inWorld.mode = defaultConfig.inWorld.mode;
        config.bar.damageNumberType = defaultConfig.bar.damageNumberType;

        anchorPointButton.setMessage(Component.literal("Anchor: " + config.hud.anchorPoint.name()));
        inWorldModeButton.setMessage(Component.literal("Mode: " + config.inWorld.mode.name()));
        damageNumberTypeButton.setMessage(Component.literal("Damage Numbers: " + config.bar.damageNumberType.name()));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int col1 = this.width / 6;
        int col2 = this.width / 2;
        int col3 = this.width - this.width / 6;

        guiGraphics.drawString(this.font, "Distance:", col1 - 55, 30, 0xFFFFFF);
        guiGraphics.drawString(this.font, "X Pos:", col1 - 55, 48, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Y Pos:", col1 - 55, 66, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Scale:", col1 - 55, 84, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Delay:", col1 - 55, 102, 0xFFFFFF);

        guiGraphics.drawString(this.font, "Distance:", col2 - 55, 120, 0xFFFFFF);

        guiGraphics.drawString(this.font, "Distance:", col3 - 55, 43, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
