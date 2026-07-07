package me.supcheg.messages.example.runtimecustom;

import me.supcheg.messages.annotation.MessageBundle;
import me.supcheg.messages.annotation.Resolution;
import me.supcheg.messages.example.GameMessages;
import me.supcheg.messages.example.translations.JsonTemplateProvider;

@MessageBundle(
        contract = GameMessages.class,
        locales = {"ru", "en"},
        resolution = Resolution.RUNTIME,
        provider = JsonTemplateProvider.class)
final class GameRuntimeBundleDecl {}
