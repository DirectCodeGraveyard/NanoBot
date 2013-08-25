package org.nanobot

class Utils {
    static def asInteger(String s) {
        try {
            return Integer.parseInt(s)
        } catch(ignored) {
            return 0
        }
    }
}
