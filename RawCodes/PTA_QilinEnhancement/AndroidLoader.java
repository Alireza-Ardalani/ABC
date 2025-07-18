package eventBasedSystem;

import qilin.EventBasedEntryPoints;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.results.InfoflowResults;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.Chain;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static soot.jimple.infoflow.InfoflowConfiguration.AliasingAlgorithm.FlowSensitive;
import static soot.jimple.infoflow.InfoflowConfiguration.CodeEliminationMode.NoCodeElimination;
import static soot.jimple.infoflow.InfoflowConfiguration.ImplicitFlowMode.AllImplicitFlows;
import static soot.jimple.infoflow.InfoflowConfiguration.PathBuildingAlgorithm.ContextInsensitive;

public class AndroidLoader {
    static CallGraph callGraph;
    public AndroidLoader(String apkFilePath, String androidJarFilesPath) throws IOException {
        String signaturesFile = "SourceSinkFile.txt";
        FileWriter fw = new FileWriter(signaturesFile);
        fw.write("<android.util.Log: int d(java.lang.String,java.lang.String)> -> _SINK_\n");
        fw.write("<android.location.LocationManager: android.location.Location getLastKnownLocation(java.lang.String)> -> _SOURCE_\n");
        fw.close();
        SetupApplication setupApplication = setupApplication(apkFilePath,androidJarFilesPath);
        InfoflowResults infoflowResults = setupApplication.runInfoflow(new File(signaturesFile));
        File file = new File(signaturesFile);
        if (file.exists()) {
            file.delete();
        }
        callGraph = Scene.v().getCallGraph();
    }

    public void loadEntryPoints(){
        EventBasedEntryPoints eventBasedEntryPoints = new EventBasedEntryPoints();
        eventBasedEntryPoints.setEntryPoints(getEntryFromFlowDroid());
    }

    public Set<String> runThreadDetection(){
        ThreadDetection threadDetection = new ThreadDetection();
        Set<String> init =  new HashSet<>();
        for(var m :  threadDetection.getWorkerThreadStarterMethod()){
            init.add(m.getSignature());
        }
        System.out.println("Threadddddd==> ");
        System.out.println(init);
        return init;
    }
    public Set<String>getComponent(){
        Set<String> components = new HashSet<>();
        for(var  m :  getEntryFromFlowDroid()){
            var temp  = m.getSignature().substring(m.getSignature().indexOf("<")+1, m.getSignature().indexOf(":"));
            components.add(temp);
        }
       return components;
    }

    public Set<String> getByteCodeOrigin (){
        Set<String>  parties = new HashSet<>();
        try {
        for(var clas : Scene.v().getClasses()){
            String[] splitter  =  clas.getName().split("\\.");
            if(splitter.length >= 3){
                if(!splitter[0].contains("android") &&  !splitter[0].contains("javax") &&
                        !splitter[0].contains("java") && !splitter[0].contains("rx") &&
                        !splitter[0].contains("kotlinx") && !splitter[0].contains("kotlin")
                ){
                    if(!clas.getName().toString().trim().startsWith("com.android") &&
                            !clas.getName().toString().trim().startsWith("com.google") &&
                            !clas.getName().toString().trim().startsWith("dalvik.system") &&
                            !clas.getName().toString().trim().startsWith("org.springframework") &&
                            !clas.getName().toString().trim().startsWith("org.json") &&
                            !clas.getName().toString().trim().startsWith("org.xml") &&
                            !clas.getName().toString().trim().startsWith("org.jsoup") &&
                            !clas.getName().toString().trim().startsWith("com.mysql") &&
                            !clas.getName().toString().trim().startsWith("kotlin.jvm") &&
                            !clas.getName().toString().trim().startsWith("org.apache") &&
                            !clas.getName().toString().trim().startsWith("net.sqlcipher") &&
                            !clas.getName().toString().trim().startsWith("io.reactivex")){
                        parties.add(splitter[0]+"."+splitter[1]);
                    }

                }
            } else {
                //Is it happen?
            }
        }

    } catch (Exception e) {
        //JustIgnore
    } catch (Error e) {
        //JustIgnore
    }
        return parties;
    }



