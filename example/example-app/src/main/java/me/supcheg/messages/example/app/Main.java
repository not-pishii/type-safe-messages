package me.supcheg.messages.example.app;

import me.supcheg.messages.StringRenderer;
import me.supcheg.messages.example.GameMessages;
import me.supcheg.messages.example.GameMessagesBundle;

public final class Main {

    public static void main(String[] args) {
        GameMessages<String> ru = GameMessagesBundle.ru(StringRenderer.instance());
        System.out.println(ru.playerJoined("Steve"));
        System.out.println(ru.balance("Steve", 10));
    }

    private Main() {}
}
