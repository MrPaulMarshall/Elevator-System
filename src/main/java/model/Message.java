package main.java.model;

import lombok.NonNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Message implements Serializable {
    public final String type;
    public final int number;
    public final String text;

    public Message(@NonNull String type, int number) {
        if (!numberArgMessages.contains(type))
            throw new IllegalArgumentException("Unrecognized number-arg command: " + type);
        this.type = type;
        this.number = number;
        this.text = "";
    }

    public Message(@NonNull String type, String message) {
        if (!stringArgMessages.contains(type))
            throw new IllegalArgumentException("Unrecognized string-arg command: " + type);
        this.type = type;
        this.number = -1;
        this.text = message;
    }

    public Message(@NonNull String type) {
        if (!noArgMessages.contains(type))
            throw new IllegalArgumentException("Unrecognized no-arg command: " + type);
        this.type = type;
        this.number = -1;
        this.text = "";
    }

    private static final Set<String> noArgMessages = Stream.of("exit")
            .collect(Collectors.toCollection(HashSet::new));
    private static final Set<String> stringArgMessages = Stream.of("result")
            .collect(Collectors.toCollection(HashSet::new));
    private static final Set<String> numberArgMessages = Stream.of("pickup", "ID")
            .collect(Collectors.toCollection(HashSet::new));
}
