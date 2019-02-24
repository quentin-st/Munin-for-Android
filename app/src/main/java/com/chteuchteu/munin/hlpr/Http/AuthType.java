package com.chteuchteu.munin.hlpr.Http;

/**
 * Apache authentication type.
 */
public enum AuthType {
    /**
     * Type hasn't been determined yet.
     */
    UNKNOWN(-2),
    /**
     * No apache authentication is used.
     */
    NONE(-1),
    /**
     * Basic authentication is used.
     */
    BASIC(1),
    /**
     * Digest authentication is used.
     */
    DIGEST(2);

    private int val;

    AuthType(int val) { this.val = val; }

    public int getVal() { return this.val; }
    public String toString() { return val + ""; }

    public static AuthType get(int val) {
        for (AuthType t : AuthType.values())
            if (t.val == val)
                return t;
        return UNKNOWN;
    }

    public static Boolean isAuthNeeded(AuthType type) {
        return type == AuthType.BASIC || type == AuthType.DIGEST;
    }
}
