package me.supcheg.messages.example.runtimedefault;

import me.supcheg.messages.annotation.MessageBundle;
import me.supcheg.messages.annotation.Resolution;
import me.supcheg.messages.example.GameMessages;

@MessageBundle(
        contract = GameMessages.class,
        locales = {"ru", "en"},
        resolution = Resolution.RUNTIME)
final class GameRuntimeBundleDecl {}
