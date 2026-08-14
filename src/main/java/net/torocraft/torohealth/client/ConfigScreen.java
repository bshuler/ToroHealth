package net.torocraft.torohealth.client;

//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiComponent;
*///?} elif >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
//? if <1.19 {
/*import net.minecraft.network.chat.TextComponent;
*///?}
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
        super(text("ToroHealth Configuration"));
        this.parent = parent;
        this.config = ToroHealth.CONFIG;
    }

    // Minecraft.setScreen(Screen) was replaced by setScreenAndShow(Screen) in
    // 26.2 (confirmed via javap - no setScreen(Screen) survivor at all);
    // pulled into one helper since this mod calls it from three places.
    private void returnToParent() {
        if (this.minecraft == null) {
            return;
        }
        //? if >=26.1 {
        /*this.minecraft.setScreenAndShow(this.parent);
        *///?} else {
        this.minecraft.setScreen(this.parent);
        //?}
    }

    // Component.literal(String) is a >=1.19-era static factory; 1.18.2 only
    // has TextComponent's public constructor (removed in favor of the
    // factory method above starting 1.19).
    private static Component text(String s) {
        //? if <1.19 {
        /*return new TextComponent(s);
        *///?} else {
        return Component.literal(s);
        //?}
    }

    // Checkbox.builder(Component, Font) is a >=1.20.2-era API and its
    // resulting Checkbox supports setPosition(int, int) same as the old
    // direct constructor's instance does on 1.19.4/1.20.1. 1.18.2's Checkbox
    // predates setPosition entirely, so position must go straight into the
    // constructor there instead - hence this takes x/y up front rather than
    // leaving callers to setPosition afterward.
    private Checkbox makeCheckbox(int x, int y, Component message, boolean selected) {
        //? if >=1.20.2 {
        Checkbox checkbox = Checkbox.builder(message, this.font).selected(selected).build();
        checkbox.setPosition(x, y);
        return checkbox;
        //?} elif >=1.19 {
        /*Checkbox checkbox = new Checkbox(0, 0, 20, 20, message, selected);
        checkbox.setPosition(x, y);
        return checkbox;
        *///?} else {
        /*return new Checkbox(x, y, 20, 20, message, selected);
        *///?}
    }

    // Button.builder(Component, OnPress) is a >=1.19-era static factory;
    // 1.18.2 only has Button's direct constructor (x, y, width, height,
    // message, onPress) - Button.OnPress is the same functional interface
    // either way, so every call-site lambda below works unchanged.
    private Button button(Component message, int x, int y, int w, int h, Button.OnPress onPress) {
        //? if <1.19 {
        /*return new Button(x, y, w, h, message, onPress);
        *///?} else {
        return Button.builder(message, onPress).bounds(x, y, w, h).build();
        //?}
    }

    @Override
    protected void init() {
        int col1 = this.width / 6;
        int col2 = this.width / 2;
        int col3 = this.width - this.width / 6;
        int topRow = 25;
        int spacing = 18;
        int sectionSpacing = 10;

        this.addRenderableWidget(
            button(text("HUD Settings"), col1 - 40, topRow - 12, 80, 12, button -> {})).active = false;

        hudDistanceBox = new EditBox(this.font, col1 - 20, topRow, 40, 16, text("HUD Distance"));
        hudDistanceBox.setValue(String.valueOf(config.hud.distance));
        this.addRenderableWidget(hudDistanceBox);

        topRow += spacing;
        hudXBox = new EditBox(this.font, col1 - 20, topRow, 40, 16, text("HUD X Position"));
        hudXBox.setValue(String.valueOf(config.hud.x));
        this.addRenderableWidget(hudXBox);

        topRow += spacing;
        hudYBox = new EditBox(this.font, col1 - 20, topRow, 40, 16, text("HUD Y Position"));
        hudYBox.setValue(String.valueOf(config.hud.y));
        this.addRenderableWidget(hudYBox);

        topRow += spacing;
        hudScaleBox = new EditBox(this.font, col1 - 20, topRow, 40, 16, text("HUD Scale"));
        hudScaleBox.setValue(String.valueOf(config.hud.scale));
        this.addRenderableWidget(hudScaleBox);

        topRow += spacing;
        hudHideDelayBox = new EditBox(this.font, col1 - 20, topRow, 40, 16, text("Hide Delay"));
        hudHideDelayBox.setValue(String.valueOf(config.hud.hideDelay));
        this.addRenderableWidget(hudHideDelayBox);

        topRow += spacing;
        anchorPointButton = this.addRenderableWidget(button(
            text("Anchor: " + config.hud.anchorPoint.name()), col1 - 40, topRow, 80, 18,
            button -> {
                Config.AnchorPoint[] values = Config.AnchorPoint.values();
                int currentIndex = config.hud.anchorPoint.ordinal();
                int nextIndex = (currentIndex + 1) % values.length;
                config.hud.anchorPoint = values[nextIndex];
                button.setMessage(text("Anchor: " + config.hud.anchorPoint.name()));
            }
        ));

        int col2Row = 25;
        this.addRenderableWidget(
            button(text("HUD Options"), col2 - 40, col2Row - 12, 80, 12, button -> {})).active = false;

        showEntityCheckbox = this.addRenderableWidget(
            makeCheckbox(col2 - 40, col2Row, text("Show Entity"), config.hud.showEntity));

        col2Row += spacing;
        showBarCheckbox = this.addRenderableWidget(
            makeCheckbox(col2 - 40, col2Row, text("Show Health Bar"), config.hud.showBar));

        col2Row += spacing;
        showSkinCheckbox = this.addRenderableWidget(
            makeCheckbox(col2 - 40, col2Row, text("Show Skin"), config.hud.showSkin));

        col2Row += spacing;
        onlyWhenHurtCheckbox = this.addRenderableWidget(
            makeCheckbox(col2 - 40, col2Row, text("Only When Hurt"), config.hud.onlyWhenHurt));

        col2Row += spacing + sectionSpacing;
        this.addRenderableWidget(
            button(text("Particles"), col2 - 40, col2Row - 12, 80, 12, button -> {})).active = false;

        particleShowCheckbox = this.addRenderableWidget(
            makeCheckbox(col2 - 40, col2Row, text("Show Particles"), config.particle.show));

        col2Row += spacing;
        particleDistanceBox = new EditBox(this.font, col2 - 20, col2Row, 40, 16, text("Particle Distance"));
        particleDistanceBox.setValue(String.valueOf(config.particle.distance));
        this.addRenderableWidget(particleDistanceBox);

        int col3Row = 25;
        this.addRenderableWidget(
            button(text("In-World"), col3 - 40, col3Row - 12, 80, 12, button -> {})).active = false;

        inWorldModeButton = this.addRenderableWidget(button(
            text("Mode: " + config.inWorld.mode.name()), col3 - 40, col3Row, 80, 18,
            button -> {
                Config.Mode[] values = Config.Mode.values();
                int currentIndex = config.inWorld.mode.ordinal();
                int nextIndex = (currentIndex + 1) % values.length;
                config.inWorld.mode = values[nextIndex];
                button.setMessage(text("Mode: " + config.inWorld.mode.name()));
            }
        ));

        col3Row += spacing;
        inWorldDistanceBox = new EditBox(this.font, col3 - 20, col3Row, 40, 16, text("In-World Distance"));
        inWorldDistanceBox.setValue(String.valueOf(config.inWorld.distance));
        this.addRenderableWidget(inWorldDistanceBox);

        col3Row += spacing;
        onlyWhenLookingAtCheckbox = this.addRenderableWidget(
            makeCheckbox(col3 - 40, col3Row, text("Only When Looking At"), config.inWorld.onlyWhenLookingAt));

        col3Row += spacing;
        inWorldOnlyWhenHurtCheckbox = this.addRenderableWidget(
            makeCheckbox(col3 - 40, col3Row, text("Only When Hurt"), config.inWorld.onlyWhenHurt));

        col3Row += spacing;
        damageNumberTypeButton = this.addRenderableWidget(button(
            text("Damage Numbers: " + config.bar.damageNumberType.name()), col3 - 40, col3Row, 80, 18,
            button -> {
                Config.NumberType[] values = Config.NumberType.values();
                int currentIndex = config.bar.damageNumberType.ordinal();
                int nextIndex = (currentIndex + 1) % values.length;
                config.bar.damageNumberType = values[nextIndex];
                button.setMessage(text("Damage Numbers: " + config.bar.damageNumberType.name()));
            }
        ));

        int maxRow = Math.max(topRow, Math.max(col2Row, col3Row));

        int minBottomY = Math.max(maxRow + 35, this.height - 50);
        this.addRenderableWidget(
            button(text("Save"), this.width / 2 - 155, minBottomY, 70, 20, button -> this.save()));

        this.addRenderableWidget(button(text("Cancel"), this.width / 2 - 75, minBottomY, 70, 20,
            button -> returnToParent()));

        this.addRenderableWidget(button(text("Reset to Defaults"), this.width / 2 + 5, minBottomY, 120, 20,
            button -> this.resetToDefaults()));
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

            returnToParent();
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

        anchorPointButton.setMessage(text("Anchor: " + config.hud.anchorPoint.name()));
        inWorldModeButton.setMessage(text("Mode: " + config.inWorld.mode.name()));
        damageNumberTypeButton.setMessage(text("Damage Numbers: " + config.bar.damageNumberType.name()));
    }

    @Override
    //? if <1.20 {
    /*public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);

        int col1 = this.width / 6;
        int col2 = this.width / 2;
        int col3 = this.width - this.width / 6;

        GuiComponent.drawString(poseStack, this.font, "Distance:", col1 - 55, 30, 0xFFFFFFFF);
        GuiComponent.drawString(poseStack, this.font, "X Pos:", col1 - 55, 48, 0xFFFFFFFF);
        GuiComponent.drawString(poseStack, this.font, "Y Pos:", col1 - 55, 66, 0xFFFFFFFF);
        GuiComponent.drawString(poseStack, this.font, "Scale:", col1 - 55, 84, 0xFFFFFFFF);
        GuiComponent.drawString(poseStack, this.font, "Delay:", col1 - 55, 102, 0xFFFFFFFF);

        GuiComponent.drawString(poseStack, this.font, "Distance:", col2 - 55, 120, 0xFFFFFFFF);

        GuiComponent.drawString(poseStack, this.font, "Distance:", col3 - 55, 43, 0xFFFFFFFF);
    }
    *///?} elif >=26.1 {
    /*// Screen.render(GuiGraphics, ...) was renamed/restructured into
    // extractRenderState(GuiGraphicsExtractor, ...) in 26.2 (confirmed via
    // javap - no render(GuiGraphics,...) survivor on Screen at all), matching
    // the same GuiGraphics->GuiGraphicsExtractor + drawString->text rename
    // already applied throughout this port (see PlatformHudCanvas).
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
        float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        int col1 = this.width / 6;
        int col2 = this.width / 2;
        int col3 = this.width - this.width / 6;

        guiGraphics.text(this.font, "Distance:", col1 - 55, 30, 0xFFFFFFFF);
        guiGraphics.text(this.font, "X Pos:", col1 - 55, 48, 0xFFFFFFFF);
        guiGraphics.text(this.font, "Y Pos:", col1 - 55, 66, 0xFFFFFFFF);
        guiGraphics.text(this.font, "Scale:", col1 - 55, 84, 0xFFFFFFFF);
        guiGraphics.text(this.font, "Delay:", col1 - 55, 102, 0xFFFFFFFF);

        guiGraphics.text(this.font, "Distance:", col2 - 55, 120, 0xFFFFFFFF);

        guiGraphics.text(this.font, "Distance:", col3 - 55, 43, 0xFFFFFFFF);
    }
    *///?} else {
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int col1 = this.width / 6;
        int col2 = this.width / 2;
        int col3 = this.width - this.width / 6;

        guiGraphics.drawString(this.font, "Distance:", col1 - 55, 30, 0xFFFFFFFF);
        guiGraphics.drawString(this.font, "X Pos:", col1 - 55, 48, 0xFFFFFFFF);
        guiGraphics.drawString(this.font, "Y Pos:", col1 - 55, 66, 0xFFFFFFFF);
        guiGraphics.drawString(this.font, "Scale:", col1 - 55, 84, 0xFFFFFFFF);
        guiGraphics.drawString(this.font, "Delay:", col1 - 55, 102, 0xFFFFFFFF);

        guiGraphics.drawString(this.font, "Distance:", col2 - 55, 120, 0xFFFFFFFF);

        guiGraphics.drawString(this.font, "Distance:", col3 - 55, 43, 0xFFFFFFFF);
    }
    //?}

    @Override
    public void onClose() {
        returnToParent();
    }
}
