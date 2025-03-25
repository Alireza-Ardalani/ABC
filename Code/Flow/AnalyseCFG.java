package org.example;

import soot.jimple.toolkits.callgraph.CallGraph;
import soot.*;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.Chain;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.*;

public class AnalyseCFG {

    private final String name;
    private final CallGraph CFG;
    private final String packageName;
    private final Set<SootMethod> targetMethods;
    private final Set<SootMethod> entryMethods;
    private final int maxParallelTasks;

    private final Queue<List<SootMethod>> allPaths = new ConcurrentLinkedQueue<>();
    private final long startTime;

    public AnalyseCFG(CallGraph inputCFG, Set<SootMethod> inputEntryMethods,
                      Set<SootMethod> inputTargetMethods, String inputName, String outputFolder,
                      String resultFolder, String apkPath, String packageName) throws IOException {
        this.CFG = inputCFG;
        this.entryMethods = inputEntryMethods;
        this.targetMethods = inputTargetMethods;
        this.name = inputName;
        this.maxParallelTasks = Runtime.getRuntime().availableProcessors();
        this.startTime = System.currentTimeMillis();
        this.packageName = packageName;
        runAnalyse(outputFolder, resultFolder, apkPath);
    }
    private void runAnalyse(String outputFolder, String resultFolder, String apkPath) throws IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(maxParallelTasks);
        List<Future<?>> futures = new ArrayList<>();

        for (SootMethod targetMethod : targetMethods) {
            Future<?> future = executorService.submit(() -> {
                log("Start processing target method => " + targetMethod);
                findAllPathsFromTarget(targetMethod);
                Deque<SootMethod> stack = new ArrayDeque<>();
                stack.push(targetMethod);
                Set<SootMethod> all = new HashSet<>();
            });
            futures.add(future);
        }

        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(1000, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        if (!allPaths.isEmpty()) {
            log("Processing found paths...");
            List<List<SootMethod>> allPathsClean = removeDouplication(new ArrayList<>(allPaths));
            ThreadDetection threadDetection = new ThreadDetection(allPathsClean, name, outputFolder, resultFolder, apkPath, "Threshold");
        } else {
            log("No paths found.");
            moveAPK(apkPath, outputFolder);
        }
    }

    private void AAA(Deque<SootMethod> stack, Set<SootMethod> all){
        SootMethod currentMethod =  stack.pop();
        while(true){
            if(!all.contains(currentMethod)){
                all.add(currentMethod);
                break;
            }
            if(!stack.isEmpty()){
                currentMethod =  stack.pop();
            }
            else {
                System.out.println(all.size());
                return;
            }
        }
        Iterator<Edge> edges = CFG.edgesInto(currentMethod);
        while(edges.hasNext()){
            Edge edge = edges.next();
            SootMethod src = edge.src();
            stack.push(src);
        }
        AAA(stack,all);
    }
    private void findAllPathsFromTarget(SootMethod targetMethod) {
        Set<SootMethod> visited = new HashSet<>();
        List<SootMethod> currentPath = new ArrayList<>();
        long timeoutMillis = 1000 * 1000;

        Deque<SootMethod> stack = new ArrayDeque<>();
        stack.push(targetMethod);
        visited.add(targetMethod);

        while (!stack.isEmpty()) {
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                log("Timeout reached, storing partial results...");
                break;
            }

            SootMethod currentMethod = stack.pop();
            if(checkSupper(currentMethod.getName()) && !currentMethod.getSignature().contains(packageName)){
                System.out.println(currentMethod.getName());
                currentPath.remove(currentMethod);  // Backtrack immediately
                continue;
            }
            if (currentMethod.getSignature().contains("<androidx.")||
                    currentMethod.getSignature().contains("<com.google.android")) {
                currentPath.remove(currentMethod);
                continue;
            }
            currentPath.add(currentMethod);

            if (entryMethods.contains(currentMethod)) {
                log("Path found to entry => " + currentMethod);
                allPaths.add(new ArrayList<>(currentPath));
                currentPath.remove(currentMethod);
                continue;
            }

            boolean hasUnvisitedTargets = false;
            Iterator<Edge> edges = CFG.edgesInto(currentMethod);

            while (edges.hasNext()) {
                Edge edge = edges.next();
                SootMethod src = edge.src();

                if (!visited.contains(src)) {
                    hasUnvisitedTargets = true;
                    visited.add(src);
                    stack.push(src);
                    log("Exploring caller method => " + src);
                }
            }

            if (!hasUnvisitedTargets) {
                visited.remove(currentMethod);
                currentPath.remove(currentMethod);
            }
        }
    }

    private List<List<SootMethod>> removeDouplication(List<List<SootMethod>> allPaths) {
        Set<String> pathChainSet = new HashSet<>();
        List<List<SootMethod>> allPathClean = new ArrayList<>();
        for (List<SootMethod> paths : allPaths) {
            StringBuilder pathChain = new StringBuilder();
            for (SootMethod method : paths) {
                pathChain.append("->").append(method);
            }
            if (pathChainSet.add(pathChain.toString())) {
                allPathClean.add(paths);
            }
        }
        return allPathClean;
    }

    private static void moveAPK(String apkPath, String finishFolder) {
        File sourceFile = new File(apkPath);
        File destinationDirectory = new File(finishFolder);

        if (!sourceFile.exists() || !destinationDirectory.exists()) {
            System.err.println("Source file or destination directory does not exist.");
            return;
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

    private void log(String message) {
        System.out.println("[LOG] " + message);
    }
    private boolean checkSupper(String method){
        List<String> X = Arrays.asList(
                "afterTextChanged",
                "beforeTextChanged",
                "onActivityCreated",
                "onActivityResult",
                "onAttach",
                "onAttachedToWindow",
                "onBackPressed",
                "onCancel",
                "onConfigurationChanged",
                "onContextItemSelected",
                "onContextMenuClosed",
                "onCreate",
                "onCreateApplication",
                "onCreateContextMenu",
                "onCreateDialog",
                "onCreateOptionsMenu",
                "onCreateView",
                "onDestroy",
                "onDestroyView",
                "onDetach",
                "onDetachedFromWindow",
                "onDismiss",
                "onDraw",
                "onEditorAction",
                "onFling",
                "onItemClick",
                "onItemLongClick",
                "onLayout",
                "onLongPress",
                "onMeasure",
                "onNewIntent",
                "onOptionsItemSelected",
                "onPause",
                "onPostCreate",
                "onPostResume",
                "onPrepareOptionsMenu",
                "onReceive",
                "onRequestPermissionsResult",
                "onRestart",
                "onRestoreInstanceState",
                "onResume",
                "onResumeFragments",
                "onSaveInstanceState",
                "onScroll",
                "onScrollStateChanged",
                "onScrolled",
                "onShowPress",
                "onSingleTapUp",
                "onSizeChanged",
                "onStart",
                "onStop",
                "onTextChanged",
                "onUserInteraction",
                "onUserLeaveHint",
                "onViewCreated",
                "onViewStateRestored",
                "onWindowFocusChanged",
                "onWindowVisibilityChanged",
                "onCreatePanelMenu",
                "findViewById",
                "setContentView"
        );
        if(X.contains(method)){
            return  true;
        }
        return  false;
    }
}
