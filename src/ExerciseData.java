import java.time.LocalDate;

public class ExerciseData {
    private final String    name;
    private final LocalDate addedDate;
    private String weight;
    private String reps;
    private int    restSeconds;

    public ExerciseData(String name, String weight, String reps, int restSeconds, LocalDate addedDate) {
        this.name       = name;
        this.weight     = weight;
        this.reps       = reps;
        this.restSeconds = restSeconds;
        this.addedDate  = addedDate;
    }

    public ExerciseData(String name, String weight, String reps, int restSeconds) {
        this(name, weight, reps, restSeconds, LocalDate.now());
    }

    public ExerciseData(String name, String weight, String reps) {
        this(name, weight, reps, 90, LocalDate.now());
    }

    public String    getName()        { return name; }
    public String    getWeight()      { return weight; }
    public String    getReps()        { return reps; }
    public int       getRestSeconds() { return restSeconds; }
    public LocalDate getAddedDate()   { return addedDate; }

    public java.time.DayOfWeek getDayOfWeek() { return addedDate.getDayOfWeek(); }

    public void setWeight(String weight)         { this.weight = weight; }
    public void setReps(String reps)             { this.reps = reps; }
    public void setRestSeconds(int restSeconds)  { this.restSeconds = restSeconds; }
}