    private static SetupApplication setupApplication(String apkFile, String androidJarFilesPath) {
        System.out.println(apkFile);
        SetupApplication setupApplication = new SetupApplication(new File(androidJarFilesPath), new File(apkFile));
        InfoflowAndroidConfiguration config = setupApplication.getConfig();
        config.setMergeDexFiles(true);
        config.setAliasingAlgorithm(FlowSensitive);
        config.setCodeEliminationMode(NoCodeElimination);
        config.getSolverConfiguration().setDataFlowSolver(InfoflowConfiguration.DataFlowSolver.ContextFlowSensitive);
        config.getSolverConfiguration().setSparsePropagationStrategy(InfoflowConfiguration.SparsePropagationStrategy.Precise);
        config.setImplicitFlowMode(AllImplicitFlows);
        config.getPathConfiguration().setPathBuildingAlgorithm(ContextInsensitive);
        config.setMemoryThreshold(1.0d);
        config.setDataFlowTimeout(5);
        config.getPathConfiguration().setMaxPathLength(100);
        config.setLogSourcesAndSinks(true);
        config.getAccessPathConfiguration().setAccessPathLength(10);
        return setupApplication;
    }
    private static Set<SootMethod> getEntryFromFlowDroid() {
        Set<SootMethod> methods = new HashSet<>();
        List<SootMethod> EntryMethods = Scene.v().getEntryPoints();
        for (SootMethod entryMethod : EntryMethods){
            exploreFromEntry(entryMethod,methods);
        }
        return methods;
    }
    private static Set<SootMethod> exploreFromEntry(SootMethod method, Set<SootMethod> methodList) {
        method.retrieveActiveBody();
        if(method.hasActiveBody()){
            Body body =  method.getActiveBody();
            UnitPatchingChain units = body.getUnits();
            for(Unit unit : units){
                Stmt stmt = (Stmt) unit;
                if(stmt.containsInvokeExpr()){
                    SootMethod invokedMethod =  stmt.getInvokeExpr().getMethod();
                    if(invokedMethod.toString().contains("dummyMainMethod")){
                        methodList.addAll(exploreFromEntry(invokedMethod,methodList));
                    }
                    else {
                        methodList.add(invokedMethod);
                    }
                }
            }
        }
        else {}
        return methodList;
    }
    public static void resolveAnonymousClass() {
        Set<String> insertedPairs = new HashSet<>();

        for (SootClass cls : Scene.v().getClasses()) {
            for (SootMethod method : cls.getMethods()) {
                if (method.isAbstract() || !method.hasActiveBody() || method.toString().contains("<init>")) continue;
                //Add this extra! sometimes I think we will get duplication without this(3rd role above "<init>"):
                Body body = method.getActiveBody();
                PatchingChain<Unit> units = body.getUnits();
                Chain<Local> locals = body.getLocals();

                for (Iterator<Unit> it = units.snapshotIterator(); it.hasNext(); ) {
                    Stmt stmt = (Stmt) it.next();
                    if (!stmt.containsInvokeExpr()) continue;

                    InvokeExpr invokeExpr = stmt.getInvokeExpr();
                    if (!(invokeExpr instanceof SpecialInvokeExpr)) continue;

                    SootMethod initMethod = invokeExpr.getMethod();
                    if (!initMethod.getName().equals("<init>")) continue;

                    // Check if this <init> belongs to an anonymous class instantiated with the outer class
                    boolean match = false;

                    for (Type type : initMethod.getParameterTypes()) {
                        if (type.toString().equals(cls.getType().toString())) {
                            match = true;
                            break;
                        }
                    }

                    // Supper class of some type should be insert
                    SootClass initClass = initMethod.getDeclaringClass();
                    while (true) {
                        if (initClass == null) break;
                        String superName = initClass.getName();
                        if (superName.equals("java.lang.Object")) break;
                        if (superName.equals("android.os.AsyncTask")) {
                            match = true;
                            break;
                        }
                        if (!initClass.hasSuperclass()) break;
                        initClass = initClass.getSuperclass();
                    }


                    if (!match) continue;

                    Local base = (Local) ((SpecialInvokeExpr) invokeExpr).getBase();
                    SootClass anonClass = initMethod.getDeclaringClass();

                    List<Unit> newStatements = new ArrayList<>();

                    for (SootMethod targetMethod : anonClass.getMethods()) {
                        if (targetMethod.getName().equals("<init>")) continue;
                        if(targetMethod.getSignature().contains("doInBackground")){
                            if(!targetMethod.getSignature().contains("(java.lang.Object[])"))continue;
                        }
//                      if(targetMethod.getSignature().contains("java.lang.Object doInBackground(java.lang.Object[])")) continue;

                        String callKey = method.getSignature() + " --> " + targetMethod.getSignature();
                        if (insertedPairs.contains(callKey)) continue;

                        List<Value> argList = new ArrayList<>();
                        boolean skip = false;

                        for (Type pType : targetMethod.getParameterTypes()) {
                            Local matchLocal = null;
                            for (Unit u : units) {
                                if (!(u instanceof IdentityStmt || u instanceof AssignStmt)) continue;
                                for (ValueBox vb : u.getUseAndDefBoxes()) {
                                    Value v = vb.getValue();
                                    if (v instanceof Local && v.getType().toString().equals(pType.toString())) {
                                        matchLocal = (Local) v;
                                        break;
                                    }
                                }
                                if (matchLocal != null) break;
                            }

                            if (matchLocal != null) {
                                argList.add(matchLocal);
                            } else {
                                Local dummy = Jimple.v().newLocal("dummy" + pType.toString().replace('.', '_'), pType);
                                locals.add(dummy);

                                if (pType instanceof PrimType) {
                                    Value initVal = IntConstant.v(0);
                                    if (pType instanceof FloatType) initVal = FloatConstant.v(0f);
                                    else if (pType instanceof DoubleType) initVal = DoubleConstant.v(0);
                                    else if (pType instanceof LongType) initVal = LongConstant.v(0L);
                                    newStatements.add(Jimple.v().newAssignStmt(dummy, initVal));
                                } else if (pType instanceof RefType) {
                                    try {
                                        SootClass refCls = Scene.v().getSootClassUnsafe(((RefType) pType).getClassName());
                                        if (refCls != null && !refCls.isAbstract()) {
                                            SootMethod refInit = refCls.getMethodByNameUnsafe("<init>");
                                            if (refInit != null) {
                                                newStatements.add(Jimple.v().newAssignStmt(dummy,
                                                        Jimple.v().newNewExpr((RefType) pType)));
                                                newStatements.add(Jimple.v().newInvokeStmt(
                                                        Jimple.v().newSpecialInvokeExpr(dummy, refInit.makeRef())));
                                            }
                                        }
                                    } catch (Exception e) {
                                        System.out.println("InResolveAnonymousClass Error>> " + e);
                                        skip = true;
                                        break;
                                    }
                                } else if (pType instanceof ArrayType) {
                                    ArrayType arrayType = (ArrayType) pType;
                                    Local dummyArray = dummy;
                                    Value size = IntConstant.v(1);
                                    NewArrayExpr arrayExpr = Jimple.v().newNewArrayExpr(arrayType.getElementType(), size);
                                    newStatements.add(Jimple.v().newAssignStmt(dummyArray, arrayExpr));
                                    if (arrayType.getElementType() instanceof PrimType) {
                                        Value defaultVal = IntConstant.v(0);
                                        if (arrayType.getElementType() instanceof FloatType) defaultVal = FloatConstant.v(0f);
                                        else if (arrayType.getElementType() instanceof DoubleType) defaultVal = DoubleConstant.v(0);
                                        else if (arrayType.getElementType() instanceof LongType) defaultVal = LongConstant.v(0L);
                                        newStatements.add(Jimple.v().newAssignStmt(
                                                Jimple.v().newArrayRef(dummyArray, IntConstant.v(0)), defaultVal));
                                    } else {
                                        newStatements.add(Jimple.v().newAssignStmt(
                                                Jimple.v().newArrayRef(dummyArray, IntConstant.v(0)), NullConstant.v()));
                                    }
                                } else {
                                    System.out.println("InResolveAnonymousClassTypeDoesNotHandle >> " + pType.getClass());
                                    skip = true;
                                    break;
                                }

                                argList.add(dummy);
                            }
                        }

                        if (skip) continue;

                        InvokeExpr callExpr;
                        if (targetMethod.isStatic()) {
                            callExpr = Jimple.v().newStaticInvokeExpr(targetMethod.makeRef(), argList);
                        } else if (targetMethod.isPrivate() || targetMethod.isConstructor()) {
                            callExpr = Jimple.v().newSpecialInvokeExpr(base, targetMethod.makeRef(), argList);
                        } else {
                            callExpr = Jimple.v().newVirtualInvokeExpr(base, targetMethod.makeRef(), argList);
                        }

                        Stmt callStmt = Jimple.v().newInvokeStmt(callExpr);
                        newStatements.add(callStmt);
                        insertedPairs.add(callKey);
                        System.out.println("Injecting: " + callStmt + " into: " + method.getSignature());
                    }

                    for (int i = newStatements.size() - 1; i >= 0; i--) {
                        units.insertAfter(newStatements.get(i), stmt);
                    }
                }
            }
        }
    }


