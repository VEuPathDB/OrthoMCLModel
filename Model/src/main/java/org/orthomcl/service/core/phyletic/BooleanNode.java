package org.orthomcl.service.core.phyletic;

public class BooleanNode implements ExpressionNode {

    private ExpressionNode left;
    private ExpressionNode right;
    private String operator;

    public BooleanNode(ExpressionNode left, ExpressionNode right,
            String operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    /**
     * @return the left
     */
    public ExpressionNode getLeft() {
        return this.left;
    }

    /**
     * @param left
     *            the left to set
     */
    public void setLeft(ExpressionNode left) {
        this.left = left;
    }

    /**
     * @return the right
     */
    public ExpressionNode getRight() {
        return this.right;
    }

    /**
     * @param right
     *            the right to set
     */
    public void setRight(ExpressionNode right) {
        this.right = right;
    }

    /**
     * @return the operator
     */
    public String getOperator() {
        return this.operator;
    }

    /**
     * @param operator
     *            the operator to set
     */
    public void setOperator(String operator) {
        this.operator = operator;
    }

    @Override
    public String toString() {
        return "{Boolean: " + operator + ", left: " + left + ", right: "
                + right + "}";
    }
}
