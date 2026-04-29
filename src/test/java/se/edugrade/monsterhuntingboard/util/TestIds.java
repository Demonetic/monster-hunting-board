package se.edugrade.monsterhuntingboard.util;

import java.util.UUID;

public final class TestIds {

    private TestIds() {
    }

    public static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
