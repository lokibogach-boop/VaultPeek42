package dev.vaultpeek;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class NativeLootPredictor {
    private static volatile boolean loaded;
    private static volatile String loadError;

    private NativeLootPredictor() {}

    public record PredictedStack(ItemStack stack, String itemId) {}

    public static List<List<PredictedStack>> generateOminous(long seed, int offset, int amount) {
        if (offset < 0 || amount < 1 || amount > 5000 || offset > 1000000) {
            throw new IllegalArgumentException("Слишком большой диапазон прогноза.");
        }
        ensureLoaded();
        String[] rows = generateOminousNative(seed, offset, amount);
        if (rows == null) {
            throw new IllegalStateException("Нативный генератор не смог построить последовательность.");
        }

        List<List<PredictedStack>> result = new ArrayList<>(rows.length);
        for (String row : rows) {
            List<PredictedStack> stacks = new ArrayList<>();
            if (row != null && !row.isBlank()) {
                for (String token : row.split("\\|")) {
                    int split = token.lastIndexOf('*');
                    if (split <= 0) continue;
                    String itemId = token.substring(0, split);
                    int count;
                    try {
                        count = Integer.parseInt(token.substring(split + 1));
                    } catch (NumberFormatException ignored) {
                        continue;
                    }

                    Identifier id = Identifier.tryParse(itemId);
                    if (id == null) continue;
                    var item = itemId.equals("vaultpeek:special_reward")
                            ? Items.CHEST
                            : Registries.ITEM.get(id);
                    if (item == null) continue;
                    stacks.add(new PredictedStack(new ItemStack(item, Math.max(1, count)), itemId));
                }
            }
            result.add(List.copyOf(stacks));
        }
        return List.copyOf(result);
    }

    private static void ensureLoaded() {
        if (loaded) return;
        synchronized (NativeLootPredictor.class) {
            if (loaded) return;
            if (loadError != null) throw new IllegalStateException(loadError);

            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
            String resource;
            if (os.contains("win") && (arch.contains("64") || arch.equals("amd64"))) {
                resource = "/native/windows-x86_64/vaultpeeknative.dll";
            } else if (os.contains("linux") && (arch.contains("64") || arch.equals("amd64"))) {
                resource = "/native/linux-x86_64/libvaultpeeknative.so";
            } else if ((os.contains("mac") || os.contains("darwin")) && (arch.contains("64") || arch.equals("aarch64"))) {
                resource = "/native/macos/libvaultpeeknative.dylib";
            } else {
                loadError = "Для этой системы пока нет готового нативного генератора.";
                throw new IllegalStateException(loadError);
            }

            try (InputStream input = NativeLootPredictor.class.getResourceAsStream(resource)) {
                if (input == null) {
                    throw new IOException("В JAR отсутствует файл " + resource);
                }
                String suffix = resource.substring(resource.lastIndexOf('.'));
                Path extracted = Files.createTempFile("vaultpeek42-", suffix);
                extracted.toFile().deleteOnExit();
                Files.copy(input, extracted, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.load(extracted.toAbsolutePath().toString());
                loaded = true;
            } catch (Exception | LinkageError e) {
                loadError = "Не удалось загрузить модуль предсказания: " + e.getMessage();
                throw new IllegalStateException(loadError, e);
            }
        }
    }

    private static native String[] generateOminousNative(long worldSeed, int offset, int amount);
}
