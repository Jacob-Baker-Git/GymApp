import java.io.*;
import java.nio.file.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

public final class DataStore {

    private static final Path DIR = Paths.get(System.getProperty("user.home"), ".ironpulse");

    private DataStore() {}

    public static void saveAll(
            List<ExerciseData>                      exercises,
            Map<LocalDate, ArrayList<ExerciseData>> completedLogs,
            ArrayList<RecordData>                   records,
            ArrayList<BodyWeightEntry>              bodyEntries,
            String[]                                macroGoals,
            String[]                                macroCurrent,
            ArrayList<CardioEntry>                  cardioEntries,
            Set<DayOfWeek>                          restDays) {
        try {
            Files.createDirectories(DIR);
            saveExercises(exercises);
            saveCompleted(completedLogs);
            saveRecords(records);
            saveBody(bodyEntries);
            saveMacros(macroGoals, macroCurrent);
            saveCardio(cardioEntries);
            saveRestDays(restDays);
        } catch (IOException e) {
            System.err.println("DataStore save error: " + e.getMessage());
        }
    }

    private static void saveRestDays(Set<DayOfWeek> restDays) throws IOException {
        try (PrintWriter w = writer("restdays.csv")) {
            for (DayOfWeek d : restDays) w.println(d.name());
        }
    }

    public static Set<DayOfWeek> loadRestDays() {
        Set<DayOfWeek> set = new HashSet<>();
        for (String[] row : readCsv("restdays.csv", 1)) {
            try { set.add(DayOfWeek.valueOf(row[0].trim())); } catch (Exception ignored) {}
        }
        return set;
    }

    private static void saveExercises(List<ExerciseData> list) throws IOException {
        try (PrintWriter w = writer("exercises.csv")) {
            for (ExerciseData ex : list) {
                w.println(ex.getAddedDate() + "," + escape(ex.getName()) + "," +
                          escape(ex.getWeight()) + "," + escape(ex.getReps()) + "," +
                          ex.getRestSeconds());
            }
        }
    }

    private static void saveCompleted(Map<LocalDate, ArrayList<ExerciseData>> map) throws IOException {
        try (PrintWriter w = writer("completed.csv")) {
            for (Map.Entry<LocalDate, ArrayList<ExerciseData>> e : map.entrySet())
                for (ExerciseData ex : e.getValue())
                    w.println(e.getKey() + "," + escape(ex.getName()) + "," + ex.getAddedDate());
        }
    }

    private static void saveRecords(ArrayList<RecordData> records) throws IOException {
        try (PrintWriter w = writer("records.csv")) {
            for (RecordData r : records) w.println(escape(r.getName()) + "," + escape(r.getWeight()));
        }
    }

    private static void saveBody(ArrayList<BodyWeightEntry> entries) throws IOException {
        try (PrintWriter w = writer("body.csv")) {
            for (BodyWeightEntry b : entries) w.println(b.getDate() + "," + b.getWeightKg());
        }
    }

    private static void saveMacros(String[] goals, String[] current) throws IOException {
        try (PrintWriter w = writer("macros.csv")) {
            w.println("goal," + String.join(",", goals));
            w.println("current," + String.join(",", current));
        }
    }

    private static void saveCardio(ArrayList<CardioEntry> entries) throws IOException {
        try (PrintWriter w = writer("cardio.csv")) {
            for (CardioEntry c : entries)
                w.println(c.getDate() + "," + escape(c.getType()) + "," +
                          c.getDistanceKm() + "," + c.getDurationMinutes() + "," +
                          escape(c.getNotes()));
        }
    }

    public static ArrayList<ExerciseData> loadExercises() {
        ArrayList<ExerciseData> list = new ArrayList<>();
        for (String[] row : readCsv("exercises.csv", 5)) {
            try {
                LocalDate added = LocalDate.parse(row[0]);
                list.add(new ExerciseData(unescape(row[1]), unescape(row[2]),
                        unescape(row[3]), Integer.parseInt(row[4]), added));
            } catch (Exception ignored) {}
        }
        return list;
    }

    public static Map<LocalDate, Set<String[]>> loadCompletedEntries() {
        Map<LocalDate, Set<String[]>> map = new HashMap<>();
        for (String[] row : readCsv("completed.csv", 3)) {
            try {
                LocalDate date = LocalDate.parse(row[0]);
                map.computeIfAbsent(date, d -> new HashSet<>()).add(new String[]{unescape(row[1]), row[2]});
            } catch (Exception ignored) {}
        }
        return map;
    }

    public static ArrayList<RecordData> loadRecords() {
        ArrayList<RecordData> list = new ArrayList<>();
        for (String[] row : readCsv("records.csv", 2))
            list.add(new RecordData(unescape(row[0]), unescape(row[1])));
        return list;
    }

    public static ArrayList<BodyWeightEntry> loadBody() {
        ArrayList<BodyWeightEntry> list = new ArrayList<>();
        for (String[] row : readCsv("body.csv", 2)) {
            try { list.add(new BodyWeightEntry(LocalDate.parse(row[0]), Double.parseDouble(row[1]))); }
            catch (Exception ignored) {}
        }
        return list;
    }

    public static String[][] loadMacros() {
        String[] goals = {"","","",""}, current = {"","","",""};
        for (String[] row : readCsv("macros.csv", 5)) {
            try {
                String[] t = "goal".equals(row[0]) ? goals : current;
                for (int i = 0; i < 4; i++) t[i] = row[i+1];
            } catch (Exception ignored) {}
        }
        return new String[][]{goals, current};
    }

    public static ArrayList<CardioEntry> loadCardio() {
        ArrayList<CardioEntry> list = new ArrayList<>();
        for (String[] row : readCsv("cardio.csv", 5)) {
            try {
                list.add(new CardioEntry(LocalDate.parse(row[0]), unescape(row[1]),
                        Double.parseDouble(row[2]), Integer.parseInt(row[3]), unescape(row[4])));
            } catch (Exception ignored) {}
        }
        return list;
    }

    private static PrintWriter writer(String filename) throws IOException {
        return new PrintWriter(new FileWriter(DIR.resolve(filename).toFile()));
    }

    private static List<String[]> readCsv(String filename, int minCols) {
        List<String[]> rows = new ArrayList<>();
        Path path = DIR.resolve(filename);
        if (!Files.exists(path)) return rows;
        try (BufferedReader r = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= minCols) rows.add(parts);
            }
        } catch (IOException ignored) {}
        return rows;
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace(",", "&#44;").replace("\n", " ");
    }

    private static String unescape(String s) {
        return s == null ? "" : s.replace("&#44;", ",");
    }
}
