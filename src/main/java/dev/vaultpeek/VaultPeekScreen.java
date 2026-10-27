package dev.vaultpeek;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.List;

public final class VaultPeekScreen extends Screen {
    private TextFieldWidget seedField;
    private String status = "Введите числовой сид мира и нажмите «Проверить».";
    private boolean searched = false;

    private static final List<LootEntry> LOOT = List.of(
            new LootEntry(Items.HEAVY_CORE, "Тяжёлое ядро", "Возможно"),
            new LootEntry(Items.ENCHANTED_GOLDEN_APPLE, "Зачарованное золотое яблоко", "Возможно"),
            new LootEntry(Items.DIAMOND_BLOCK, "Алмазный блок", "Возможен"),
            new LootEntry(Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, "Шаблон Flow", "Возможен"),
            new LootEntry(Items.MUSIC_DISC_CREATOR, "Диск Creator", "Возможен"),
            new LootEntry(Items.CROSSBOW, "Арбалет", "Возможен"),
            new LootEntry(Items.ENCHANTED_BOOK, "Зачарованная книга", "Возможна"),
            new LootEntry(Items.EMERALD, "Изумруд", "Возможен"),
            new LootEntry(Items.GOLDEN_APPLE, "Золотое яблоко", "Возможно")
    );

    public VaultPeekScreen() {
        super(Text.literal("Vault Peek 42"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        seedField = new TextFieldWidget(
                textRenderer, centerX - 145, 54, 290, 20, Text.literal("Сид мира")
        );
        seedField.setPlaceholder(Text.literal("Например: 123456789"));
        seedField.setMaxLength(32);
        addDrawableChild(seedField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Проверить"), button -> checkSeed())
                .dimensions(centerX - 145, 82, 140, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Очистить"), button -> {
            seedField.setText("");
            searched = false;
            status = "Введите числовой сид мира и нажмите «Проверить».";
        }).dimensions(centerX + 5, 82, 140, 20).build());
    }

    private void checkSeed() {
        String seed = seedField.getText().trim();
        if (seed.isEmpty()) {
            status = "Сначала введи сид мира.";
            searched = false;
            return;
        }

        try {
            Long.parseLong(seed);
            searched = true;
            status = "Сид принят: " + seed + ". Список ниже справочный, не точный прогноз.";
        } catch (NumberFormatException ex) {
            status = "Для этого прототипа введи числовой сид (целое число).";
            searched = false;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int cx = this.width / 2;

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Vault Peek 42"), cx, 18, 0xFFFFFF);
        context.drawCenteredTextWithShadow(
                textRenderer, Text.literal("Minecraft 1.21.4 Fabric • клавиша P"), cx, 32, 0xAFCBFF
        );
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, Text.literal(status), cx, 112, 0xFFFFFF);

        int startY = 140;
        int cellW = 118;
        int cellH = 54;
        int gap = 6;
        int totalW = 3 * cellW + 2 * gap;
        int startX = cx - totalW / 2;

        for (int i = 0; i < LOOT.size(); i++) {
            LootEntry entry = LOOT.get(i);
            int col = i % 3;
            int row = i / 3;
            int x = startX + col * (cellW + gap);
            int y = startY + row * (cellH + gap);

            context.fill(x, y, x + cellW, y + cellH, 0xCC202632);
            context.drawBorder(x, y, cellW, cellH, 0xFF59677D);
            context.drawItem(new ItemStack(entry.item()), x + 6, y + 8);
            context.drawText(textRenderer, trim(entry.name(), 15), x + 29, y + 8, 0xFFFFFFFF, false);
            context.drawText(textRenderer, Text.literal(entry.chance()), x + 29, y + 23, 0xFFB8C8E8, false);
        }

        int noteY = startY + 3 * (cellH + gap) + 4;
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("* Справочный список возможных предметов; сид пока не меняет результат."),
                cx, noteY, 0xFFFFD080
        );
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Точный прогноз требует отдельной реализации алгоритма Minecraft."),
                cx, noteY + 13, 0xFFCFD5E0
        );
        context.drawCenteredTextWithShadow(
                textRenderer, Text.literal("Esc — закрыть окно"), cx, this.height - 18, 0xFFAAAAAA
        );
    }

    private Text trim(String value, int max) {
        if (value.length() <= max) {
            return Text.literal(value);
        }
        return Text.literal(value.substring(0, max - 1) + "…");
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private record LootEntry(Item item, String name, String chance) {}
}
