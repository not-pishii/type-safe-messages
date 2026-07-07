package me.supcheg.messages.example.provider;

import me.supcheg.messages.annotation.MessageBundle;
import me.supcheg.messages.example.GameMessages;
import me.supcheg.messages.example.translations.JsonTemplateProvider;

@MessageBundle(
        contract = GameMessages.class,
        locales = {"ru", "en"},
        provider = JsonTemplateProvider.class)
final class GameProviderBundleDecl {}
