package ru.hogwarts.school;

class Recommendation {
    private String name;
    private String id;
    private String text;

    public Recommendation() {}

    public Recommendation(String name, String id, String text) {
        this.name = name;
        this.id = id;
        this.text = text;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}