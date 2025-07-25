package qilin.parm.select;

import qilin.core.pag.AllocNode;
import qilin.core.pag.FieldValNode;
import qilin.core.pag.LocalVarNode;
import soot.Context;
import soot.SootMethod;

public class UniformSelectorComponent extends CtxSelector {
    private final int k;
    private final int hk;

    public UniformSelectorComponent(int k, int hk) {
        this.k = k;
        this.hk = hk;
    }

    @Override
    public Context select(SootMethod m, Context context) {
        return contextTailor_Component(context, k);
    }

    @Override
    public Context select(LocalVarNode lvn, Context context) {
        return contextTailor_Component(context, k);
    }

    @Override
    public Context select(FieldValNode fvn, Context context) {
        return contextTailor_Component(context, k);
    }

    @Override
    public Context select(AllocNode heap, Context context) {
        return contextTailor_Component(context, hk);
    }
}
