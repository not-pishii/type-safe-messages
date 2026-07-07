package me.supcheg.messages.example;

import me.supcheg.messages.annotation.Messages;

@Messages
public interface GameMessages<T> {

    T playerJoined(String player);

    T balance(String player, int coins);
}
