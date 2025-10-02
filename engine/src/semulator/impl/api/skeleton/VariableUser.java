package semulator.impl.api.skeleton;

import semulator.variable.VariableImpl;

public interface VariableUser {
    public void setSecondaryVariable(VariableImpl variable);
    public VariableImpl getSecondaryVariable();
}
