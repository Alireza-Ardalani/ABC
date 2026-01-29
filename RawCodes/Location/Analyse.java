package org.example;

import soot.*;
import soot.jimple.infoflow.solver.cfg.InfoflowCFG;
import soot.jimple.Stmt;
import soot.util.Chain;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.*;

public class Analyse {
    private final String name;
    private final InfoflowCFG infCFG;
    private final Set<Stmt> targetStmts;
    private final Set<SootMethod> entryMethods;
    private final int maxParallelTasks;
    private  final Set<String> packageNames;
    private final Queue<List<SootMethod>> allPaths = new ConcurrentLinkedQueue<>();
    private final long startTime;

    public Analyse(InfoflowCFG infCFG, Set<SootMethod> inputEntryMethods,
                   Set<Stmt> inputTargetStmts, String inputName, String outputFolder,
                   String resultFolder, String apkPath, Set<String> packageNames) throws IOException {
        this.infCFG = infCFG;
        this.entryMethods = inputEntryMethods;
        this.targetStmts = inputTargetStmts;
        this.name = inputName;
        this.maxParallelTasks = Runtime.getRuntime().availableProcessors();
        this.startTime = System.currentTimeMillis();
        this.packageNames = packageNames;
        runAnalyse(outputFolder, resultFolder, apkPath);
    }
    private void runAnalyse(String outputFolder, String resultFolder, String apkPath) throws IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(maxParallelTasks);
        for (Stmt targetStmt : targetStmts) {
            Future<?> future = executorService.submit(() -> {
                SootMethod method = targetStmt.getInvokeExpr().getMethod();
                findAllPathsFromTarget(targetStmt, method );
            });
        }
        executorService.shutdown();
        boolean completedWithinThreshold = true;
        try {
            if (!executorService.awaitTermination(900, TimeUnit.SECONDS)) {
                completedWithinThreshold = false;
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            completedWithinThreshold = false;
            executorService.shutdownNow();
        }
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        String analysisStatus = completedWithinThreshold ? String.valueOf(duration) : "Threshold";
        if (!allPaths.isEmpty()) {
            List<List<SootMethod>> allPathsClean = removeDuplication(new ArrayList<>(allPaths));
            ThreadDetection threadDetection = new ThreadDetection(allPathsClean, name, outputFolder, resultFolder, apkPath, analysisStatus);
        } else {
            System.out.println("NoResult!!");
            moveAPK(apkPath, outputFolder);
            FileWriter w = new FileWriter(resultFolder + "/" + "Issue.txt",true);
            w.write(name + " -*-> " + "NoResult" + "\n");
            w.close();
        }
    }

