package com.example.testing5;

public class enemy {
    int id;
    String name;
    int hp;
    int spd;
    int size;

    // Example enemies registry
    private static java.util.Map<String, enemy> EXAMPLES;
    static {
        EXAMPLES = new java.util.HashMap<>();
        // Baseline enemy: hp=1, spd=10, size=10 (matches previous single enemy behavior)
        EXAMPLES.put("small", new enemy(1, "small", 1, 10, 10));
        // Big enemy: hp=3, spd=3, size=20
        EXAMPLES.put("big", new enemy(2, "big", 3, 3, 20));
        EXAMPLES.put("fast", new enemy(1, "fast",1,20, 5));
    }

    public enemy(int id, String name, int hp, int spd, int size) {
        this.id = id;
        this.name = name;
        this.hp = hp;
        this.spd = spd;
        this.size = size;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
    }

    public int getSpd() {
        return spd;
    }

    public void setSpd(int spd) {
        this.spd = spd;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    // Returns a copy to avoid mutating the shared registry instances
    private static enemy copyOf(enemy e) {
        return new enemy(e.id, e.name, e.hp, e.spd, e.size);
    }

    public static java.util.Map<String, enemy> getExamples() {
        return EXAMPLES;
    }

    public static enemy getExample(String key) {
        enemy e = EXAMPLES.get(key);
        return e == null ? null : copyOf(e);
    }

    public static enemy randomExample() {
        java.util.List<enemy> values = new java.util.ArrayList<>(EXAMPLES.values());
        if (values.isEmpty()) return null;
        int idx = (int) Math.floor(Math.random() * values.size());
        return copyOf(values.get(idx));
    }

}
