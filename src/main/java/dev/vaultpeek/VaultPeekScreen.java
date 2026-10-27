package dev.vaultpeek;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public final class VaultPeekScreen extends Screen {
    private static final int VISIBLE_VAULTS = 12;

    private TextFieldWidget seedField;
    private TextFieldWidget offsetField;
    private String status = "Введите сид и нажмите «Показать 12».";
    private String detail = "Позиция 0 означает начало последовательности.";
    private List<List<NativeLootPredictor.PredictedStack>> predicted = List.of();
    private int displayedOffset = 0;

    public VaultPeekScreen() {
        super(Text.literal("Vault Peek 42"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        seedField = new TextFieldWidget(textRenderer, cx - 250, 52, 210, 20, Text.literal("Сид мира"));
        seedField.setPlaceholder(Text.literal("Числовой сид мира"));
        seedField.setMaxLength(20);
        addDrawableChild(seedField);

        offsetField = new TextFieldWidget(textRenderer, cx - 30, 52, 80, 20, Text.literal("Позиция"));
        offsetField.setText("0");
        offsetField.setMaxLength(9);
        addDrawableChild(offsetField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Показать 12"), button -> predictRange())
                .dimensions(cx - 110, 79, 105, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Следующие"), button -> nextRange())
                .dimensions(cx + 5, 79, 105, 20).build());
    }

    private void nextRange() {
        try {
            int offset = readOffset();
            offsetField.setText(Integer.toString(Math.addExact(offset, VISIBLE_VAULTS)));
            predictRange();
        } catch (Exception e) {
            status = "Не удалось перейти дальше.";
            detail = safeMessage(e);
        }
    }

    private void predictRange() {
        try {
            long seed = readSeed();
            int offset = readOffset();
            predicted = NativeLootPredictor.generateOminous(seed, offset, VISIBLE_VAULTS);
            displayedOffset = offset;
            status = "Показана рассчитанная последовательность наград.";
            detail = "Номера с начала последовательности: " + (offset + 1)
                    + "–" + (offset + predicted.size()) + ".";
        } catch (Exception | LinkageError e) {
            predicted = List.of();
            status = "Не удалось построить последовательность.";
            detail = safeMessage(e);
        }
    }

    private long readSeed() {
        String value = seedField.getText().trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Сначала введи сид мира.");
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Сид должен быть целым числом в диапазоне long.");
        }
    }

    private int readOffset() {
        String value = offsetField.getText().trim();
        if (value.isEmpty()) return 0;
        try {
            int offset = Integer.parseInt(value);
            if (offset < 0 || offset > 1000000) {
                throw new IllegalArgumentException("Позиция должна быть от 0 до 1 000 000.");
            }
            return offset;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Позиция должна быть целым неотрицательным числом.");
        }
    }

    private static String safeMessage(Throwable e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int cx = this.width / 2;

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Vault Peek 42"), cx, 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(
                textRenderer, Text.literal("Minecraft 1.21.4 Fabric • клавиша P"), cx, 26, 0xFFB5C8EA
        );
        context.drawText(textRenderer, Text.literal("Сид мира"), cx - 250, 40, 0xFFFFFFFF, false);
        context.drawText(textRenderer, Text.literal("Позиция"), cx - 30, 40, 0xFFFFFFFF, false);

        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(
                textRenderer, Text.literal(textRenderer.trimToWidth(status, this.width - 30)), cx, 105, 0xFFFFFFFF
        );
        context.drawCenteredTextWithShadow(
                textRenderer, Text.literal(textRenderer.trimToWidth(detail, this.width - 30)), cx, 118, 0xFFDFD4A7
        );

        int cellW = 220;
        int cellH = 48;
        int gapX = 10;
        int gapY = 7;
        int columns = 3;
        int totalW = columns * cellW + (columns - 1) * gapX;
        int startX = cx - totalW / 2;
        int startY = 140;

        for (int i = 0; i < predicted.size() && i < VISIBLE_VAULTS; i++) {
            List<NativeLootPredictor.PredictedStack> items = predicted.get(i);
            int col = i % columns;
            int row = i / columns;
            int x = startX + col * (cellW + gapX);
            int y = startY + row * (cellH + gapY);

            context.fill(x, y, x + cellW, y + cellH, 0xCC202632);
            context.drawBorder(x, y, cellW, cellH, 0xFF59677D);
            context.drawText(textRenderer, Text.literal("Хранилище № " + (displayedOffset + i + 1)),
                    x + 6, y + 4, 0xFFFFFFFF, false);

            int iconX = x + 6;
            int iconY = y + 22;
            for (int itemIndex = 0; itemIndex < items.size() && itemIndex < 7; itemIndex++) {
                NativeLootPredictor.PredictedStack entry = items.get(itemIndex);
                ItemStack stack = entry.stack();
                int itemX = iconX + itemIndex * 29;
                context.drawItem(stack, itemX, iconY);
                if (entry.itemId().equals("vaultpeek:special_reward")) {
                    context.drawText(textRenderer, Text.literal("?"), itemX + 12, iconY - 4, 0xFFFFD76A, false);
                }
            }
            if (items.isEmpty()) {
                context.drawText(textRenderer, Text.literal("Нет предметов в расчёте"),
                        x + 6, y + 26, 0xFFB8C1D9, false);
            }
        }

        if (predicted.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer, Text.literal("Здесь появится рассчитанная последовательность наград."),
                    cx, 160, 0xFFC8CFDB
            );
        }

        int footerY = startY + 4 * (cellH + gapY) + 2;
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Результат зависит от сида и текущей позиции последовательности."),
                cx, footerY, 0xFFFFD58A
        );
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("На сервере нужно знать или восстановить offset ранее открытых хранилищ."),
                cx, footerY + 13, 0xFFC8CFDB
        );
        context.drawCenteredTextWithShadow(
                textRenderer, Text.literal("Некоторые особые предметы обозначены ящиком с ?."), cx, footerY + 26, 0xFFC8CFDB
        );
        context.drawCenteredTextWithShadow(
                textRenderer, Text.literal("Esc — закрыть окно"), cx, this.height - 18, 0xFFAAAAAA
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
