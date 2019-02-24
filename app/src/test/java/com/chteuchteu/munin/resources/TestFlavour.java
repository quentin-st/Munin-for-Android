package com.chteuchteu.munin.resources;

public enum TestFlavour {
    MUNIN_1_4("munin-1.4"),
    MUNIN_2("munin-2"),
    // TODO - we don't have HTML outputs for munin 2.999.9
    //MUNIN_2_999_9("munin-2.999.9"),
    MUNSTRAP("munstrap");

    private final String dir;

    TestFlavour(final String dir) {
        this.dir = dir;
    }

    @Override
    public String toString() {
        return this.dir;
    }
}
