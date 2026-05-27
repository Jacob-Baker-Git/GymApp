public class RecordData {
    private final String name;
    private String weight;

    public RecordData(String name, String weight) {
        this.name = name;
        this.weight = weight;
    }

    public String getName()   { return name; }
    public String getWeight() { return weight; }
    public void setWeight(String weight) { this.weight = weight; }
}
