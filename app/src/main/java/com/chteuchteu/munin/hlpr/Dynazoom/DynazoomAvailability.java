package com.chteuchteu.munin.hlpr.Dynazoom;

public enum DynazoomAvailability {
    AUTO_DETECT(""), FALSE("false"), TRUE("true");
    private String val = "";
    DynazoomAvailability(String val) { this.val = val; }
    public String getVal() { return this.val; }
    public String toString() { return this.val; }
    public static DynazoomAvailability get(String val) {
        for (DynazoomAvailability g : DynazoomAvailability.values())
            if (g.val.equals(val))
                return g;
        return AUTO_DETECT;
    }
    public static DynazoomAvailability get(boolean val) {
        return val ? TRUE : FALSE;
    }
}
