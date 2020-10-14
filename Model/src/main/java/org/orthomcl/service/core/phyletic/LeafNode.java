package org.orthomcl.service.core.phyletic;

import java.util.List;

public class LeafNode implements ExpressionNode {

    private List<String> terms;
    private String condition;
    private boolean onSpecies;
    private int count;

    public List<String> getTerms() {
        return this.terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }

    public String getCondition() {
        return this.condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public boolean isOnSpecies() {
        return this.onSpecies;
    }

    public void setOnSpecies(boolean onSpecies) {
        this.onSpecies = onSpecies;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("{Leaf: terms: {");
        for (int i = 0; i < terms.size(); i++) {
            if (i > 0) builder.append(",");
            builder.append(terms.get(i));
        }
        builder.append("}, "+condition + ", " + count + ", ");
        builder.append((onSpecies ? "T" : "-") + "}");
        return builder.toString();
    }
}
