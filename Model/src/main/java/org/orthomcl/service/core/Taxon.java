package org.orthomcl.service.core;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gusdb.fgputil.json.ToJson;
import org.json.JSONObject;
import org.orthomcl.service.core.layout.Renderable;

public class Taxon implements Comparable<Taxon>, Renderable, ToJson {

  private final int id;
  private String abbrev;
  private String name;
  private String commonName;
  private int sortIndex;
  private boolean species;
  private Taxon parent;
  private Taxon root;
  private String groupColor;
  private String color;
  private final Map<Integer, Taxon> children = new LinkedHashMap<>();

  public Taxon(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public String getAbbrev() {
    return abbrev;
  }

  public void setAbbrev(String abbrev) {
    this.abbrev = abbrev;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCommonName() {
    return commonName;
  }

  public void setCommonName(String commonName) {
    this.commonName = commonName;
  }

  public String getFullName() {
    String fullName = name;
    if (commonName != null && !commonName.equals(name)) {
      fullName += " (" + commonName + ")";
    }
    return fullName;
  }

  public int getSortIndex() {
    return sortIndex;
  }

  public void setSortIndex(int sortIndex) {
    this.sortIndex = sortIndex;
  }

  public boolean isSpecies() {
    return species;
  }

  public void setSpecies(boolean species) {
    this.species = species;
  }

  public Taxon getParent() {
    return parent;
  }

  public void setParent(Taxon parent) {
    this.parent = parent;
  }

  public Map<Integer, Taxon> getChildrenMap() {
    return children;
  }

  public Taxon[] getChildren() {
    Taxon[] array = new Taxon[children.size()];
    children.values().toArray(array);
    Arrays.sort(array);
    return array;
  }

  public void addChildren(Taxon child) {
    this.children.put(child.getId(), child);
  }

  public Taxon getRoot() {
    return root;
  }

  public void setRoot(Taxon root) {
    this.root = root;
  }

  @Override
  public String getColor() {
    return color;
  }

  @Override
  public void setColor(String color) {
    this.color = color;
  }

  /**
   * @return the groupColor
   */
  public String getGroupColor() {
    if (groupColor == null && parent != null)
      groupColor = parent.getGroupColor();
    return groupColor;
  }

  /**
   * @param groupColor
   *          the groupColor to set
   */
  public void setGroupColor(String groupColor) {
    this.groupColor = groupColor;
  }

  public String getPath() {
    String parentPath = (parent == null) ? "" : parent.getPath();
    if (parentPath.equals("ALL"))
      parentPath = "";
    if (!parentPath.equals(""))
      parentPath += "-&gt;";
    return parentPath + abbrev;
  }

  @Override
  public int compareTo(Taxon taxon) {
    // first compare the root
    int diff = 0;
    if (root != null && taxon.root == null) {
      diff = 1;
    }
    else if (root == null && taxon.root != null) {
      diff = -1;
    }
    else if (root != null && taxon.root != null) {
      diff = root.compareTo(taxon.root);
    }

    if (diff != 0) {
      return diff;
    }
    else {
      diff = this.sortIndex - taxon.sortIndex;
      return (diff != 0) ? diff : this.abbrev.compareTo(taxon.abbrev);
    }
  }

  @Override
  public JSONObject toJson() {
    return new JSONObject()
      .put("id", id)
      .put("abbrev", abbrev)
      .put("name", name)
      .put("commonName", commonName)
      .put("sortIndex", sortIndex)
      .put("species", species)
      .put("groupColor", groupColor)
      .put("color", color)
      .put("children", ToJson.mapToJson(children));
  }

}
