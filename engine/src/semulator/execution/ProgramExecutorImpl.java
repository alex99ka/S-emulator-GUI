package semulator.execution;

import semulator.impl.api.skeleton.AbstractOpBasic;
import semulator.label.FixedLabel;
import semulator.label.Label;
import semulator.program.FunctionExecutor;

import java.util.*;
import javafx.util.Pair;
import semulator.variable.VariableImpl;


public class ProgramExecutorImpl {

    public ProgramExecutorImpl() { }

    public static List<Pair<Integer, TreeMap<VariableImpl, Long>>> run(FunctionExecutor program, List<Long> inputs, List <FunctionExecutor> functions) {
        return program.run(inputs, functions);
    }
}
