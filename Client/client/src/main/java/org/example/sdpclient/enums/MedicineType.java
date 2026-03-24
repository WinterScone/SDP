package org.example.sdpclient.enums;

public enum MedicineType {
    VTM01(0, "Vitamin C", "Tablet", "White", 1000),
    VTM02(1, "Vitamin E", "Capsule", "Green", 268),
    VTM03(2, "Vitamin B6", "Tablet", "Pale Yellow", 100),
    SUP01(3, "Omega-3 Fish Oil", "Capsule", "Clear", 1000),
    MINMG(4, "Magnesium", "Tablet", "White", 400),
    MINCA(5, "Calcium", "Tablet", "White", 600),
    MINZN(6, "Zinc", "Tablet", "Brown", 50),
    MINFE(7, "Iron", "Tablet", "Brown", 18),
    SUP02(8, "Probiotics", "Capsule", "White", 1000),
    SUP03(9, "Turmeric", "Capsule", "Yellow", 500),
    SUP04(10, "CoQ10", "Capsule", "Yellow", 100),
    SUP05(11, "Ashwagandha", "Capsule", "Green", 500),
    MINK(12, "Potassium", "Tablet", "Pale Yellow", 2500);

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
