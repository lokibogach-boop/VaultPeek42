package dev.vaultpeek;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.List;

public final class VaultPeekScreen extends Screen {
    private static final int VISIBLE_VAULTS = 12;
    private static final int CORE_SCAN_LIMIT = 5000;

    private TextFieldWidget seedField;
    private TextFieldWidget offsetField;
    private String status = "Введите сид и нажмите «Предсказать 12».";
    private String detail = "Позиция 0 означает начало последовательности.";
    private List<List<NativeLootPredictor.PredictedStack>> predicted = List.of();
    private int displayedOffset = 0;

    public VaultPeekScreen() {
        super(Text.literal("Vault Peek 42"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        contextLabels(centerX);

        seedField = new TextFieldWidget(textRenderer, centerX - 295, 52, 220, 20, Text.literal("Сид мира"));
        seedField.setPlaceholder(Text.literal("Числовой сид мира"));
        seedField.setMaxLength(20);
        addDrawableChild(seedField);

        offsetField = new TextFieldWidget(textRenderer, centerX - 65, 52, 80, 20, Text.literal("Позиция"));
        offsetField.setText("0");
        offsetField.setMaxLength(9);
        addDrawableChild(offsetField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Предсказать 12"), button -> predictRange())
                .dimensions(centerX + 25, 52, 110, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Найти ядро"), button -> findCore())
                .dimensions(centerX + 140, 52, 110, 20).build());
    }

    private void contextLabels(int centerX) {
        // Labels are drawn from render(), where the screen context is available.
    }

    private void predictRange() {
        try {
            long seed = readSeed();
            int offset = readOffset();
            predicted = NativeLootPredictor.generateOminous(seed, offset, VISIBLE_VAULTS);
            displayedOffset = offset;
            status = "Показаны следующие " + predicted.size() + " хранилищ.";
            detail = "Нумерация с начала последовательности: " + (offset + 1)
                    + "–" + (offset + predicted.size()) + ".";
        } catch (Exception | LinkageError e) {
            predicted = List.of();
            status = "Не удалось построить прогноз.";
            detail = safeMessage(e);
        }
    }

    private void findCore() {
        try {
            long seed = readSeed();
            int offset = readOffset();
            status = "Ищу тяжёлое ядро в следующих " + CORE_SCAN_LIMIT + " хранилищах…";
            List<List<NativeLootPredictor.PredictedStack>> sequence =
                    NativeLootPredictor.generateOminous(seed, offset, CORE_SCAN_LIMIT);

            int foundAt = -1;
            for (int i = 0; i < sequence.size(); i++) {
                if (containsHeavyCore(sequence.get(i))) {
                    foundAt = i;
                    break;
                }
            }

            if (foundAt < 0) {
                predicted = sequence.subList(0, Math.min(VISIBLE_VAULTS, sequence.size()));
                displayedOffset = offset;
                status = "В этом диапазоне тяжёлое ядро не найдено.";
                detail = "Проверь сид или увеличь начальную позицию и попробуй снова.";
                return;
            }

            int displayStart = Math.max(0, foundAt - 5);
            int displayEnd = Math.min(sequence.size(), displayStart + VISIBLE_VAULTS);
            predicted = sequence.subList(displayStart, displayEnd);
            displayedOffset = offset + displayStart;
            int globalNumber = offset + foundAt + 1;
            status = "Тяжёлое ядро: хранилище № " + globalNumber + " в этой последовательности.";
            detail = "От указанной позиции — " + (foundAt + 1) + "-е открытие. Строка с ядром выделена.";
        } catch (Exception | LinkageError e) {
            predicted = List.of();
            status = "Не удалось рассчитать последовательность.";
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

    private static boolean containsHeavyCore(List<NativeLootPredictor.PredictedStack> items) {
        return items.stream().anyMatch(entry -> entry.stack().isOf(Items.HEAVY_CORE));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        int cx = this.width / 2;

        context.drawCenteredTextWithShadow(textRenderer, Text.literal("Vault Peek 42"), cx, 12, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(
                textRenderer, Text.literal("Minecraft 1.21.4 Fabric • клавиша P"), cx, 26, 0xFFB5C8EA
        );
        context.drawText(textRenderer, Text.literal("Сид мира"), cx - 295, 40, 0xFFFFFFFF, false);
        context.drawText(textRenderer, Text.literal("Позиция"), cx - 65, 40, 0xFFFFFFFF, false);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(textRenderer.trimToWidth(status, this.width - 30)), cx, 82, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal(textRenderer.trimToWidth(detail, this.width - 30)), cx, 96, 0xFFDFD4A7);

        if (predicted.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    textRenderer, Text.literal("Здесь появится рассчитанная последовательность наград."),
                    cx, 150, 0xFFC8CFDB
            );
        }

        int cellW = 220;
        int cellH = 48;
        int gapX = 10;
        int gapY = 7;
        int columns = 3;
        int totalW = columns * cellW + (columns - 1) * gapX;
        int startX = cx - totalW / 2;
        int startY = 116;

        for (int i = 0; i < predicted.size() && i < VISIBLE_VAULTS; i++) {
            List<NativeLootPredictor.PredictedStack> items = predicted.get(i);
            int col = i % columns;
            int row = i / columns;
            int x = startX + col * (cellW + gapX);
            int y = startY + row * (cellH + gapY);
            boolean hasCore = containsHeavyCore(items);

            context.fill(x, y, x + cellW, y + cellH, hasCore ? 0xCC45391C : 0xCC202632);
            context.drawBorder(x, y, cellW, cellH, hasCore ? 0xFFFFD76A : 0xFF59677D);
            context.drawText(
                    textRenderer,
                    Text.literal("Хранилище № " + (displayedOffset + i + 1)),
                    x + 6, y + 4,
                    hasCore ? 0xFFFFD76A : 0xFFFFFFFF,
                    false
            );

            int iconX = x + 6;
            int iconY = y + 22;
            for (int itemIndex = 0; itemIndex < items.size() && itemIndex < 7; itemIndex++) {
                ItemStack stack = items.get(itemIndex).stack();
                context.drawItem(stack, iconX + itemIndex * 29, iconY);
            }
            if (items.isEmpty()) {
                context.drawText(textRenderer, Text.literal("Нет предметов в расчёте"),
                        x + 6, y + 26, 0xFFB8C1D9, false);
            }
        }

        int footerY = startY + 4 * (cellH + gapY) + 2;
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("Важно: точный результат требует правильного сида и актуальной позиции последовательности."),
                cx, footerY, 0xFFFFD58A
        );
        context.drawCenteredTextWithShadow(
                textRenderer,
                Text.literal("На сервере с ранее открытыми хранилищами позицию пока нужно задавать вручную."),
                cx, footerY + 13, 0xFFC8CFDB
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
