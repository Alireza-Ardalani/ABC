package driver;

import java.io.*;
import java.util.concurrent.TimeUnit;

public class Starter {
    static String jarFilePath;
    static String inputApkPath;
    static String ptaMode;
    static String sinksFile;
    static String resultFolder;
    static  String apkName;
    static String time;
    static String java;
    public static void main(String[] args) throws IOException, InterruptedException {

        long maxHeap = Runtime.getRuntime().maxMemory();
        long minHeap = Runtime.getRuntime().totalMemory();
        System.out.println("Component: Min Heap Size: " + (minHeap / (1024 * 1024)) + " MB");
        System.out.println("Component: Max Heap Size: " + (maxHeap / (1024 * 1024)) + " MB");

        inputApkPath = args[0];
        resultFolder = args[1];
        jarFilePath = args[2];
        sinksFile = args[3];
        apkName = args[4];
        time = args[5];
        java = args[6];

        String fakeEntranceJar = "FakeEntrance-all-1.0-SNAPSHOT.jar";

        String outputTxt = apkName.replace(".apk",".txt");
        //String outputTxt = apkName.replace(".apk","_KCFA.txt");
        //String outputTxt = apkName.replace(".apk","_Component.txt");
        //String outputTxt = apkName.replace(".apk","_Bytecode.txt");
        //String outputTxt = apkName.replace(".apk","KOBJ.txt");
        //String outputTxt = apkName.replace(".apk","_KHYB.txt");

        try {
            // Command to run the JAR file with arguments
            ProcessBuilder processBuilder = new ProcessBuilder(
                    java, "-jar", fakeEntranceJar, inputApkPath , jarFilePath, sinksFile
            );
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            Process process = processBuilder.start();

            process.waitFor();

            System.out.println("JAR execution completed.");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 1; i < 4; i++) {
            String mode = i + "c";
            try {
                long startTime = System.nanoTime();

                ProcessBuilder processBuilder = new ProcessBuilder(
                        java, "-Xms16g", "-Xmx24g", "-cp", System.getProperty("java.class.path"), "driver.Main",
                        inputApkPath, resultFolder, mode, jarFilePath, apkName);
                processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

                Process process = processBuilder.start();
                long threshold  = Long.parseLong(time);
                boolean finished = process.waitFor(threshold, TimeUnit.SECONDS);

                if (!finished) {
                    System.out.println("Analysis took too long, terminating...");
                    process.destroy();
                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }
                    FileWriter Fw = new FileWriter(resultFolder+"/"+outputTxt, true);
                    Fw.write("K-CFA: " + mode + "\n");
                    Fw.write("Time: " + "Threshold" + "\n");
                    Fw.write("------------------------------------\n");
                    Fw.close();
                    break;
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

                            if(line.toLowerCase().contains("exception") || line.toLowerCase().contains("error") ){
                                exceptionErrorMessage = line;
                            }
                        }
                        if (errorOutput.length() > 0) {
                            System.out.println("Full Error Output:\n" + errorOutput.toString());
                        }
                    }
                    FileWriter Fw = new FileWriter(resultFolder+"/"+outputTxt, true);
                    Fw.write("K-CFA: " + mode + "\n");
                    Fw.write("Time: " + elapsedTimeInSeconds + "\n");
                    Fw.write("Error: " + exceptionErrorMessage + "\n");
                    Fw.write("------------------------------------\n");
                    Fw.close();
                    break;
                }

            } catch (IOException | InterruptedException | Error e) {
                try {
                    FileWriter Fw = new FileWriter(resultFolder+"/"+outputTxt, true);
                    Fw.write("K-CFA: " + mode + "\n");
                    Fw.write("Time: " + "---" + "\n");
                    Fw.write("Issue: " + e + "\n");
                    Fw.write("------------------------------------\n");
                    Fw.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                break;
            }
        }
    }
}
