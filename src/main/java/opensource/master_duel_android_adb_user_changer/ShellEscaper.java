package opensource.master_duel_android_adb_user_changer;

public final class ShellEscaper {
    private ShellEscaper() {
    }

    public static String quote(String value) {
        if (value == null) {
            return "''";
        }
        return "'" + value.replace("'", "'\"'\"'") + "'";
    }
}