    static Set<String> insertedPairs = new HashSet<>();
    public static void investigateCallGraphEdges() {
        Set<SootMethod> missingCallees = new HashSet<>();
        Map<String, Set<String>> methodCallMap = new HashMap<>();
        Map<String, SootMethod> methodLookup = new HashMap<>();

        for (Iterator<Edge> edgeIter = callGraph.iterator(); edgeIter.hasNext(); ) {
            Edge edge = edgeIter.next();
            SootMethod src = edge.src();
            SootMethod tgt = edge.tgt();

            String srcSig = src.getSignature();
            String tgtSig = tgt.getSignature();
            methodCallMap.computeIfAbsent(srcSig, k -> new HashSet<>()).add(tgtSig);
            methodLookup.putIfAbsent(srcSig, src);
            methodLookup.putIfAbsent(tgtSig, tgt);
        }


        for (Map.Entry<String, Set<String>> entry : methodCallMap.entrySet()) {
            if (entry.getKey().contains("dummyMainClass")) continue;

            SootMethod caller = methodLookup.get(entry.getKey());
            if (!caller.isConcrete()) continue;

            Body callerBody;
            try {
                callerBody = caller.retrieveActiveBody();
            } catch (Exception e) {
                continue;
            }

            Set<String> invokedMethods = new HashSet<>();
            for (Unit u : callerBody.getUnits()) {
                if (u instanceof Stmt stmt && stmt.containsInvokeExpr()) {
                    invokedMethods.add(stmt.getInvokeExpr().getMethod().getSignature());
                }
            }

            Chain<Unit> units = callerBody.getUnits();

            for (String calleeSig : entry.getValue()) {
                if (invokedMethods.contains(calleeSig)) continue;

                SootMethod callee = methodLookup.get(calleeSig);
                if(callee.getSignature().equals("<java.lang.Thread: void run()>") ||
                        caller.getSignature().equals("<java.lang.Thread: void run()>")){
                    //Because it gives duplication!
                    continue;
                }
                String callKey = caller.getSignature() + " --> " + callee.getSignature();

                // Early deduplication
                if (!insertedPairs.add(callKey)) continue;

                try {
                    if (callee.isStatic()) {
                        StaticInvokeExpr staticInvoke = Jimple.v().newStaticInvokeExpr(callee.makeRef());
                        units.add(Jimple.v().newInvokeStmt(staticInvoke));
                    } else {
                        Local baseLocal = Jimple.v().newLocal("tmp_" + callee.getDeclaringClass().getShortName(), callee.getDeclaringClass().getType());
                        if (!callerBody.getLocals().contains(baseLocal)) {
                            callerBody.getLocals().add(baseLocal);
                        }
                        NewExpr newExpr = Jimple.v().newNewExpr(callee.getDeclaringClass().getType());
                        AssignStmt assign = Jimple.v().newAssignStmt(baseLocal, newExpr);
                        VirtualInvokeExpr virtualInvoke = Jimple.v().newVirtualInvokeExpr(baseLocal, callee.makeRef());

                        units.add(assign);
                        units.add(Jimple.v().newInvokeStmt(virtualInvoke));
                    }

                    System.out.println("🔧 Inserted synthetic call for: " + calleeSig + " Into " + caller );
                    missingCallees.add(callee);

                } catch (Exception e) {
                    try {
                        Local dummy = Jimple.v().newLocal("tmp_dummy_" + callee.getDeclaringClass().getShortName(), callee.getDeclaringClass().getType());
                        if (!callerBody.getLocals().contains(dummy)) {
                            callerBody.getLocals().add(dummy);
                        }
                        VirtualInvokeExpr virtualInvoke = Jimple.v().newVirtualInvokeExpr(dummy, callee.makeRef());
                        units.add(Jimple.v().newInvokeStmt(virtualInvoke));

                        System.out.println("🛠️  Fallback virtual call for: " + calleeSig);
                        missingCallees.add(callee);
                    } catch (Exception ex) {
                        System.out.println("❌ Final failure inserting for: " + calleeSig + " Reason: " + ex.getMessage());
                    }
                }
            }
        }

        resolveVirtualCall(missingCallees);
    }