    private void findAllPathsFromTarget(Stmt targetStmt, SootMethod method) {
        Set<SootMethod> visitedMethods = new HashSet<>();
        List<SootMethod> currentPath = new ArrayList<>();
        currentPath.add(0,method);
        long timeoutMillis = 1000 * 1000;

        SootMethod targetMethod = infCFG.getMethodOf(targetStmt);
        Deque<SootMethod> stack = new ArrayDeque<>();
        stack.push(targetMethod);
        visitedMethods.add(targetMethod);

        while (!stack.isEmpty()) {
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                log("Timeout reached, storing partial results...");
                break;
            }
            SootMethod currentMethod = stack.pop();
            boolean packageMatch = packageNames.stream().anyMatch(currentMethod.getSignature()::contains);
            if(checkSupper(currentMethod.getName()) && !packageMatch ){
                currentPath.remove(currentMethod);
                continue;
            }

            if (currentMethod.getSignature().contains("<androidx.") ||
                    currentMethod.getSignature().contains("<com.google.") ||
                    currentMethod.getSignature().contains("<com.inmobi.") ||
                    currentMethod.getSignature().contains("<okhttp3.") ||
                    currentMethod.getSignature().contains("<io.reactivex.") ||
                    currentMethod.getSignature().contains("<android.support.") ||
                    currentMethod.getSignature().contains("com.facebook.") ||
                    currentMethod.getSignature().contains("<com.startapp.") ||
                    currentMethod.getSignature().contains("<com.yandex.") ||
                    currentMethod.getSignature().contains("<com.ironsource.") ||
                    currentMethod.getSignature().contains("<com.baidu.") ||
                    currentMethod.getSignature().contains("<net.hockeyapp.") ||
                    currentMethod.getSignature().contains("<com.unity3d.") ||
                    currentMethod.getSignature().contains("<com.digits.sdk.") ||
                    currentMethod.getSignature().contains("<com.mopub.") ||
                    currentMethod.getSignature().contains("<io.card.") ||
                    currentMethod.getSignature().contains("<com.paypal.") ||
                    currentMethod.getSignature().contains("<com.umeng.") ||
                    currentMethod.getSignature().contains("<org.jsoup.") ||
                    currentMethod.getSignature().contains("<com.purplebrain.") ||
                    currentMethod.getSignature().contains("<com.tencent.") ||
                    currentMethod.getSignature().contains("<com.purplebrain.") ||
                    currentMethod.getSignature().contains("<com.bumptech.") ||
                    currentMethod.getSignature().contains("<com.twinkly") ||
                    currentMethod.getSignature().contains("com.flurry.") ||
                    currentMethod.getSignature().contains("<com.facebook.") ||
                    currentMethod.getSignature().contains("<com.mopub.") ||
                    currentMethod.getSignature().contains("<net.hockeyapp.") ||
                    currentMethod.getSignature().contains("<com.twitter.") ||
                    currentMethod.getSignature().contains("<com.amazon.") ||
                    currentMethod.getSignature().contains("<io.card.") ||
                    currentMethod.getSignature().contains("<com.applovin.") ||
                    currentMethod.getSignature().contains("<com.flurry.") ||
                    currentMethod.getSignature().contains("<com.appboy.") ||
                    currentMethod.getSignature().contains("<com.unity3d.") ||
                    currentMethod.getSignature().contains("<com.urbanairship.") ||
                    currentMethod.getSignature().contains("<com.millennialmedia.") ||
                    currentMethod.getSignature().contains("<com.inmobi.") ||
                    currentMethod.getSignature().contains("<com.digits.") ||
                    currentMethod.getSignature().contains("<com.my.") ||
                    currentMethod.getSignature().contains("<com.startapp.") ||
                    currentMethod.getSignature().contains("<com.zendesk.") ||
                    currentMethod.getSignature().contains("<com.braintreepayments.") ||
                    currentMethod.getSignature().contains("<com.vungle.") ||
                    currentMethod.getSignature().contains("<com.onesignal.") ||
                    currentMethod.getSignature().contains("<com.tapjoy.") ||
                    currentMethod.getSignature().contains("<com.jirbo.") ||
                    currentMethod.getSignature().contains("<com.jakewharton.") ||
                    currentMethod.getSignature().contains("<io.intercom.") ||
                    currentMethod.getSignature().contains("<com.chartboost.") ||
                    currentMethod.getSignature().contains("<com.apptentive.") ||
                    currentMethod.getSignature().contains("<com.mobvista.") ||
                    currentMethod.getSignature().contains("<com.localytics.") ||
                    currentMethod.getSignature().contains("<com.uservoice.") ||
                    currentMethod.getSignature().contains("<com.revmob.") ||
                    currentMethod.getSignature().contains("<com.mixpanel.") ||
                    currentMethod.getSignature().contains("<com.yalantis.") ||
                    currentMethod.getSignature().contains("<com.seattleclouds.") ||
                    currentMethod.getSignature().contains("<de.appplant.") ||
                    currentMethod.getSignature().contains("<com.journeyapps.") ||
                    currentMethod.getSignature().contains("<com.theartofdev.") ||
                    currentMethod.getSignature().contains("<net.sourceforge.") ||
                    currentMethod.getSignature().contains("<epic.mychart.") ||
                    currentMethod.getSignature().contains("<com.paypal.") ||
                    currentMethod.getSignature().contains("<com.adcolony.") ||
                    currentMethod.getSignature().contains("<com.smaato.") ||
                    currentMethod.getSignature().contains("<com.karumi.") ||
                    currentMethod.getSignature().contains("<com.adobe.") ||
                    currentMethod.getSignature().contains("<com.americanwell.") ||
                    currentMethod.getSignature().contains("<com.foresee.") ||
                    currentMethod.getSignature().contains("<com.vervewireless.") ||
                    currentMethod.getSignature().contains("<com.crashlytics.") ||
                    currentMethod.getSignature().contains("<com.soundcloud.") ||
                    currentMethod.getSignature().contains("<com.roximity.") ||
                    currentMethod.getSignature().contains("<com.mikepenz.") ||
                    currentMethod.getSignature().contains("<org.nexage.") ||
                    currentMethod.getSignature().contains("<io.fabric.") ||
                    currentMethod.getSignature().contains("<com.helpshift.") ||
                    currentMethod.getSignature().contains("<com.inner.") ||
                    currentMethod.getSignature().contains("<com.tencent.") ||
                    currentMethod.getSignature().contains("<com.zjsoft.") ||
                    currentMethod.getSignature().contains("<com.brightcove.") ||
                    currentMethod.getSignature().contains("<com.admarvel.") ||
                    currentMethod.getSignature().contains("<com.supersonicads.") ||
                    currentMethod.getSignature().contains("<com.stripe.") ||
                    currentMethod.getSignature().contains("<com.bmob.") ||
                    currentMethod.getSignature().contains("<com.wsi.") ||
                    currentMethod.getSignature().contains("<com.wemob.") ||
                    currentMethod.getSignature().contains("<com.rfm.") ||
                    currentMethod.getSignature().contains("<ti.modules.") ||
                    currentMethod.getSignature().contains("<com.yandex.") ||
                    currentMethod.getSignature().contains("<org.appcelerator.") ||
                    currentMethod.getSignature().contains("<com.pushwoosh.") ||
                    currentMethod.getSignature().contains("<net.openid.") ||
                    currentMethod.getSignature().contains("<com.mob.") ||
                    currentMethod.getSignature().contains("<com.appbrain.") ||
                    currentMethod.getSignature().contains("<com.optimizely.") ||
                    currentMethod.getSignature().contains("<com.spotify.") ||
                    currentMethod.getSignature().contains("<com.mobfox.") ||
                    currentMethod.getSignature().contains("<com.lifestreet.") ||
                    currentMethod.getSignature().contains("<pub.devrel.") ||
                    currentMethod.getSignature().contains("<com.ironsource.") ||
                    currentMethod.getSignature().contains("<com.aerserv.") ||
                    currentMethod.getSignature().contains("<com.nhaarman.") ||
                    currentMethod.getSignature().contains("<com.instabug.") ||
                    currentMethod.getSignature().contains("<eu.janmuller.") ||
                    currentMethod.getSignature().contains("<com.swrve.") ||
                    currentMethod.getSignature().contains("<com.vk.") ||
                    currentMethod.getSignature().contains("<io.presage.") ||
                    currentMethod.getSignature().contains("<com.tbruyelle.") ||
                    currentMethod.getSignature().contains("<com.dropbox.") ||
                    currentMethod.getSignature().contains("<com.usebutton.") ||
                    currentMethod.getSignature().contains("<mobi.infolife.") ||
                    currentMethod.getSignature().contains("<com.tremorvideo.") ||
                    currentMethod.getSignature().contains("<com.amber.") ||
                    currentMethod.getSignature().contains("<com.monet.") ||
                    currentMethod.getSignature().contains("<com.inneractive.") ||
                    currentMethod.getSignature().contains("<com.apptracker.") ||
                    currentMethod.getSignature().contains("<com.sina.") ||
                    currentMethod.getSignature().contains("<com.snipermob.") ||
                    currentMethod.getSignature().contains("<com.cootek.") ||
                    currentMethod.getSignature().contains("<com.squareup.") ||
                    currentMethod.getSignature().contains("<com.sponsorpay.") ||
                    currentMethod.getSignature().contains("<com.gbmx.") ||
                    currentMethod.getSignature().contains("<com.pocketprep.") ||
                    currentMethod.getSignature().contains("<com.testfairy.") ||
                    currentMethod.getSignature().contains("<com.netpulse.") ||
                    currentMethod.getSignature().contains("<com.bumptech.") ||
                    currentMethod.getSignature().contains("<com.ihs.") ||
                    currentMethod.getSignature().contains("<com.avocarrot.") ||
                    currentMethod.getSignature().contains("<com.mobutils.") ||
                    currentMethod.getSignature().contains("<com.zopim.") ||
                    currentMethod.getSignature().contains("<com.appnext.") ||
                    currentMethod.getSignature().contains("<com.zjlib.") ||
                    currentMethod.getSignature().contains("<com.visa.") ||
                    currentMethod.getSignature().contains("<org.acra.") ||
                    currentMethod.getSignature().contains("<com.quantcast.") ||
                    currentMethod.getSignature().contains("<com.janrain.") ||
                    currentMethod.getSignature().contains("<com.intowow.") ||
                    currentMethod.getSignature().contains("<cn.jpush.") ||
                    currentMethod.getSignature().contains("<com.gz.") ||
                    currentMethod.getSignature().contains("<com.gigya.") ||
                    currentMethod.getSignature().contains("<com.outfit7.") ||
                    currentMethod.getSignature().contains("<com.nativex.") ||
                    currentMethod.getSignature().contains("<com.pushio.") ||
                    currentMethod.getSignature().contains("<com.ansca.") ||
                    currentMethod.getSignature().contains("<com.adjust.") ||
                    currentMethod.getSignature().contains("<com.commonsware.") ||
                    currentMethod.getSignature().contains("<com.qq.") ||
                    currentMethod.getSignature().contains("<com.layer.") ||
                    currentMethod.getSignature().contains("<com.alipay.") ||
                    currentMethod.getSignature().contains("<applovin.sdk.") ||
                    currentMethod.getSignature().contains("<com.clevertap.") ||
                    currentMethod.getSignature().contains("<com.hyprmx.") ||
                    currentMethod.getSignature().contains("<com.loopme.") ||
                    currentMethod.getSignature().contains("<com.surveymonkey.") ||
                    currentMethod.getSignature().contains("<com.jumio.") ||
                    currentMethod.getSignature().contains("<com.duapps.") ||
                    currentMethod.getSignature().contains("<com.carnival.") ||
                    currentMethod.getSignature().contains("<zendesk.support.") ||
                    currentMethod.getSignature().contains("<com.facebook.") ||
                    currentMethod.getSignature().contains("<com.mopub.") ||
                    currentMethod.getSignature().contains("<net.hockeyapp.") ||
                    currentMethod.getSignature().contains("<com.twitter.") ||
                    currentMethod.getSignature().contains("<com.amazon.") ||
                    currentMethod.getSignature().contains("<io.card.") ||
                    currentMethod.getSignature().contains("<com.applovin.") ||
                    currentMethod.getSignature().contains("<com.flurry.") ||
                    currentMethod.getSignature().contains("<com.appboy.") ||
                    currentMethod.getSignature().contains("<com.unity3d.") ||
                    currentMethod.getSignature().contains("<com.urbanairship.") ||
                    currentMethod.getSignature().contains("<com.millennialmedia.") ||
                    currentMethod.getSignature().contains("<com.inmobi.") ||
                    currentMethod.getSignature().contains("<com.digits.") ||
                    currentMethod.getSignature().contains("<com.my.") ||
                    currentMethod.getSignature().contains("<com.startapp.") ||
                    currentMethod.getSignature().contains("<com.zendesk.") ||
                    currentMethod.getSignature().contains("<com.braintreepayments.") ||
                    currentMethod.getSignature().contains("<com.vungle.") ||
                    currentMethod.getSignature().contains("<com.onesignal.") ||
                    currentMethod.getSignature().contains("<com.tapjoy.") ||
                    currentMethod.getSignature().contains("<com.jirbo.") ||
                    currentMethod.getSignature().contains("<com.jakewharton.") ||
                    currentMethod.getSignature().contains("<io.intercom.") ||
                    currentMethod.getSignature().contains("<com.chartboost.") ||
                    currentMethod.getSignature().contains("<com.apptentive.") ||
                    currentMethod.getSignature().contains("<com.mobvista.") ||
                    currentMethod.getSignature().contains("<com.localytics.") ||
                    currentMethod.getSignature().contains("<com.uservoice.") ||
                    currentMethod.getSignature().contains("<com.revmob.") ||
                    currentMethod.getSignature().contains("<com.mixpanel.") ||
                    currentMethod.getSignature().contains("<com.yalantis.") ||
                    currentMethod.getSignature().contains("<com.seattleclouds.") ||
                    currentMethod.getSignature().contains("<de.appplant.") ||
                    currentMethod.getSignature().contains("<com.journeyapps.") ||
                    currentMethod.getSignature().contains("<com.theartofdev.") ||
                    currentMethod.getSignature().contains("<net.sourceforge.") ||
                    currentMethod.getSignature().contains("<epic.mychart.") ||
                    currentMethod.getSignature().contains("<com.paypal.") ||
                    currentMethod.getSignature().contains("<com.adcolony.") ||
                    currentMethod.getSignature().contains("<com.smaato.") ||
                    currentMethod.getSignature().contains("<com.karumi.") ||
                    currentMethod.getSignature().contains("<com.adobe.") ||
                    currentMethod.getSignature().contains("<com.americanwell.") ||
                    currentMethod.getSignature().contains("<com.foresee.") ||
                    currentMethod.getSignature().contains("<com.vervewireless.") ||
                    currentMethod.getSignature().contains("<com.crashlytics.") ||
                    currentMethod.getSignature().contains("<com.soundcloud.") ||
                    currentMethod.getSignature().contains("<com.roximity.") ||
                    currentMethod.getSignature().contains("<com.mikepenz.") ||
                    currentMethod.getSignature().contains("<org.nexage.") ||
                    currentMethod.getSignature().contains("<io.fabric.") ||
                    currentMethod.getSignature().contains("<com.helpshift.") ||
                    currentMethod.getSignature().contains("<com.inner.") ||
                    currentMethod.getSignature().contains("<com.tencent.") ||
                    currentMethod.getSignature().contains("<com.zjsoft.") ||
                    currentMethod.getSignature().contains("<com.brightcove.") ||
                    currentMethod.getSignature().contains("<com.admarvel.") ||
                    currentMethod.getSignature().contains("<com.supersonicads.") ||
                    currentMethod.getSignature().contains("<com.stripe.") ||
                    currentMethod.getSignature().contains("<com.bmob.") ||
                    currentMethod.getSignature().contains("<com.wsi.") ||
                    currentMethod.getSignature().contains("<com.wemob.") ||
                    currentMethod.getSignature().contains("<com.rfm.") ||
                    currentMethod.getSignature().contains("<ti.modules.") ||
                    currentMethod.getSignature().contains("<com.yandex.") ||
                    currentMethod.getSignature().contains("<org.appcelerator.") ||
                    currentMethod.getSignature().contains("<com.pushwoosh.") ||
                    currentMethod.getSignature().contains("<net.openid.") ||
                    currentMethod.getSignature().contains("<com.mob.") ||
                    currentMethod.getSignature().contains("<com.appbrain.") ||
                    currentMethod.getSignature().contains("<com.optimizely.") ||
                    currentMethod.getSignature().contains("<com.spotify.") ||
                    currentMethod.getSignature().contains("<com.mobfox.") ||
                    currentMethod.getSignature().contains("<com.lifestreet.") ||
                    currentMethod.getSignature().contains("<pub.devrel.") ||
                    currentMethod.getSignature().contains("<com.ironsource.") ||
                    currentMethod.getSignature().contains("<com.aerserv.") ||
                    currentMethod.getSignature().contains("<com.nhaarman.") ||
                    currentMethod.getSignature().contains("<com.instabug.") ||
                    currentMethod.getSignature().contains("<eu.janmuller.") ||
                    currentMethod.getSignature().contains("<com.swrve.") ||
                    currentMethod.getSignature().contains("<com.vk.") ||
                    currentMethod.getSignature().contains("<io.presage.") ||
                    currentMethod.getSignature().contains("<com.tbruyelle.") ||
                    currentMethod.getSignature().contains("<com.dropbox.") ||
                    currentMethod.getSignature().contains("<com.usebutton.") ||
                    currentMethod.getSignature().contains("<mobi.infolife.") ||
                    currentMethod.getSignature().contains("<com.tremorvideo.") ||
                    currentMethod.getSignature().contains("<com.amber.") ||
                    currentMethod.getSignature().contains("<com.monet.") ||
                    currentMethod.getSignature().contains("<com.inneractive.") ||
                    currentMethod.getSignature().contains("<com.apptracker.") ||
                    currentMethod.getSignature().contains("<com.sina.") ||
                    currentMethod.getSignature().contains("<com.snipermob.") ||
                    currentMethod.getSignature().contains("<com.cootek.") ||
                    currentMethod.getSignature().contains("<com.squareup.") ||
                    currentMethod.getSignature().contains("<com.sponsorpay.") ||
                    currentMethod.getSignature().contains("<com.gbmx.") ||
                    currentMethod.getSignature().contains("<com.pocketprep.") ||
                    currentMethod.getSignature().contains("<com.testfairy.") ||
                    currentMethod.getSignature().contains("<com.netpulse.") ||
                    currentMethod.getSignature().contains("<com.bumptech.") ||
                    currentMethod.getSignature().contains("<com.ihs.") ||
                    currentMethod.getSignature().contains("<com.avocarrot.") ||
                    currentMethod.getSignature().contains("<com.mobutils.") ||
                    currentMethod.getSignature().contains("<com.zopim.") ||
                    currentMethod.getSignature().contains("<com.appnext.") ||
                    currentMethod.getSignature().contains("<com.zjlib.") ||
                    currentMethod.getSignature().contains("<com.visa.") ||
                    currentMethod.getSignature().contains("<org.acra.") ||
                    currentMethod.getSignature().contains("<com.quantcast.") ||
                    currentMethod.getSignature().contains("<com.janrain.") ||
                    currentMethod.getSignature().contains("<com.intowow.") ||
                    currentMethod.getSignature().contains("<cn.jpush.") ||
                    currentMethod.getSignature().contains("<com.gz.") ||
                    currentMethod.getSignature().contains("<com.gigya.") ||
                    currentMethod.getSignature().contains("<com.outfit7.") ||
                    currentMethod.getSignature().contains("<com.nativex.") ||
                    currentMethod.getSignature().contains("<com.pushio.") ||
                    currentMethod.getSignature().contains("<com.ansca.") ||
                    currentMethod.getSignature().contains("<com.adjust.") ||
                    currentMethod.getSignature().contains("<com.commonsware.") ||
                    currentMethod.getSignature().contains("<com.qq.") ||
                    currentMethod.getSignature().contains("<com.layer.") ||
                    currentMethod.getSignature().contains("<com.alipay.") ||
                    currentMethod.getSignature().contains("<applovin.sdk.") ||
                    currentMethod.getSignature().contains("<com.clevertap.") ||
                    currentMethod.getSignature().contains("<com.hyprmx.") ||
                    currentMethod.getSignature().contains("<com.loopme.") ||
                    currentMethod.getSignature().contains("<com.surveymonkey.") ||
                    currentMethod.getSignature().contains("<com.jumio.") ||
                    currentMethod.getSignature().contains("<com.duapps.") ||
                    currentMethod.getSignature().contains("<com.carnival.") ||
                    currentMethod.getSignature().contains("<zendesk.support.")){
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

            Collection<Unit> callers = infCFG.getCallersOf(currentMethod);

            for (Unit caller : callers) {
                SootMethod callerMethod = infCFG.getMethodOf(caller);
                if (!visitedMethods.contains(callerMethod)) {
                    hasUnvisitedTargets = true;
                    visitedMethods.add(callerMethod);
                    stack.push(callerMethod);
                    log("Exploring caller method => " + callerMethod);
                }
            }
            if (!hasUnvisitedTargets) {
                visitedMethods.remove(currentMethod);
                currentPath.remove(currentMethod);
            }
        }
    }

    private List<List<SootMethod>> removeDuplication(List<List<SootMethod>> allPaths) {
        Set<Integer> pathHashSet = new HashSet<>();
        List<List<SootMethod>> allPathClean = new ArrayList<>();

        for (List<SootMethod> paths : allPaths) {
            int pathHash = paths.hashCode();
            if (pathHashSet.add(pathHash)) {
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
