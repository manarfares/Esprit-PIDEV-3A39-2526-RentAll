package org.example.utilis;

import java.util.List;
import java.util.stream.Collectors;

import org.example.model.Manzel;

/**
 * Suggests a listing price based on similar properties already in the database.
 * Similarity is based on room count and address keywords.
 */
public class PriceSuggestionEngine {

    /**
     * Suggests a price for a new property.
     * Looks for existing properties with the same room count and a matching
     * address keyword, then returns the average price of those matches.
     *
     * @param allRecords   all existing properties from the DB
     * @param address      address of the new property
     * @param rooms        number of rooms
     * @return suggested price, or -1 if not enough data to suggest
     */
    public static int suggest(List<Manzel> allRecords, String address, int rooms) {
        if (allRecords == null || allRecords.isEmpty() || address == null) return -1;

        String addressLower = address.toLowerCase().trim();

        // Extract first meaningful word from address as location keyword
        String locationKeyword = extractLocationKeyword(addressLower);

        // Step 1: find properties with same rooms AND matching location keyword
        List<Manzel> similar = allRecords.stream()
                .filter(m -> m.getRooms() == rooms)
                .filter(m -> locationKeyword.isEmpty() ||
                             m.getAddress().toLowerCase().contains(locationKeyword))
                .collect(Collectors.toList());

        // Step 2: if no location match, fall back to same room count only
        if (similar.isEmpty()) {
            similar = allRecords.stream()
                    .filter(m -> m.getRooms() == rooms)
                    .collect(Collectors.toList());
        }

        // Step 3: if still nothing, use all records
        if (similar.isEmpty()) {
            similar = allRecords;
        }

        // Return average price of matched records
        return (int) similar.stream()
                .mapToInt(Manzel::getPrice)
                .average()
                .orElse(-1);
    }

    /**
     * Extracts the first meaningful word from an address to use as a location keyword.
     * Skips common words like "street", "st", "ave", numbers, etc.
     */
    private static String extractLocationKeyword(String address) {
        String[] words = address.split("[\\s,]+");
        for (String word : words) {
            if (word.length() > 2 && !word.matches("\\d+") &&
                !word.equalsIgnoreCase("st") && !word.equalsIgnoreCase("ave") &&
                !word.equalsIgnoreCase("rd") && !word.equalsIgnoreCase("blvd") &&
                !word.equalsIgnoreCase("street") && !word.equalsIgnoreCase("road")) {
                return word;
            }
        }
        return "";
    }
}
