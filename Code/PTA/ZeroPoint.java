package driver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ZeroPoint {

    static String jarFilePath;
    static String inputFolder;
    static String outputFolder;
    static String sinksFile;
    static String resultFolder;
    static String timer;
    static String java;
    public static void main(String[] args) throws IOException, InterruptedException {

        inputFolder = args[0];
        outputFolder = args[1];
        resultFolder = args[2];
        jarFilePath = args[3];
        sinksFile = args[4];
        timer = args[5];
        java = args[6];

        List<String> apks = findApkFiles(inputFolder);
        for(String apk : apks){
            String apkPath = inputFolder + "/" + apk;
            if(!(new File(apkPath).exists())){
                System.out.println(apkPath + "\n File Does not Exist! ");
                continue;
            }
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(
                        java, "-cp", System.getProperty("java.class.path"), "driver.Starter", apkPath , resultFolder, jarFilePath, sinksFile, apk,timer,java
                );
                processBuilder.redirectErrorStream(true);
                processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                Process process = processBuilder.start();

                process.waitFor();
                System.out.println("JAR execution completed.");
                moveAPK(apkPath,outputFolder);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        }

    }
    public static List<String> findApkFiles(String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("Invalid directory path");
            return null;
        }
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".apk"));
        List<String> filesName = new ArrayList<String>();
        if (files != null) {
            for (File file : files) {
                filesName.add(file.getName());

            }
        }
        return filesName;
    }
    private static void moveAPK(String apkPath, String finishFolder) {
        File sourceFile = new File(apkPath);
        File destinationDirectory = new File(finishFolder);

        if (!sourceFile.exists() || !destinationDirectory.exists()) {
            System.err.println("Source file or destination directory does not exist.");
        }
        try {
            Path sourcePath = sourceFile.toPath();
            Path destinationPath = new File(destinationDirectory, sourceFile.getName()).toPath();

            Files.move(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("File moved successfully!");
        } catch (IOException e) {
            System.err.println("Error moving file: " + e.getMessage());
        }
    }
}
