package org.example;

import soot.SootClass;
import soot.SootMethod;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class ThreadDetection {
    public ThreadDetection(List<List<SootMethod>> allPathsClean, String name, String outputFolder, String resultFolder, String apkPath, String duration) throws IOException {
        startAnalyse(allPathsClean, name, outputFolder, resultFolder, apkPath, duration);
    }

    private void startAnalyse(List<List<SootMethod>> allPathsClean, String name, String outputFolder, String resultFolder, String apkPath, String duration) throws IOException {
        List<String> results = new ArrayList<>();
        List<String> UIPaths = new ArrayList<>();
        Map<String,Integer> mapUI = new HashMap<>();
        Map<String,Integer> mapWorker = new HashMap<>();
        int UI = 0;
        int Worker = 0;
        for (List<SootMethod> path : allPathsClean) {
            Boolean isUIThread = true;
            String pathResult = "";
            String finalMethod = "";
            Collections.reverse(path);
            for (SootMethod method : path) {
                finalMethod = method.getSignature();
                if (isUIThread) {
                    pathResult = pathResult + method.getSignature() + "   [UI] -->\n";
                } else {
                    pathResult = pathResult + method.getSignature() + "   [Worker] -->\n";
                }
                if (isBackgroundMethod(method)) {
                    isUIThread = false;
                } else if (postsToUIThread(method)) {
                    isUIThread = true;
                } else if (isLifecycle(method.getSignature())) {
                    isUIThread = true;
                }
            }
            if (isUIThread) {
                UIPaths.add(pathResult);
                UI++;
                if(mapUI.containsKey(finalMethod)){
                    mapUI.put(finalMethod,mapUI.get(finalMethod)+1);
                }
                else {
                    mapUI.put(finalMethod,1);
                }
            } else {
                Worker++;
                if(mapWorker.containsKey(finalMethod)){
                    mapWorker.put(finalMethod,mapWorker.get(finalMethod)+1);
                }
                else {
                    mapWorker.put(finalMethod,1);
                }
            }
            results.add(pathResult);
        }
        saveResult(UI, Worker, results, name, outputFolder, resultFolder, apkPath, duration, mapUI, mapWorker, UIPaths);
    }

    private void saveResult(int ui, int worker, List<String> results, String name, String outputFolder, String resultFolder, String apkPath, String duration, Map<String, Integer> mapUI, Map<String, Integer> mapWorker, List<String> UIPaths) throws IOException {
        FileWriter fileWriter = new FileWriter(resultFolder + "/" + name + ".txt", true);
        fileWriter.write("Duration => " + duration + "\n");
        fileWriter.write("UI Thread => " + ui + "\n");
        fileWriter.write("Worker Thread => " + worker + "\n\n");
        fileWriter.write("----------------StartUI----------------\n");
        mapUI.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    try {
                        fileWriter.write(entry.getKey() + ": " + entry.getValue() + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        fileWriter.write("----------------StartWorker----------------\n");
        mapWorker.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> {
                    try {
                        fileWriter.write(entry.getKey() + ": " + entry.getValue() + "\n");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
        fileWriter.close();
        FileWriter fileWriter1 = new FileWriter(resultFolder + "/" + name + "_fullPath.txt", true);
        for (String result : results) {
            fileWriter1.write(result);
            fileWriter1.write("---------------------------------------------------------------");
            fileWriter1.write("\n");
        }
        fileWriter1.close();

        FileWriter fileWriter2 = new FileWriter(resultFolder + "/" + name + "_UIThread.txt", true);
        for (String result : UIPaths) {
            fileWriter2.write(result);
            fileWriter2.write("---------------------------------------------------------------");
            fileWriter2.write("\n");
        }
        fileWriter2.close();


       moveAPK(apkPath,outputFolder);
    }

    private static boolean isLifecycle(String signature) {
        List<String> lifecycleMethods = Arrays.asList(
                "onCreate",
                "onStart",
                "onResume",
                "onPause",
                "onStop",
                "onDestroy",
                "onRestart",
                "onSaveInstanceState",
                "onRestoreInstanceState",
                "onAttach",
                "onDetach",
                "onActivityResult"
        );

        for (String lifecycleMethod : lifecycleMethods) {
            if (signature.contains(lifecycleMethod)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isBackgroundMethod(SootMethod method) {
        String signature = method.getSignature();
        List<String> backgroundSignatures = Arrays.asList(
                "<java.util.concurrent.ExecutorService: void execute(java.lang.Runnable)>",
                "<java.util.concurrent.ExecutorService: java.util.concurrent.Future submit(java.lang.Runnable)>",
                "<java.util.concurrent.ExecutorService: java.util.concurrent.Future submit(java.util.concurrent.Callable)>",
                "<java.util.concurrent.ExecutorService: java.util.List invokeAll(java.util.Collection)>",
                "<java.util.concurrent.ExecutorService: java.lang.Object invokeAny(java.util.Collection)>",
                "<java.util.concurrent.Executor: void execute(java.lang.Runnable)>",
                "<java.util.concurrent.ThreadPoolExecutor: void execute(java.lang.Runnable)>",
                "<java.util.concurrent.ScheduledThreadPoolExecutor: java.util.concurrent.ScheduledFuture schedule(java.lang.Runnable, long, java.util.concurrent.TimeUnit)>",
                "<androidx.work.Worker: androidx.work.ListenableWorker.Result doWork()>",
                "<java.lang.Thread: void <init>(java.lang.Runnable)>",
                "doInBackground",
                "<android.app.IntentService: void onHandleIntent(android.content.Intent)>",
                "<androidx.core.app.JobIntentService: void onHandleWork(android.content.Intent)>",
                "<androidx.work.Worker: androidx.work.ListenableWorker.Result doWork()>",
                "<java.util.concurrent.ScheduledExecutorService: void scheduleAtFixedRate(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)>",
                "<java.util.concurrent.ScheduledExecutorService: void scheduleWithFixedDelay(java.lang.Runnable, long, long, java.util.concurrent.TimeUnit)>",
                "<android.app.job.JobService: boolean onStartJob(android.app.job.JobParameters)>",
                "<kotlinx.coroutines.Dispatchers: Dispatchers.IO>",
                "<kotlinx.coroutines.Dispatchers: Dispatchers.Default>",
                "runBlocking"
        );
=
        for (String bgSignature : backgroundSignatures) {
            if (signature.contains(bgSignature)) {
                return true;
            }
        }

        SootClass declaringClass = method.getDeclaringClass();
        if (declaringClass.isConcrete()) {
            if (extendsThread(declaringClass) || implementsRunnable(declaringClass)) {
                //Do all runnable implementation meaning thread?
                return true;
            }
        }

        return false;
    }

    private static boolean postsToUIThread(SootMethod method) {
        String signature = method.getSignature();
        List<String> uiPostSignatures = Arrays.asList(
                "runOnUiThread",
                "<android.view.View: boolean post(java.lang.Runnable)>",
                "void onPostExecute",
                "<android.os.Handler: boolean post(java.lang.Runnable)>",
                "<android.os.Handler: boolean postDelayed(java.lang.Runnable, long)>",
                "<android.os.Handler: boolean sendMessage(android.os.Message)>",
                "<android.os.Handler: boolean sendMessageAtFrontOfQueue(android.os.Message)>",
                "<android.os.Handler: boolean sendMessageDelayed(android.os.Message, long)>",
                "<android.app.Activity: void runOnUiThread(java.lang.Runnable)>",
                "<kotlinx.coroutines.Dispatchers: Dispatchers.Main>",
                "<androidx.lifecycle.LiveData: void observeForever(androidx.lifecycle.Observer)>",
                "<androidx.lifecycle.LiveData: void removeObserver(androidx.lifecycle.Observer)>"
        );

        for (String uiSignature : uiPostSignatures) {
            if (signature.contains(uiSignature) & ishandlerWithUILooper(method) ) {
                return true;
            }
        }

        return false;
    }


    private static boolean extendsThread(SootClass sootClass) {
        while (sootClass != null) {
            if (sootClass.getName().equals("java.lang.Thread")) {
                return true;
            }
            if (sootClass.hasSuperclass()) {
                sootClass = sootClass.getSuperclass();
            } else {
                return false;
            }
        }
        return false;
    }

    private static boolean implementsRunnable(SootClass sootClass) {
        while (sootClass != null) {
            for (SootClass iface : sootClass.getInterfaces()) {
                if (iface.getName().equals("java.lang.Runnable")) {
                    return true;
                }
            }
            if (sootClass.hasSuperclass()) {
                sootClass = sootClass.getSuperclass();
            } else {
                return false;
            }
        }
        return false;
    }
    private static void moveAPK(String apkPath,String finishFolder) {
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
