package me.supcheg.messages.example.compiledefault;

import me.supcheg.messages.annotation.MessageBundle;
import me.supcheg.messages.example.GameMessages;

@MessageBundle(
        contract = GameMessages.class,
        locales = {"ru", "en"})
final class GameBundle {}
