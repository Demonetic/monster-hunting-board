package se.edugrade.monsterhuntingboard.model;

public record ResolvedLocation(
        String city,
        String country,
        double latitude,
        double longitude
) {
}
