package org.example.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Represents a single record in the "manzel" table.
 * Maps to columns: id, Name, Address, Price, original_price, Rooms, listed_date.
 */
public class Manzel {
    private int id;
    private String name;
    private String address;
    private int price;          // current price (may be discounted)
    private int originalPrice;  // price before any discount
    private int rooms;
    private LocalDate listedDate;
    private String description; // user-editable, auto-generated if blank
    private String photoPath;   // absolute path to the property photo, or null

    /** Full constructor — used when reading from DB. */
    public Manzel(int id, String name, String address, int price, int originalPrice, int rooms, LocalDate listedDate) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.price = price;
        this.originalPrice = originalPrice;
        this.rooms = rooms;
        this.listedDate = listedDate;
        this.description = null;
        this.photoPath = null;
    }

    /** Full constructor with description — used when reading from DB. */
    public Manzel(int id, String name, String address, int price, int originalPrice, int rooms, LocalDate listedDate, String description) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.price = price;
        this.originalPrice = originalPrice;
        this.rooms = rooms;
        this.listedDate = listedDate;
        this.description = description;
        this.photoPath = null;
    }

    /** Constructor without id — used when inserting a new record. */
    public Manzel(String name, String address, int price, int rooms) {
        this.name = name;
        this.address = address;
        this.price = price;
        this.originalPrice = price;
        this.rooms = rooms;
        this.listedDate = LocalDate.now();
    }

    // --- Getters ---
    public int getId()               { return id; }
    public String getName()          { return name; }
    public String getAddress()       { return address; }
    public int getPrice()            { return price; }
    public int getOriginalPrice()    { return originalPrice; }
    public int getRooms()            { return rooms; }
    public LocalDate getListedDate() { return listedDate; }
    /** Returns the stored description, or auto-generates one if null/blank. */
    public String getDescription() {
        return (description != null && !description.isBlank()) ? description : generateDescription();
    }
    /** Returns the raw stored description (may be null). */
    public String getRawDescription() { return description; }

    // --- Setters ---
    public void setId(int id)                        { this.id = id; }
    public void setPrice(int price)                  { this.price = price; }
    public void setOriginalPrice(int originalPrice)  { this.originalPrice = originalPrice; }
    public void setRooms(int rooms)                  { this.rooms = rooms; }
    public void setListedDate(LocalDate listedDate)  { this.listedDate = listedDate; }
    public void setDescription(String description)   { this.description = description; }
    public String getPhotoPath()                     { return photoPath; }
    public void setPhotoPath(String photoPath)       { this.photoPath = photoPath; }

    public void setName(String name) {
        if (name == null) throw new IllegalArgumentException("name must not be null");
        if (name.length() > 30) throw new IllegalArgumentException("name must not exceed 30 characters");
        this.name = name;
    }

    public void setAddress(String address) {
        if (address == null) throw new IllegalArgumentException("address must not be null");
        if (address.length() > 30) throw new IllegalArgumentException("address must not exceed 30 characters");
        this.address = address;
    }

    /** Returns how many days this property has been listed. */
    public long daysListed() {
        if (listedDate == null) return 0;
        return ChronoUnit.DAYS.between(listedDate, LocalDate.now());
    }

    /** Returns true if listed for 7+ days — triggers 15% discount. */
    public boolean needsPriceReduction() {
        return daysListed() >= 7;
    }

    /** Returns the discounted price (15% off original). */
    public int effectivePrice() {
        return (int) (originalPrice * 0.85);
    }

    /** Returns true if a discount is currently active. */
    public boolean isDiscounted() {
        return needsPriceReduction();
    }

    /**
     * Auto-generates a human-readable description based on the property's fields.
     * e.g. "Cozy 2-room property located in Tunis. Listed at 150,000 TND."
     */
    public String generateDescription() {
        // Room label
        String roomLabel;
        if (rooms <= 1)      roomLabel = "studio";
        else if (rooms == 2) roomLabel = "cozy 2-room";
        else if (rooms == 3) roomLabel = "spacious 3-room";
        else if (rooms <= 5) roomLabel = rooms + "-room";
        else                 roomLabel = "large " + rooms + "-room";

        // Extract location name from address (last meaningful word)
        String location = extractCity(address);

        // Price label
        String priceLabel = String.format("%,d TND", isDiscounted() ? effectivePrice() : price);

        // Discount note
        String discountNote = isDiscounted() ? " (15% discount applied)" : "";

        return "A " + roomLabel + " property located in " + location +
               ". Listed at " + priceLabel + discountNote + ".";
    }

    /** Extracts the most meaningful location word from an address string. */
    private String extractCity(String addr) {
        if (addr == null || addr.isBlank()) return "an unknown location";
        String[] tokens = addr.split("[\\s,]+");
        String result = null;
        java.util.Set<String> skip = new java.util.HashSet<>(java.util.Arrays.asList(
            "street", "st", "avenue", "ave", "road", "rd", "rue", "no", "number"
        ));
        for (String t : tokens) {
            if (t.length() > 1 && !t.matches("\\d+") && !skip.contains(t.toLowerCase()))
                result = t;
        }
        return result != null ? result : addr.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Manzel)) return false;
        Manzel m = (Manzel) o;
        return id == m.id && price == m.price && rooms == m.rooms &&
               Objects.equals(name, m.name) && Objects.equals(address, m.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, address, price, rooms);
    }

    @Override
    public String toString() {
        return "Manzel{id=" + id + ", name='" + name + "', address='" + address +
               "', price=" + price + ", originalPrice=" + originalPrice +
               ", rooms=" + rooms + ", listedDate=" + listedDate + "}";
    }
}
