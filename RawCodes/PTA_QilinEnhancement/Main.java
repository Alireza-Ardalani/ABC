package eventBasedSystem;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Main {

    static String inputFolder;
    static String jarFilePath;


    static String resultFolder;
    static String time;
    static String java;
    public static void main(String[] args) throws IOException, InterruptedException {

        java = args[0];
        String namesPath = args[1];
        List<String> names = Files.readAllLines(Paths.get(namesPath));
        time =  "15000";
        inputFolder = args[2];
        resultFolder =  args[3];
        jarFilePath = args[4];


        for(String name: names) {
            run("1-CFA",name);
            run("1-OBJ",name);
            run("1-CFA-B",name);
            run("1-CFA-C",name);
            run("1-CFA-T",name);

            run("2-CFA",name);
            run("2-OBJ",name);
            run("2-CFA-B",name);
            run("2-CFA-C",name);
            run("2-CFA-T",name);

        }

    }

    private static boolean run(String mode, String name) {
        try {
            long startTime = System.nanoTime();
            ProcessBuilder processBuilder = new ProcessBuilder(
                    java, "-Xms96g", "-Xmx128g", "-cp", System.getProperty("java.class.path"), "eventBasedSystem.RunAndroid",
                    mode, inputFolder, name,  resultFolder, jarFilePath);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
            processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);

            Process process = processBuilder.start();
            long threshold = Long.parseLong(time);
            boolean finished = process.waitFor(threshold, TimeUnit.SECONDS);

            if (!finished) {
                System.out.println("Analysis took too long, terminating...");
                process.destroy();
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
                FileWriter Fw = new FileWriter(resultFolder + "/" + name + ".txt", true);
                Fw.write("Context: " + mode + "\n");
                Fw.write("Time: " + "Threshold" + "\n");
                Fw.write("------------------------------------\n");
                Fw.close();
                return false;
            }
            long endTime = System.nanoTime();
            double elapsedTimeInSeconds = (endTime - startTime) / 1_000_000_000.0;
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String exceptionErrorMessage = "Unknown";
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    StringBuilder errorOutput = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorOutput.append(line).append("\n");

                        if (line.toLowerCase().contains("exception") || line.toLowerCase().contains("error")) {
                            exceptionErrorMessage = line;
                            //break;
                        }
                    }
                    if (errorOutput.length() > 0) {
                        System.out.println("Full Error Output:\n" + errorOutput.toString());
                    }
                }
                FileWriter Fw = new FileWriter(resultFolder + "/" + name + ".txt", true);
                Fw.write("Context: " + mode + "\n");
                Fw.write("Time: " + elapsedTimeInSeconds + "\n");
                Fw.write("Error: " + exceptionErrorMessage + "\n");
                Fw.write("------------------------------------\n");
                Fw.close();
                return false;
            }

        } catch (IOException | InterruptedException | Error e) {
            try {
                FileWriter Fw = new FileWriter(resultFolder + "/" + name + ".txt", true);
                Fw.write("Context: " + mode + "\n");
                Fw.write("Time: " + "---" + "\n");
                Fw.write("Issue: " + e + "\n");
                Fw.write("------------------------------------\n");
                Fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return false;
        }
        return true;
    }
}
