package opensource.master_duel_android_adb_user_changer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AdbClient {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private final String adbPath;

    public AdbClient(String adbPath) {
        this.adbPath = adbPath;
    }

    public CommandResult run(List<String> args) {
        return run(args, DEFAULT_TIMEOUT);
    }

    public CommandResult run(List<String> args, Duration timeout) {
        return run(args, timeout, null);
    }

    public CommandResult runWithInput(List<String> args, byte[] input) {
        return run(args, DEFAULT_TIMEOUT, input);
    }

    public CommandResult run(List<String> args, Duration timeout, byte[] input) {
        List<String> command = new ArrayList<>();
        command.add(adbPath);
        command.addAll(args);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(false);

        try {
            Process process = builder.start();
            if (input != null) {
                try (var outputStream = process.getOutputStream()) {
                    outputStream.write(input);
                }
            } else {
                process.getOutputStream().close();
            }
            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);

            if (!finished) {
                process.destroyForcibly();
                return new CommandResult(1,
                        "Process timeout after " + timeout.toSeconds() + "s",
                        "",
                        String.join(" ", command));
            }

            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());

            int exitCode = process.exitValue();
            return new CommandResult(exitCode, stdout, stderr, String.join(" ", command));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new CommandResult(1, ex.getMessage(), "", String.join(" ", command));
        } catch (IOException ex) {
            return new CommandResult(1, ex.getMessage(), "", String.join(" ", command));
        }
    }

    private String readStream(java.io.InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
        }
        return builder.toString();
    }
}
