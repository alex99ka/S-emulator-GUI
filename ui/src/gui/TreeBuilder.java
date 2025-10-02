package gui;

import javafx.scene.control.TreeItem;
import semulator.impl.api.skeleton.AbstractOpBasic;
import semulator.program.FunctionExecutor;

import java.util.HashMap;
import java.util.Map;

public class TreeBuilder {

    public static TreeItem<OpTreeNode> buildTree(FunctionExecutor program) {
        TreeItem<OpTreeNode> dummyRoot = new TreeItem<>(new OpTreeNode("Program", null));
        Map<AbstractOpBasic, TreeItem<OpTreeNode>> nodeMap = new HashMap<>();

        AbstractOpBasic op;
        while ((op = program.getNextOp()) != null) {
            TreeItem<OpTreeNode> leafItem = buildTreeFromLeaf(op, nodeMap);
            if (!dummyRoot.getChildren().contains(leafItem)) {
                dummyRoot.getChildren().add(leafItem);
            }
        }

        return dummyRoot;
    }

    // Recursive build from leaf, reusing existing TreeItems if already created
    private static TreeItem<OpTreeNode> buildTreeFromLeaf(AbstractOpBasic leaf, Map<AbstractOpBasic, TreeItem<OpTreeNode>> nodeMap) {
        if (nodeMap.containsKey(leaf)) {
            return nodeMap.get(leaf);
        }

        TreeItem<OpTreeNode> currentItem = new TreeItem<>(new OpTreeNode(getOpString(leaf), leaf.getType()));
        nodeMap.put(leaf, currentItem);

        AbstractOpBasic parent = leaf.getParent();
        if (parent != null) {
            TreeItem<OpTreeNode> parentItem = buildTreeFromLeaf(parent, nodeMap);
            if (!parentItem.getChildren().contains(currentItem)) {
                parentItem.getChildren().add(currentItem);
            }
            return parentItem; // return root of this subtree
        } else {
            return currentItem; // leaf is root itself
        }
    }

    private static String getOpString(AbstractOpBasic op) {
        String label = op.getLabel().getLabelRepresentation();
        return String.format("%s : %s (%d)",
                label.isEmpty() ? "   " : label,
                op.getRepresentation(),
                op.getCycles());
    }
}


class OpTreeNode {
    private final String displayText;
    private final String type;

    public OpTreeNode(String displayText, String type) {
        this.displayText = displayText;
        this.type = type;
    }

    public String getText() { return displayText; }
    public String getType() { return type; }

    @Override
    public String toString() { return displayText; }
}