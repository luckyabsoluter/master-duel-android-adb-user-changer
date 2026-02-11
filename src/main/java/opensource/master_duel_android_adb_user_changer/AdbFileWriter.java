package opensource.master_duel_android_adb_user_changer;

import java.util.ArrayList;
import java.util.List;

public class AdbFileWriter {
    private final AdbClient client;

    public AdbFileWriter(String adbPath) {
        this.client = new AdbClient(adbPath);
    }

    public CommandResult writeAsRoot(String serial, String path, byte[] data) {
        String redirectCommand = "cat > " + ShellEscaper.quote(path);
        String command = "sh -c " + ShellEscaper.quote(redirectCommand);
        return runShellWithInput(serial, data, "su", "-c", command);
    }

    private CommandResult runShellWithInput(String serial, byte[] input, String... shellArgs) {
        List<String> args = new ArrayList<>();
        args.add("-s");
        args.add(serial);
        args.add("shell");
        for (String arg : shellArgs) {
            args.add(arg);
        }
        return client.runWithInput(args, input);
    }
}
