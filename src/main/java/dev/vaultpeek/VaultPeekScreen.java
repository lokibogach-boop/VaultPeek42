package dev.vaultpeek;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VaultPeekScreen extends Screen {
    private static final int VISIBLE_VAULTS = 12;
    private static final int TABLE_ROW_HEIGHT = 20;

    private TextFieldWidget seedField;
    private TextFieldWidget offsetField;
    private TextFieldWidget searchField;

    private String status = "Введите сид и нажмите «Показать 12».";
    private String detail = "Позиция 0 начинает последовательность.";
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
                .dimensions(cx + 60, 52, 105, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Следующие 12"), button -> nextRange())
                .dimensions(cx + 175, 52, 120, 20).build());

        searchField = new TextFieldWidget(textRenderer, cx - 250, 105, 280, 20, Text.literal("Найти предмет"));
        searchField.setPlaceholder(Text.literal("Название или ID обычного предмета"));
        searchField.setMaxLength(100);
        addDrawableChild(searchField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Найти в таблице"), button -> findItem())
                .dimensions(cx + 40, 105, 130, 20).build());
    }

    private void nextRange() {
        try {
            int offset = readOffset();
            offsetField.setText(Integer.toString(Math.addExact(offset, VISIBLE_VAULTS)));
            predictRange();
        } catch (Exception e) {
            status = "Не удалось перейти к следующему диапазону.";
            detail = safeMessage(e);
        }
    }

    private void predictRange() {
        try {
            long seed = readSeed();
            int offset = readOffset();
            predicted = NativeLootPredictor.generateOminous(seed, offset, VISIBLE_VAULTS);
            displayedOffset = offset;
            status = "Таблица рассчитана для введённого сида.";
            detail = "Показаны позиции № " + (offset + 1) + "–" + (offset + predicted.size())
                    + ". Поиск проверяет только эти 12 хранилищ.";
        } catch (Exception | LinkageError e) {
            predicted = List.of();
            status = "Не удалось построить последовательность.";
            detail = safeMessage(e);
        }
    }

    private void findItem() {
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (predicted.isEmpty()) {
            status = "Сначала нажмите «Показать 12».";
            detail = "Поиск работает по уже рассчитанной таблице.";
            return;
        }
        if (query.isEmpty()) {
            status = "Введите название предмета или его ID.";
            detail = "Например, можно искать по обычному названию или minecraft:item_id.";
            return;
        }

        List<String> positions = new ArrayList<>();
        for (int i = 0; i < predicted.size(); i++) {
            boolean foundInThisVault = false;
            for (NativeLootPredictor.PredictedStack entry : predicted.get(i)) {
                // Special rewards deliberately stay generic in the public viewer.
                if (entry.itemId().equals("vaultpeek:special_reward")) {
                    continue;
                }
                String localizedName = entry.stack().getName().getString().toLowerCase(Locale.ROOT);
                String itemId = entry.itemId().toLowerCase(Locale.ROOT);
                if (localizedName.contains(query) || itemId.contains(query)) {
                    foundInThisVault = true;
                    break;
                }
            }
            if (foundInThisVault) {
                positions.add(Integer.toString(displayedOffset + i + 1));
            }
        }

        if (positions.isEmpty()) {
            status = "Совпадений в текущих 12 хранилищах нет.";
            detail = "Нажмите «Следующие 12» или задайте другую позицию и рассчитайте диапазон.";
        } else {
            status = "Найдено в хранилищах № " + String.join(", ", positions) + ".";
            detail = "Это номера в рассчитанной последовательности; поиск охватывает только видимые 12 строк.";
        }
    }

    private long readSeed() {
        String value = seedField.getText().trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Сначала введите сид мира.");
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

        // Header panel
        context.fill(cx - 310, 8, cx + 310, 35, 0xDD151C27);
        context.fill(cx - 310, 8, cx + 310, 10, 0xFF4A9BDF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("VAULT PEEK 42"), cx, 13, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(
                textRenderer, Text.literal("Minecraft 1.21.4 Fabric  •  клавиша P"), cx, 25, 0xFFB5C8EA
        );

        context.drawText(textRenderer, Text.literal("Сид мира"), cx - 250, 40, 0xFFFFFFFF, false);
        context.drawText(textRenderer, Text.literal("Позиция с начала (0 = начало)"), cx - 30, 40, 0xFFFFFFFF, false);
        context.drawText(textRenderer, Text.literal("Поиск по текущей таблице"), cx - 250, 93, 0xFFDAE7F7, false);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                textRenderer, Text.literal(textRenderer.trimToWidth(status, this.width - 24)), cx, 131, 0xFFFFFFFF
        );
        context.drawCenteredTextWithShadow(
                textRenderer, Text.literal(textRenderer.trimToWidth(detail, this.width - 24)), cx, 143, 0xFFDFD4A7
        );

        int tableW = Math.min(760, this.width - 24);
        int tableX = cx - tableW / 2;
        int headerY = 160;
        int rowStartY = 180;
        int numberColumnW = 58;

        // Table heading
        context.fill(tableX, headerY, tableX + tableW, headerY + 18, 0xFF31516F);
        context.drawBorder(tableX, headerY, tableW, 18, 0xFF79B7E8);
        context.drawText(textRenderer, Text.literal("№"), tableX + 12, headerY + 5, 0xFFFFFFFF, false);
        context.drawText(textRenderer, Text.literal("ПРЕДМЕТЫ В НАГРАДЕ"), tableX + numberColumnW, headerY + 5,
                0xFFFFFFFF, false);

        int rows = Math.min(predicted.size(), VISIBLE_VAULTS);
        for (int i = 0; i < rows; i++) {
            List<NativeLootPredictor.PredictedStack> items = predicted.get(i);
            int y = rowStartY + i * TABLE_ROW_HEIGHT;
            int number = displayedOffset + i + 1;

            context.fill(tableX, y, tableX + tableW, y + TABLE_ROW_HEIGHT,
                    i % 2 == 0 ? 0xDD202A36 : 0xDD293645);
            context.fill(tableX, y, tableX + 3, y + TABLE_ROW_HEIGHT, 0xFF4A9BDF);
            context.drawBorder(tableX, y, tableW, TABLE_ROW_HEIGHT, 0x805C7189);
            context.drawText(textRenderer, Text.literal(Integer.toString(number)),
                    tableX + 12, y + 6, 0xFFFFD58A, false);

            if (items.isEmpty()) {
                context.drawText(textRenderer, Text.literal("Нет предметов в расчёте"),
                        tableX + numberColumnW, y + 6, 0xFFB8C1D9, false);
                continue;
            }

            int itemAreaW = tableW - numberColumnW - 8;
            int slotW = itemAreaW / 4;
            int shownCount = items.size() > 4 ? 3 : Math.min(items.size(), 4);

            for (int itemIndex = 0; itemIndex < shownCount; itemIndex++) {
                NativeLootPredictor.PredictedStack entry = items.get(itemIndex);
                ItemStack stack = entry.stack();
                int itemX = tableX + numberColumnW + itemIndex * slotW + 3;
                context.drawItem(stack, itemX, y + 2);

                String name;
                if (entry.itemId().equals("vaultpeek:special_reward")) {
                    name = "Особая награда";
                    context.drawText(textRenderer, Text.literal("?"), itemX + 11, y - 1, 0xFFFFD76A, false);
                } else {
                    name = stack.getName().getString();
                    if (stack.getCount() > 1) {
                        name += " ×" + stack.getCount();
                    }
                }
                int availableNameW = Math.max(20, slotW - 24);
                name = textRenderer.trimToWidth(name, availableNameW);
                context.drawText(textRenderer, Text.literal(name), itemX + 19, y + 6, 0xFFE8EDF5, false);
            }

            if (items.size() > 4) {
                int moreX = tableX + numberColumnW + 3 * slotW + 3;
                context.drawText(textRenderer, Text.literal("+" + (items.size() - 3) + " ещё"),
                        moreX + 2, y + 6, 0xFFB5C8EA, false);
            }
        }

        if (predicted.isEmpty()) {
            int emptyY = rowStartY + 42;
            context.fill(tableX, emptyY, tableX + tableW, emptyY + 42, 0xCC202A36);
            context.drawBorder(tableX, emptyY, tableW, 42, 0xFF59677D);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Введите сид мира и нажмите «Показать 12», чтобы заполнить таблицу."),
                    cx, emptyY + 16, 0xFFC8CFDB);
        }

        int footerY = Math.min(this.height - 35, rowStartY + VISIBLE_VAULTS * TABLE_ROW_HEIGHT + 8);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("На сервере текущая позиция может отличаться, если хранилища уже открывали."),
                cx, footerY, 0xFFC8CFDB);
        context.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Особые награды обозначены обобщённо. Esc — закрыть окно."),
                cx, footerY + 13, 0xFFFFD58A);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
