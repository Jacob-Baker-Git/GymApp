import java.time.LocalDate;

/**
 * A single cardio session log entry.
 */
public class CardioEntry {
    private final LocalDate date;
    private final String    type;        // e.g. "Run", "Cycle", "Row"
    private final double    distanceKm;
    private final int       durationMinutes;
    private final String    notes;

    public CardioEntry(LocalDate date, String type, double distanceKm,
                       int durationMinutes, String notes) {
        this.date            = date;
        this.type            = type;
        this.distanceKm      = distanceKm;
        this.durationMinutes = durationMinutes;
        this.notes           = notes;
    }

    public LocalDate getDate()            { return date; }
    public String    getType()            { return type; }
    public double    getDistanceKm()      { return distanceKm; }
    public int       getDurationMinutes() { return durationMinutes; }
    public String    getNotes()           { return notes; }

    /** Pace in min/km. Returns 0 if distance is zero. */
    public double getPaceMinPerKm() {
        if (distanceKm <= 0) return 0;
        return durationMinutes / distanceKm;
    }
}
