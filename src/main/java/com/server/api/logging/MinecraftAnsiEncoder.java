package com.server.api.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class MinecraftAnsiEncoder extends PatternLayoutEncoder {
    @Override
    public byte[] encode(ILoggingEvent event) {
        String original = this.layout.doLayout(event);
        String colored;
        if (event.getLevel() == Level.ERROR) {
            colored= "\u001B[91m" + original + "\u001B[0m";
        } else if (event.getLevel() == Level.WARN) {
            colored = "\u001B[93m" + original + "\u001B[0m";
        } else {
            colored = MinecraftColorToAnsi.translate(original) + "\u001B[0m";
        }
        return colored.getBytes(getCharset());
    }
}