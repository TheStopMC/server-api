package com.server.api.translations;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.translation.Translator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class CustomTranslator implements Translator {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomTranslator.class);
    private final Object2ObjectMap<String, MessageFormat> FORMAT_CACHE = new Object2ObjectLinkedOpenHashMap<>(500, 0.8F);

    private final ResourceBundle bundle;

    public CustomTranslator(Class<?> clazz) {
        bundle = ResourceBundle.getBundle("messages", Locale.ENGLISH, new FileResClassLoader(clazz, Path.of("")), UTF8ResourceBundleControl.get());
    }

    @Override
    public @NotNull Key name() {
        return Key.key("server:translations");
    }

    @Override
    public @Nullable MessageFormat translate(final @NotNull String key, final @NotNull Locale locale) {
        if (FORMAT_CACHE.containsKey(key)) {
            return FORMAT_CACHE.get(key);
        } else {
            if (bundle.containsKey(key)) {
                MessageFormat messageFormat = new MessageFormat(bundle.getString(key));
                FORMAT_CACHE.put(key, messageFormat);
                return messageFormat;
            } else {
                return null;
            }
        }
    }

    @Override
    public @Nullable Component translate(@NotNull TranslatableComponent component, @NotNull Locale locale) {
        return null;
    }

    @ApiStatus.Internal
    private static class FileResClassLoader extends ClassLoader {
        private final Path configFolder;

        private FileResClassLoader(Class<?> clazz, Path configFolder) {
            super(clazz.getClassLoader());
            this.configFolder = configFolder;
        }

        @Nullable
        public URL getResource(@NotNull String string) {
            Path file = this.configFolder.resolve(string);
            try {
                return file.toUri().toURL();
            } catch (MalformedURLException var4) {}

            return null;
        }

        @Nullable
        public InputStream getResourceAsStream(@NotNull String string) {
            Path file = this.configFolder.resolve(string);
            try {
                return new FileInputStream(file.toFile());
            } catch (FileNotFoundException var4) {}

            return null;
        }
    }

    @ApiStatus.Internal
    private static class UTF8ResourceBundleControl extends ResourceBundle.Control {
        private static final UTF8ResourceBundleControl INSTANCE = new UTF8ResourceBundleControl();

        public static ResourceBundle.Control get() {
            return INSTANCE;
        }

        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException, IOException {
            if (format.equals("java.properties")) {
                String bundle = this.toBundleName(baseName, locale);
                String resource = this.toResourceName(bundle, "properties");
                InputStream is = null;
                if (reload) {
                    URL url = loader.getResource(resource);
                    if (url != null) {
                        URLConnection connection = url.openConnection();
                        if (connection != null) {
                            connection.setUseCaches(false);
                            is = connection.getInputStream();
                        }
                    }
                } else {
                    is = loader.getResourceAsStream(resource);
                }

                if (is != null) {
                    InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);

                    PropertyResourceBundle var14;
                    try {
                        var14 = new PropertyResourceBundle(isr);
                    } catch (Throwable var13) {
                        try {
                            isr.close();
                        } catch (Throwable var12) {
                            var13.addSuppressed(var12);
                        }

                        throw var13;
                    }

                    isr.close();
                    return var14;
                } else {
                    return null;
                }
            } else {
                return super.newBundle(baseName, locale, format, loader, reload);
            }
        }
    }
}