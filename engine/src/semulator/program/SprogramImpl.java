package semulator.program;
import semulator.impl.api.skeleton.AbstractOpBasic;
import semulator.impl.api.synthetic.OpFunctionBase;

import java.util.*;

public class SprogramImpl extends FunctionExecutorImpl {

    List <FunctionExecutor> functions;

    public SprogramImpl(String name) {
        super(name);

    }

    public void addFunction(FunctionExecutor func) {
        if (functions == null)
            functions = new ArrayList<>();
        functions.add(func);
    };

    public void setFunctions(List <FunctionExecutor> functions)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    {
        this.functions = functions;
    }

    public List <FunctionExecutor> getFunctions(){ return  functions;}

    @Override
    public int calculateCycles() {

        if (functions != null) {
            functions.forEach(func->{
                cycles += func.calculateCycles();
            });
        }
        return cycles;
    }

    public void reset() {
        this.cycles = 0;
        this.opListIndex = 0;
        context.reset();
        if (functions != null) {
            functions.forEach(FunctionExecutor::reset);
        }
    }

    @Override
    public void opListIndexReset()
    {
        this.opListIndex = 0;
        if (functions != null) {
            functions.forEach(FunctionExecutor::opListIndexReset);
        }
    }

    @Override
    public FunctionExecutor getFunction(String functionName) {
        if (functions == null)
            return null;
        else for (FunctionExecutor func : functions) {
            if (func.getName().equals(functionName))
                return func;
        }
        return null;
    }

    // deep clone
    @Override
    public FunctionExecutor myClone() {
        SprogramImpl newProgram = new SprogramImpl(this.name);
        for (AbstractOpBasic op : this.opList) {
            newProgram.addOp( op.myClone()); // Assuming AbstractOpBasic is immutable or properly cloned
        }
        newProgram.setInputVars(new ArrayList<>(this.inputVars));
        newProgram.setAllVars(new HashSet<>(this.variables));
        newProgram.setContext(context);
        newProgram.addLabelSet(new LinkedHashSet<>(this.labelsHashSet));
        newProgram.origVariables = new HashSet<>(this.origVariables);

        if (functions != null) {
            functions.forEach(func->{
                FunctionExecutor newFunc = func.myClone();
                newProgram.addFunction(newFunc);
            });
        }
        return newProgram;
    }

    public void calculateQuoteDegree() {
        opList.forEach(op -> {
            if (op instanceof OpFunctionBase opFunctionBase) {
                opFunctionBase.setDegree();
            }
        });
    }

    @Override
    public void init() {
        super.init();

        if (functions == null)
            return;
        functions.forEach(FunctionExecutor::init);
    }

    @Override
    public void restoreOriginalVars() {
        super.restoreOriginalVars();

        if (functions == null)
            return;
        functions.forEach(FunctionExecutor::restoreOriginalVars);
    }

    @Override
    public void resetSnap() {
        super.resetSnap();

        if (functions == null)
            return;
        functions.forEach(FunctionExecutor::resetSnap);
    }

}

