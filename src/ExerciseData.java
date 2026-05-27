import java.time.LocalDate;

public class ExerciseData {
    private final String    name;
    private final LocalDate addedDate;
    private double  weightKg;
    private boolean isBodyweight;
    private String  reps;
    private int     restSeconds;

    public ExerciseData(String name, double weightKg, boolean isBodyweight,
                        String reps, int restSeconds, LocalDate addedDate) {
        this.name         = name;
        this.weightKg     = weightKg;
        this.isBodyweight = isBodyweight;
        this.reps         = reps;
        this.restSeconds  = restSeconds;
        this.addedDate    = addedDate;
    }

    /** Legacy String-weight constructor — used by DataStore and dialogs. */
    public ExerciseData(String name, String weightStr, String reps, int restSeconds, LocalDate addedDate) {
        this.name        = name;
        this.reps        = reps;
        this.restSeconds = restSeconds;
        this.addedDate   = addedDate;
        if (weightStr == null || weightStr.trim().isEmpty()
                || weightStr.trim().equalsIgnoreCase("bw")
                || weightStr.trim().equalsIgnoreCase("bodyweight")
                || weightStr.trim().equals("0")) {
            this.isBodyweight = true;
            this.weightKg     = 0.0;
        } else {
            this.isBodyweight = false;
            try { this.weightKg = Double.parseDouble(weightStr.trim()); }
            catch (NumberFormatException e) { this.weightKg = 0.0; this.isBodyweight = true; }
        }
    }

    public ExerciseData(String name, String weightStr, String reps, int restSeconds) {
        this(name, weightStr, reps, restSeconds, LocalDate.now());
    }

    public ExerciseData(String name, String weightStr, String reps) {
        this(name, weightStr, reps, 90, LocalDate.now());
    }

    public String    getName()         { return name; }
    public double    getWeightKg()     { return weightKg; }
    public boolean   isBodyweight()    { return isBodyweight; }
    public String    getReps()         { return reps; }
    public int       getRestSeconds()  { return restSeconds; }
    public LocalDate getAddedDate()    { return addedDate; }

    /** For display and CSV storage — returns plain number string or "" for bodyweight. */
    public String getWeight() {
        return isBodyweight ? "" : (weightKg == Math.floor(weightKg)
                ? String.valueOf((int) weightKg) : String.valueOf(weightKg));
    }

    public java.time.DayOfWeek getDayOfWeek() { return addedDate.getDayOfWeek(); }

    public void setWeightKg(double kg)          { this.weightKg = kg; this.isBodyweight = false; }
    public void setBodyweight(boolean bw)        { this.isBodyweight = bw; if (bw) this.weightKg = 0.0; }
    public void setWeight(String weightStr)       {
        if (weightStr == null || weightStr.trim().isEmpty()
                || weightStr.trim().equalsIgnoreCase("bw")
                || weightStr.trim().equalsIgnoreCase("bodyweight")
                || weightStr.trim().equals("0")) {
            isBodyweight = true; weightKg = 0.0;
        } else {
            isBodyweight = false;
            // Normalise: strip unit suffixes, handle European comma decimal
            String t = weightStr.trim()
                    .replaceAll("(?i)\\s*(kgs?|lbs?|pounds?)\\s*$", "").trim();
            if (t.matches("\\d+,\\d+")) t = t.replace(",", ".");
            t = t.replaceAll("[^0-9.]", "").trim();
            int dot = t.indexOf('.');
            if (dot >= 0) t = t.substring(0, dot+1) + t.substring(dot+1).replace(".", "");
            try { weightKg = t.isEmpty() ? 0.0 : Double.parseDouble(t); }
            catch (NumberFormatException e) { isBodyweight = true; weightKg = 0.0; }
            if (weightKg <= 0) { isBodyweight = true; weightKg = 0.0; }
        }
    }
    public void setReps(String reps)              { this.reps = reps; }
    public void setRestSeconds(int restSeconds)   { this.restSeconds = restSeconds; }
}
