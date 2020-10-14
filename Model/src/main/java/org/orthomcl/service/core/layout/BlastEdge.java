package org.orthomcl.service.core.layout;

import org.gusdb.fgputil.json.ToJson;
import org.json.JSONObject;
import org.orthomcl.service.core.GenePair;
import org.orthomcl.shared.model.layout.Edge;
import org.orthomcl.shared.model.layout.Layout;

public class BlastEdge extends GenePair implements Comparable<BlastEdge>, Edge, ToJson {

  public static final int MIN_EVALUE = -180;
  public static final int MAX_EVALUE = -5;

  private String evalueA;
  private String evalueB;
  private double score;
  private EdgeType type;
  private GeneNode nodeA;
  private GeneNode nodeB;
  private String color;

  public BlastEdge(String sourceIdA, String sourceIdB) {
    super(sourceIdA, sourceIdB);
  }

  public String getEvalue() {
    String evalue = evalueA;
    if (evalueB != null && evalueA.equals(evalueB))
      evalue += " / " + evalueB;
    return evalue;
  }

  /**
   * @return the evalueA
   */
  public String getEvalueA() {
    return evalueA;
  }

  /**
   * @param evalueA
   *          the evalueA to set
   */
  public void setEvalueA(String evalueA) {
    this.evalueA = evalueA;
  }

  /**
   * @return the evalueB
   */
  public String getEvalueB() {
    return evalueB;
  }

  /**
   * @param evalueB
   *          the evalueB to set
   */
  public void setEvalueB(String evalueB) {
    this.evalueB = evalueB;
  }

  /**
   * @return the type
   */
  public EdgeType getType() {
    return type;
  }

  /**
   * @param type
   *          the type to set
   */
  public void setType(EdgeType type) {
    this.type = type;
  }

  /**
   * @return the nodeA
   */
  @Override
  public GeneNode getNodeA() {
    return nodeA;
  }

  /**
   * @param nodeA
   *          the nodeA to set
   */
  public void setNodeA(GeneNode nodeA) {
    this.nodeA = nodeA;
  }

  /**
   * @return the nodeB
   */
  @Override
  public GeneNode getNodeB() {
    return nodeB;
  }

  /**
   * @param nodeB
   *          the nodeB to set
   */
  public void setNodeB(GeneNode nodeB) {
    this.nodeB = nodeB;
  }

  /**
   * @return the score
   */
  public double getScore() {
    return score;
  }

  public String getScoreFormatted() {
    return LayoutManager.FORMAT.format(score);
  }

  /**
   * @param score
   *          the score to set
   */
  public void setScore(double score) {
    this.score = score;
  }

  /**
   * @return the color
   */
  public String getColor() {
    if (color == null) {
      int value = (int) Math.abs(Math.round((score - MIN_EVALUE) * 256 / (MAX_EVALUE - MIN_EVALUE + 1)));
      if (value > 255)
        value = 255;
      else if (value < 0)
        value = 0;
      color = "#" + toHex(255 - value) + "00" + toHex(value);
    }
    return color;
  }

  private String toHex(int value) {
    String hex = Integer.toHexString(value);
    if (hex.length() == 1)
      hex = "0" + hex;
    return hex;
  }

  /**
   * Sort edge by score
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(BlastEdge edge) {
    double diff = edge.score - score;
    if (diff < 0)
      return -1;
    else if (diff > 0)
      return 1;
    else
      return 0;
  }

  @Override
  public double getPreferredLength() {
    return Layout.MAX_PREFERRED_LENGTH +score;
  }
  
  @Override
  public JSONObject toJson() {
    return super.toJson()
      .put("score", score)
      .put("color", color)
      .put("Q", nodeA.getIndex())
      .put("S", nodeB.getIndex())
      .put("E", getEvalue())
      .put("T", type.getCode());
  }

}
