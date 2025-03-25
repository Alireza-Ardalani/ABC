

package qilin.core.builder;

import qilin.CoreConfig;
import qilin.core.ArtificialMethod;
import qilin.core.PTAScene;
import qilin.util.PTAUtils;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.NullConstant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FakeMainFactory extends ArtificialMethod {
    public static int implicitCallEdges;
    private final SootClass fakeClass;

    public FakeMainFactory() {
        this.localStart = 0;
        this.fakeClass = new SootClass("FakeMain");
        this.fakeClass.setResolvingLevel(SootClass.BODIES);
        this.method = new SootMethod("fakeMain", null, VoidType.v());
        this.method.setModifiers(Modifier.STATIC);
        SootField currentThread = new SootField("currentThread", RefType.v("java.lang.Thread"), Modifier.STATIC);
        SootField globalThrow = new SootField("globalThrow", RefType.v("java.lang.Exception"), Modifier.STATIC);
        fakeClass.addMethod(this.method);
        fakeClass.addField(currentThread);
        fakeClass.addField(globalThrow);
    }

    private List<SootMethod> getEntryPoints() {
        List<SootMethod> ret = new ArrayList<>();
        if (CoreConfig.v().getPtaConfig().clinitMode == CoreConfig.ClinitMode.FULL) {
            ret.addAll(EntryPoints.v().clinits());
        } else {
            // on the fly mode, resolve the clinit methods on the fly.
            ret.addAll(Collections.emptySet());
        }

        if (CoreConfig.v().getPtaConfig().singleentry) {
            //List<SootMethod> entries = EntryPoints.v().application();
            List<SootMethod> entries = new ArrayList<>();

            //Alireza-Added This
            findEntryPointsFromFlowDroid();
            entries.addAll(findEntryPointsFromFlowDroid());
            //Alireza-Added This


            if (entries.isEmpty()) {
                throw new RuntimeException("Must specify MAINCLASS when appmode enabled!!!");
            } else {
                ret.addAll(entries);
            }
        } else {
            System.out.println("include implicit entry!");
            ret.addAll(EntryPoints.v().application());
            ret.addAll(EntryPoints.v().implicit());
        }
        System.out.println("#EntrySize:" + ret.size());
        return ret;
    }

    private static List<SootMethod> findEntryPointsFromFlowDroid(){
        List<SootMethod> entryPoints = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        try {
            lines = Files.readAllLines(Paths.get("EntryMethods.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(var clazz: PTAScene.v().getClasses()){
            for (SootMethod method : clazz.getMethods()) {
                if (lines.contains(method.getSignature())) {
                    entryPoints.add(method);
                }
            }
        }
        return entryPoints;
    }

    public SootMethod getFakeMain() {
        if (body == null) {
            synchronized (this) {
                if (body == null) {
                    this.method.setSource((m, phaseName) -> new JimpleBody(this.method));
                    this.body = PTAUtils.getMethodBody(method);
                    makeFakeMain();
                }
            }
        }
        return this.method;
    }

    public Value getFieldCurrentThread() {

        return getStaticFieldRef("FakeMain", "currentThread");
    }

    public Value getFieldGlobalThrow() {

        return getStaticFieldRef("FakeMain", "globalThrow");
    }

    private void makeFakeMain() {
        implicitCallEdges = 0;
        int entryNumber = 0;
        for (SootMethod entry : getEntryPoints()) {
            if (!entry.isStatic()) {
                if (!entry.getDeclaringClass().isAbstract()) {
                    entryNumber++;
                    if(entry.isConstructor()){
                        Local thisLocal = Jimple.v().newLocal("$newConstructor" + entryNumber, RefType.v(entry.getDeclaringClass().getName()));
                        body.getLocals().add(thisLocal);
                        Value thisInstance = Jimple.v().newNewExpr(RefType.v(entry.getDeclaringClass().getName()));
                        body.getUnits().add(Jimple.v().newAssignStmt(thisLocal, thisInstance));
                        if(entry.getParameterCount() == 0){
                            body.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(thisLocal, entry.makeRef())));
                        }
                        else {
                            List<Value> args = new ArrayList<>();
                            for (int i = 0; i < entry.getParameterCount(); i++) {
                                args.add(NullConstant.v());
                            }
                            body.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(thisLocal, entry.makeRef(), args)));
                        }

                    }
                    else {
                            if (entry.getReturnType() instanceof VoidType){
                                Local targetLocal = body.getLocals().stream()
                                        .filter(local -> local.getType().toString().equals(entry.getDeclaringClass().getName()))
                                        .findFirst()
                                        .orElse(null);
                                if(targetLocal==null){
                                    targetLocal = Jimple.v().newLocal("$newTargetLocal" + entryNumber, RefType.v(entry.getDeclaringClass().getName()));
                                    body.getLocals().add(targetLocal);
                                }
                                if(entry.getParameterCount() == 0){
                                    body.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(targetLocal, entry.makeRef())));
                                }
                                else {
                                    List<Value> args = new ArrayList<>();
                                    for (int i = 0; i < entry.getParameterCount(); i++) {
                                        args.add(NullConstant.v());
                                    }
                                    body.getUnits().add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(targetLocal, entry.makeRef(), args)));
                                }
                            } else {
                                Local targetLocal = body.getLocals().stream()
                                        .filter(local -> local.getType().toString().equals(entry.getDeclaringClass().getName()))
                                        .findFirst()
                                        .orElse(null);
                                if(targetLocal == null){
                                    targetLocal = Jimple.v().newLocal("$newTargetLocal" + entryNumber, RefType.v(entry.getDeclaringClass().getName()));
                                    body.getLocals().add(targetLocal);
                                }
                                Type returnType = entry.getReturnType();
                                Local newLocal = Jimple.v().newLocal("$Base"+entryNumber, returnType);
                                body.getLocals().add(newLocal);

                                if(entry.getParameterCount() == 0){
                                    Value invokeExpr = Jimple.v().newVirtualInvokeExpr(targetLocal, entry.makeRef());
                                    AssignStmt assignStmt = Jimple.v().newAssignStmt(newLocal, invokeExpr);
                                    body.getUnits().add(assignStmt);
                                }
                                else {
                                    List<Value> args = new ArrayList<>();
                                    for (int i = 0; i < entry.getParameterCount(); i++) {
                                        args.add(NullConstant.v());
                                    }
                                    Value invokeExpr = Jimple.v().newVirtualInvokeExpr(targetLocal, entry.makeRef(),args);
                                    AssignStmt assignStmt = Jimple.v().newAssignStmt(newLocal, invokeExpr);
                                    body.getUnits().add(assignStmt);
                                }

                            }

                    }
                }
            }
        }
        body.getUnits();
        System.out.println();
        if (CoreConfig.v().getPtaConfig().singleentry) {
            return;
        }
        Value sv = getNextLocal(RefType.v("java.lang.String"));
        Value mainThread = getNew(RefType.v("java.lang.Thread"));
        Value mainThreadGroup = getNew(RefType.v("java.lang.ThreadGroup"));
        Value systemThreadGroup = getNew(RefType.v("java.lang.ThreadGroup"));

        Value gCurrentThread = getFieldCurrentThread();
        addAssign(gCurrentThread, mainThread); // Store
        Value vRunnable = getNextLocal(RefType.v("java.lang.Runnable"));

        Value lThreadGroup = getNextLocal(RefType.v("java.lang.ThreadGroup"));
        addInvoke(mainThread, "<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.String)>", mainThreadGroup, sv);
        Value tmpThread = getNew(RefType.v("java.lang.Thread"));
        addInvoke(tmpThread, "<java.lang.Thread: void <init>(java.lang.ThreadGroup,java.lang.Runnable)>", lThreadGroup, vRunnable);
        addInvoke(tmpThread, "<java.lang.Thread: void exit()>");

        addInvoke(systemThreadGroup, "<java.lang.ThreadGroup: void <init>()>");
        addInvoke(mainThreadGroup, "<java.lang.ThreadGroup: void <init>(java.lang.ThreadGroup,java.lang.String)>", systemThreadGroup, sv);

        Value lThread = getNextLocal(RefType.v("java.lang.Thread"));
        Value lThrowable = getNextLocal(RefType.v("java.lang.Throwable"));
        Value tmpThreadGroup = getNew(RefType.v("java.lang.ThreadGroup"));
        addInvoke(tmpThreadGroup, "<java.lang.ThreadGroup: void uncaughtException(java.lang.Thread,java.lang.Throwable)>", lThread, lThrowable); // TODO.


        // ClassLoader
        Value defaultClassLoader = getNew(RefType.v("sun.misc.Launcher$AppClassLoader"));
        addInvoke(defaultClassLoader, "<java.lang.ClassLoader: void <init>()>");
        Value vClass = getNextLocal(RefType.v("java.lang.Class"));
        Value vDomain = getNextLocal(RefType.v("java.security.ProtectionDomain"));
        addInvoke(defaultClassLoader, "<java.lang.ClassLoader: java.lang.Class loadClassInternal(java.lang.String)>", sv);
        addInvoke(defaultClassLoader, "<java.lang.ClassLoader: void checkPackageAccess(java.lang.Class,java.security.ProtectionDomain)>", vClass, vDomain);
        addInvoke(defaultClassLoader, "<java.lang.ClassLoader: void addClass(java.lang.Class)>", vClass);

        // PrivilegedActionException
        Value privilegedActionException = getNew(RefType.v("java.security.PrivilegedActionException"));
        Value gLthrow = getNextLocal(RefType.v("java.lang.Exception"));
        addInvoke(privilegedActionException, "<java.security.PrivilegedActionException: void <init>(java.lang.Exception)>", gLthrow);


    }
}
