package semulator.execution;
import semulator.impl.api.skeleton.AbstractOpBasic;
import semulator.label.Label;
import semulator.variable.VariableImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ExecutionContext {

    Long getVariableValue(VariableImpl v);
    Map<Label, AbstractOpBasic> getLabelMap();
    void addSnap(ArrayList<VariableImpl> vars, ArrayList<Long> vals);
    Map<VariableImpl, Long> getCurrSnap();
    List<Map<VariableImpl, Long>> getSnapshots();
    void reset();
}