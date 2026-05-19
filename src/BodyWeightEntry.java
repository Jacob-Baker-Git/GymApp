import java.time.LocalDate;

public class BodyWeightEntry {
    LocalDate date;
    double weightKg;

    public BodyWeightEntry(LocalDate date, double weightKg) {
        this.date = date;
        this.weightKg = weightKg;
    }
}