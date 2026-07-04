package me.supcheg.messages.example;

import me.supcheg.messages.annotation.MessageBundle;
import me.supcheg.messages.annotation.Resolution;

@MessageBundle(
        contract = GameMessages.class,
        locales = {"ru", "en"},
        resolution = Resolution.RUNTIME)
final class GameRuntimeBundleDecl {}
