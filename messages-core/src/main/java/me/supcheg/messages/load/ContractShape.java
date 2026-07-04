package me.supcheg.messages.load;

import java.util.List;

public record ContractShape(List<MessageShape> messages) {

    public ContractShape {
        messages = List.copyOf(messages);
    }
}
