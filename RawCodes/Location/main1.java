import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.*;
import java.nio.file.*;

import jadx.core.utils.exceptions.JadxRuntimeException;
import org.xmlpull.v1.XmlPullParserException;
import soot.*;
import soot.jimple.Stmt;

import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.manifest.IAndroidComponent;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.results.DataFlowResult;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class main {

    static String projectFolder = System.getProperty("user.dir");
    public static  String sinkSourceFilePath = projectFolder + "/inputData/sourcesSinks/SourcesAndSinksLocation.txt";
    private static  String androidJarPath;
    private static String inputFolder;
    private static String outPutFolder;
    private static  String finishFolder;
    private static List<String> special = new ArrayList<>();

    public static void main(String[] args) throws XmlPullParserException, IOException {
        androidJarPath = args[0];
        sinkSourceFilePath = args[1];
        inputFolder = args[2];
        finishFolder = args[3];
        outPutFolder = args[4];
        String thirdPartyFilePathInput = args[5];
        String sinkSourceFileOriginal = args[6];

        List<String> filenames = findApkFiles(inputFolder);
        for (String name : filenames) {
            try{
                System.gc();
                String apkPath = inputFolder + "/" + name;
                JADXHandler newInstance = null;
                boolean success = false;
                int retries = 0;
                int MAX_RETRIES = 6;
                while (!success && retries < MAX_RETRIES) {
                    try {
                        newInstance = new JADXHandler(apkPath);
                        success = true;
                    } catch (JadxRuntimeException e) {
                        retries++;
                        if (retries < MAX_RETRIES) {
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        } else {
                        }
                    }
                }
                List<LeakFactory> results = findLeak(apkPath);
                List<LeakFactory> resultsClear = removeDuplicate(results);
                if(!resultsClear.isEmpty()){
                    outputFunction(resultsClear,name);
                }
                try {
                    if(!(newInstance == null)){
                        List<String> dotCodeGroup = visual(resultsClear,  newInstance,  name, thirdPartyFilePathInput);
                        int size = dotCodeGroup.size();
                        int i = 1;
                        for(String dotCode: dotCodeGroup){
                            if(size == 1){
                                if(!(resultsClear.isEmpty() || dotCode.equals(""))){
                                    generatePNG(dotCode,outPutFolder + "/" + name + ".png");
                                    //saveDotCode(dotCode,outPutFolder + "/"+ name +"_"+ i + ".txt");
                                }
                            }
                            else {
                                if(!(resultsClear.isEmpty() || dotCode.equals(""))){
                                    generatePNG(dotCode,outPutFolder + "/" + name +"_"+ i + ".png");
                                    //saveDotCode(dotCode,outPutFolder + "/" + name +"_"+ i + ".txt");
                                }
                            }
                            i++;
                        }
                    }
                    else{
                        System.err.println("NullPointerException at JADX");
                    }
                } catch (NullPointerException e) {
                    System.err.println("NullPointerException at JADX");
                }

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
            catch (Exception e){
                continue;
            }
        }
    }
    public static void generatePNG(String dotString, String outputPath) {
        ProcessBuilder processBuilder = new ProcessBuilder("dot", "-Tpng");
        processBuilder.redirectOutput(new File(outputPath));
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(dotString.getBytes());
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("PNG generated successfully at: " + outputPath);
            } else {
                System.err.println("Graphviz execution error. Exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }
    public static void saveDotCode (String dotString, String outputPath) throws IOException {
        FileWriter fileWriter = new FileWriter(outputPath, true);
        fileWriter.write(dotString);
        fileWriter.close();
    }

    private static List<String> visual(List<LeakFactory> resultsClear, JADXHandler newInstance, String firstPartyName, String thirdPartyFilePathInput) {
        List<String> clearance = new ArrayList<String>();
        List<String> dots = new ArrayList<String>();
        int size = resultsClear.size();
        String FirstPartyName  = firstPartyName.substring(0,firstPartyName.length()-4);
        String dotCode = "";
        int i = 0;
        int counter = 0;
        int distinctCounter = 0;
        for(LeakFactory leakFactory : resultsClear){
            String temp = "";
            String dotCodeTemp = "";
            Categorization categorization = new Categorization(leakFactory, newInstance, thirdPartyFilePathInput,firstPartyName);
            //result
            String specialResult = categorization.specialResult;
            specialResult = specialResult +"-"+leakFactory.getResult().getSink();
            //
            ArrayList<ArrayList<String>> category = categorization.getFinalCategory();
            ArrayList<ArrayList<String>> itemCategory = categorization.getFinalCategoryItems();
            String  sub = "cluster_" + i;
            dotCodeTemp = dotCodeTemp + "subgraph "+ sub +" { \n";
            boolean first = true;
            int j = 1;
            for(ArrayList<String> element : category){
                if (first){
                    dotCodeTemp = dotCodeTemp + "label= <<table border=\"0\" cellborder=\"0\"> \n" + "<tr><td align=\"center\" colspan=\"2\"><font color=\"black\">" + "First Party: "+FirstPartyName+ "</font></td></tr>";
                    dotCodeTemp = dotCodeTemp + "<tr><td align=\"center\" colspan=\"2\">" + "<font color=\"" + element.get(1) +"\">" +"Component: "+element.get(0)+ "</font></td></tr>" + "\n" + "</table>>;";
                    //dotCodeTemp = dotCodeTemp + "label = \"" +"First Party: " + "\n Enclosing Component: "+element.get(0) + "\"; \n" + "fontcolor = " + element.get(1) + "; \n";
                    first = false;
                    temp = temp + element.get(0) + "-" + element.get(1);
                }
                else {
                    if(element.get(2).equals("Method")){
                        dotCodeTemp = dotCodeTemp + "node" +"_" + i +"_" + j + " [" + "label = \"" + element.get(3) + "\n" + element.get(0) + "\" ,"  + " fontcolor = " + element.get(1) + "]; \n";
                        temp = temp + element.get(0) + "-" + element.get(1);
                    } else if (element.get(2).equals("Element")) {
                        dotCodeTemp = dotCodeTemp + "node" +"_" + i +"_" + j + " [" + "label = \"" + element.get(0) + "\" ,"  + " fontcolor = " + element.get(1) + " shape = box"+ "]; \n";
                        temp = temp + element.get(0) + "-" + element.get(1);
                    }
                    j++;
                }
            }
            /**
             * Because we don't want green element as sink! we just interested in red element as sink!
             */
            if(category.get(category.size()-1).get(2).equals("Element")){
                if(category.get(category.size()-1).get(1).equals("seagreen")){
                    counter++;
                    //System.out.println("counter => " + counter + " - " + size );
                    if(counter == size){
                        if(!dotCode.isEmpty()){
                            String dotGroup = "digraph G { \n"  + dotCode + "}";
                            dots.add(dotGroup);
                        }
                    }
                    continue;
                }
            }
            /**
             * This rule is not nice, it is necessary, but we need a better solution
             */
//            if(!(category.get(2).get(0).contains("getLastKnownLocation") || category.get(2).get(0).contains("getLatitude") || category.get(2).get(0).contains("getLongitude"))){
//                System.out.println(category.get(2).get(0));
//                System.out.println("Source ok nist");
//                counter++;
//                if(counter == size){
//                    if(!dotCode.isEmpty()){
//                        String dotGroup = "digraph G { \n"  + dotCode + "}";
//                        dots.add(dotGroup);
//                    }
//                }
//                continue;
//            }
            counter++;

            dotCodeTemp = dotCodeTemp + "source" +"_"  +i + " [" + "label = \"" + "Source" + "\" ,"  + " fontcolor = " + "black" + "]; \n";
            dotCodeTemp = dotCodeTemp + "sink" +"_" + i + " [" + "label = \"" + "Sink" + "\" ,"  + " fontcolor = " + "black" + "]; \n";

            dotCodeTemp = dotCodeTemp + "source" + "_" +i + " -> ";
            for(int z = 1; z <j ; z++){
                dotCodeTemp = dotCodeTemp + "node" + "_" +i +"_" + z + "-> ";
            }
            dotCodeTemp = dotCodeTemp  + "sink" +"_" + i + "; \n";

            dotCodeTemp = dotCodeTemp + "} \n";

            i++;
            if(!clearance.contains(temp)){
                dotCode = dotCode + dotCodeTemp;
                clearance.add(temp);
                distinctCounter++;
                if(dotCode.contains("red")){
                    specialResult = specialResult + "-" + "red";
                }
                else {
                    specialResult = specialResult + "-" + "green";
                }
                special.add(specialResult);
            }
            if(!dotCode.isEmpty()){
                if(distinctCounter % 7 == 0 || counter == size){
                    String dotGroup = "digraph G { \n"  + dotCode + "}";
                    dots.add(dotGroup);

                    dotCode = "";
                }
            }
        }
        return dots;
    }
    public static List<LeakFactory> removeDuplicate(List<LeakFactory> resultsInput){
        List<LeakFactory> resultsOutput = new ArrayList<LeakFactory>();
        for(LeakFactory result : resultsInput){
            if(resultsOutput.isEmpty()){
                resultsOutput.add(result);
            }
            else {
                boolean check = true;
                for (LeakFactory savedResult : resultsOutput){
                    Stmt[] pathSaved = savedResult.getResult().getSource().getPath();
                    Stmt[] newPath = result.getResult().getSource().getPath();
                    if(arePathsEqual(pathSaved,newPath)){
                        check = false;
                    }
                }
                if(check){
                    resultsOutput.add(result);
                }
            }
        }
        return resultsOutput;
    }

    public static boolean arePathsEqual(Stmt[] pathSaved, Stmt[] newPath) {
        if (pathSaved.length != newPath.length) {
            return false;
        }
        for (int i = 0; i < pathSaved.length; i++) {
            String savedString = pathSaved[i].toString();
            String newString = newPath[i].toString();
            if (!savedString.equals(newString)) {
                return false;
            }
        }
        return true;
    }

    public static void outputFunction(List<LeakFactory> results, String name) throws IOException {
        FileWriter fileWriter1 = new FileWriter(outPutFolder +"/" + name + ".txt", true);
        for (LeakFactory result : results){
            fileWriter1.write("Enclosing Component => " + result.getEnclosingComponent() + "\n" + "\n");
            fileWriter1.write("\n");
            fileWriter1.write("Source => "+ result.getResult().getSource().toString() + "\n");
            fileWriter1.write("Sink => "+ result.getResult().getSink().toString() + "\n" + "\n");
            fileWriter1.write("Full Path => " + "\n" );
            if(!(result.getResult().getSource().getPath() == null)){
                for(Stmt statement : result.getResult().getSource().getPath()){
                    fileWriter1.write(result.getiCFG().getMethodOf(statement).toString() + "  -->  " + statement.toString() + "\n");
                }
            }
            else {
                fileWriter1.write("NO PATH" + "\n" );
            }
            fileWriter1.write(  "\n" + "--------------------------" + "\n" + "\n");
        }
        fileWriter1.close();
    }
    public static List<LeakFactory> findLeak(String apkPath) throws XmlPullParserException, IOException, URISyntaxException {
        List<LeakFactory> leakList = new ArrayList<LeakFactory>();
        FlowDetection leakInstance = new FlowDetection(apkPath,androidJarPath,sinkSourceFilePath,"POWER");
        InfoflowResults leakResults = leakInstance.getResults();
        if(!(leakResults.getResultSet() == null)){
            for(DataFlowResult resultLeak : leakResults.getResultSet()){
                LeakFactory leak = new LeakFactory(resultLeak,leakInstance.getICFG(),leakInstance.getGraph(),leakInstance.getAllComponents(), apkPath,"POWER" );
                leakList.add(leak);
            }
        }
        return leakList;
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
}