    public static void resolveVirtualCall(Set<SootMethod> targetMethods) {
        Set<String> alreadyInserted = new HashSet<>();

        for (SootMethod method : targetMethods) {
            if (!method.isConcrete()) continue;

            try {
                Body body = method.retrieveActiveBody();
                Chain<Unit> units = body.getUnits();
                List<Unit> toInsert = new ArrayList<>();
                List<Unit> insertBefore = new ArrayList<>();

                for (Unit u : units) {
                    if (u instanceof Stmt stmt && stmt.containsInvokeExpr()) {
                        InvokeExpr ie = stmt.getInvokeExpr();
                        if (ie instanceof VirtualInvokeExpr vInvoke) {

                            // Include unit in uniqueness check to avoid duplicates for same method call
                            String callSig = vInvoke.getMethod().getSignature() + "@" + method.getSignature() + "@" + u.toString();
                            if (!alreadyInserted.add(callSig)) continue;

                            Value base = vInvoke.getBase();
                            if (!(base instanceof Local baseLocal)) continue;

                            SootClass baseClass = ((RefType) baseLocal.getType()).getSootClass();
                            NewExpr newExpr = Jimple.v().newNewExpr(baseClass.getType());
                            AssignStmt assign = Jimple.v().newAssignStmt(baseLocal, newExpr);

                            toInsert.add(assign);
                            insertBefore.add(u);

                            System.out.println("✅ Resolved base for: " + vInvoke.getMethod().getSignature() + " in method: " + method.getSignature());
                        }
                    }
                }

                for (int i = 0; i < toInsert.size(); i++) {
                    units.insertBefore(toInsert.get(i), insertBefore.get(i));
                }

            } catch (Exception e) {
                System.out.println("⚠️ resolveVirtualCall error in method: " + method.getSignature() + " reason: " + e.getMessage());
            }
        }
    }

}
