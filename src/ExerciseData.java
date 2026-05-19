public class ExerciseData {
    String name;
    String weight;
    String reps;
    int restSeconds;
    int loggedSets;

    public ExerciseData(String name, String weight, String reps) {
        this(name, weight, reps, 90);
    }

    public ExerciseData(String name, String weight, String reps, int restSeconds) {
        this.name = name;
        this.weight = weight;
        this.reps = reps;
        this.restSeconds = restSeconds;
        this.loggedSets = 0;
    }
}