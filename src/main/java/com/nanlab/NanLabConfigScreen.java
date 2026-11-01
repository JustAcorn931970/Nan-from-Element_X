package com.nanlab;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class NanLabConfigScreen extends Screen {
    private final Screen parent;

    public NanLabConfigScreen(Screen parent) {
        super(Component.literal("NaN Lab"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int buttonWidth = Math.min(220, (this.width - 54) / 2);
        int gap = 12;
        int left = (this.width - (buttonWidth * 2 + gap)) / 2;
        int right = left + buttonWidth + gap;
        int top = 92;
        int step = 27;

        addActionButton(left, top, buttonWidth, "Position → NaN", "/nan set position nan");
        addActionButton(right, top, buttonWidth, "Motion → NaN", "/nan set motion nan");
        addActionButton(left, top + step, buttonWidth, "Rotation → NaN", "/nan set rotation nan");
        addActionButton(right, top + step, buttonWidth, "Fall distance → NaN", "/nan set fall_distance nan");
        addActionButton(left, top + step * 2, buttonWidth, "Walk / fly speed → NaN", "/nan set speeds nan");
        addActionButton(right, top + step * 2, buttonWidth, "Attributes → NaN", "/nan set attributes nan");
        addActionButton(left, top + step * 3, buttonWidth, "Health / absorption → NaN", "/nan set vitals nan");
        addActionButton(right, top + step * 3, buttonWidth, "Hunger → NaN", "/nan set hunger nan");
        addActionButton(left, top + step * 4, buttonWidth, "XP progress → NaN", "/nan set xp_progress nan");
        addActionButton(right, top + step * 4, buttonWidth, "EVERYTHING → NaN", "/nan set all nan");

        this.addRenderableWidget(Button.builder(Component.literal("Reset player safely"), button -> sendCommand("nan reset"))
            .bounds(left, top + step * 5 + 4, buttonWidth * 2 + gap, 20)
            .build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> this.onClose())
            .bounds((this.width - 100) / 2, this.height - 32, 100, 20)
            .build());
    }

    private void addActionButton(int x, int y, int width, String label, String command) {
        this.addRenderableWidget(Button.builder(Component.literal(label), button -> sendCommand(command.substring(1)))
            .bounds(x, y, width, 20)
            .build());
    }

    private void sendCommand(String command) {
        Minecraft client = this.minecraft;
        if (client == null || client.player == null || client.player.connection == null) {
            return;
        }
        // The server performs the mutation and enforces the command permission level.
        client.player.connection.sendCommand(command);
        client.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 30, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "NaN Lab controls", this.width / 2, 49, 0xB8D7FF);
        graphics.drawCenteredString(this.font, "Requires cheats enabled in singleplayer or permission level 2 on a server.", this.width / 2, 66, 0xAAAAAA);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }
}
