package semulator.impl.api.synthetic;

import semulator.impl.api.skeleton.AbstractOpBasic;
import semulator.impl.api.skeleton.OpData;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.program.FunctionExecutorImpl;
import semulator.program.FunctionExecutor;
import semulator.variable.VariableImpl;

import java.util.ArrayList;
import java.util.List;

public class OPQuote extends OpFunctionBase {

    public OPQuote(VariableImpl variable, String functionName, String functionArguments, FunctionExecutor function) {
        this(variable, FixedLabel.EMPTY, functionName, functionArguments, function,null);
    }

    public OPQuote( VariableImpl variable, String functionName, String functionArguments, FunctionExecutor function, AbstractOpBasic parent) {
        this(variable, FixedLabel.EMPTY, functionName, functionArguments, function, parent);
    }

    public OPQuote(VariableImpl variable, Label label,  String functionName, String functionArguments, FunctionExecutor function) {
        this(variable, label, functionName, functionArguments, function, null);
    }

    public OPQuote(VariableImpl variable, Label label, String functionName, String functionArguments, FunctionExecutor function, AbstractOpBasic parent) {
        super(OpData.QUOTE, variable, label, functionName, functionArguments, function, parent);
        generateUniqId();
    }

    public OPQuote(OPQuote src)
    {
        super(OpData.QUOTE, src.getVariable(), src.getLabel(), src.getFunctionName(), src.getStrFunctionArguments(), src.getFunction().myClone(), null);
        generateUniqId();
    }

    @Override
    public Label execute(FunctionExecutor program) { // not supported for this OP
        return FixedLabel.EMPTY;
    }

    @Override
    public Label execute(FunctionExecutor program, List <FunctionExecutor> functions) {

        ((FunctionExecutorImpl)function).run(program, functionArguments, functions);
        Long result = function.getVariableValue(VariableImpl.RESULT);
        ArrayList<VariableImpl> vars = new ArrayList<>();
        ArrayList<Long> vals = new ArrayList<>();
        vars.add(getVariable());
        vals.add(result);

        program.addSnap(vars,vals);
        program.increaseCycleCounter(getCycles());

        return FixedLabel.EMPTY;
    }

    @Override
    protected AbstractOpBasic getFinalOp(VariableImpl resultVar, AbstractOpBasic parent) {
        return  new OpAssignment(getVariable(), resultVar, parent);
    }

    @Override
    public List<AbstractOpBasic> expand(int ignoredExtensionLevel, FunctionExecutor ignoredProgram, VariableImpl Papa) {
        return expand(ignoredExtensionLevel, ignoredProgram);
    }

    @Override
    public AbstractOpBasic myClone() {
        return new OPQuote(this);
    }

    @Override
    public String getRepresentation() {
        return String.format("%s ← %s", getVariable().getRepresentation(), ((FunctionExecutorImpl)function).getUserString());
    }
}
