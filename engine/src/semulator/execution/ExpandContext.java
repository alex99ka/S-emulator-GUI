package semulator.execution;

import semulator.impl.api.skeleton.AbstractOpBasic;

import semulator.label.Label;
import semulator.variable.VariableImpl;

public interface ExpandContext {
    Label newUniqueLabel();

    /** מחזיר משתנה עבודה חדש בפורמט z<number> (ייחודי), מאופס ומוסף ל-currSnap ולסט המשתנים. */
    VariableImpl newWorkVar();

    void addOpWithNewLabel(AbstractOpBasic op);


}

