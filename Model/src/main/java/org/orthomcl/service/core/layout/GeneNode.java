package org.orthomcl.service.core.layout;

import org.gusdb.fgputil.json.ToJson;
import org.json.JSONObject;
import org.orthomcl.service.core.Gene;
import org.orthomcl.shared.model.layout.Node;
import org.orthomcl.shared.model.layout.Vector;

public class GeneNode implements Node, ToJson {

  private final Gene gene;
  
  private int index;
  private Vector point = new Vector();
  
  public GeneNode(Gene gene) {
    this.gene = gene;
  }
  
  public Gene getGene() {
    return gene;
  }
  
  /**
   * @return the index
   */
  public int getIndex() {
    return index;
  }

  /**
   * @param index the index to set
   */
  public void setIndex(int index) {
    this.index = index;
  }

  public void setLocation(double x, double y) {
    this.point.setLocation(x, y);
  }
  
  public double getX() {
    return point.x;
  }
  
  public String getXFormatted() {
    return LayoutManager.FORMAT.format(getX());
  }
  
  public double getY() {
    return point.y;
  }
  
  public String getYFormatted() {
    return LayoutManager.FORMAT.format(getY());
  }

  @Override
  public Vector getPoint() {
    return point;
  }

  @Override
  public JSONObject toJson() {
    JSONObject json = new JSONObject();
    json.put("id", gene.getSourceId());
    json.put("x", LayoutManager.FORMAT.format(point.x));
    json.put("y", LayoutManager.FORMAT.format(point.y));
    json.put("i", index);
    return json;
  }

}
