package org.example.sdpclient.enums;

public enum MedicineType {
    VTM01(1, "Vitamin C", "Tablet", "White", 1000),
    VTM02(2, "Vitamin E", "Capsule", "Green", 268),
    VTM03(3, "Vitamin B6", "Tablet", "Pale Yellow", 100),
    SUP01(4, "Omega-3 Fish Oil", "Capsule", "Clear", 1000),
    MINMG(5, "Magnesium", "Tablet", "White", 400),
    MINCA(6, "Calcium", "Tablet", "White", 600),
    MINZN(7, "Zinc", "Tablet", "Brown", 50),
    MINFE(8, "Iron", "Tablet", "Brown", 18),
    SUP02(9, "Probiotics", "Capsule", "White", 1000),
    SUP03(10, "Turmeric", "Capsule", "Yellow", 500),
    SUP04(11, "CoQ10", "Capsule", "Yellow", 100),
    SUP05(12, "Ashwagandha", "Capsule", "Green", 500),
    MINK(13, "Potassium", "Tablet", "Pale Yellow", 2500);

    private final int id;
    private final String name;
    private final String shape;
    private final String colour;
    private final Integer dosage;

    MedicineType(int id, String name, String shape, String colour, Integer dosage) {
        this.id = id;
        this.name = name;
        this.shape = shape;
        this.colour = colour;
        this.dosage = dosage;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getShape() { return shape; }
    public String getColour() { return colour; }
    public Integer getDosage() { return dosage; }
}
