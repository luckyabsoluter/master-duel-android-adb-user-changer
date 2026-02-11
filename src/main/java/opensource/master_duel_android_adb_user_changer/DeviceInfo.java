package opensource.master_duel_android_adb_user_changer;

public class DeviceInfo {
    private final String serial;
    private final String state;
    private final String raw;

    public DeviceInfo(String serial, String state, String raw) {
        this.serial = serial;
        this.state = state;
        this.raw = raw;
    }

    public String getSerial() {
        return serial;
    }

    public String getState() {
        return state;
    }

    public String getRaw() {
        return raw;
    }

    @Override
    public String toString() {
        return serial + " (" + state + ")";
    }
}
