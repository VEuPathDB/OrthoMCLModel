package org.orthomcl.service.core.layout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gusdb.fgputil.json.ToJson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.orthomcl.service.core.Gene;
import org.orthomcl.service.core.GenePair;
import org.orthomcl.service.core.Group;
import org.orthomcl.service.core.Taxon;
import org.orthomcl.shared.model.layout.Graph;
import org.orthomcl.shared.model.layout.Layout;

public class GroupLayout implements Graph {

  private final Group group;
  private final Map<String, Taxon> taxons;
  private final Map<Integer, GeneNode> nodes;
  private final Map<GenePair, BlastEdge> edges;
  private final Map<String, Integer> taxonCounts;
  private final int size;

  private int minEvalueExp;
  private int maxEvalueExp;

  public GroupLayout(Group group, int size) {
    this.group = group;
    this.taxons = new LinkedHashMap<>();
    this.edges = new LinkedHashMap<>();
    this.nodes = new LinkedHashMap<>();
    this.taxonCounts = new HashMap<>();
    this.size = size;
    this.minEvalueExp = Integer.MAX_VALUE;
    this.maxEvalueExp = Integer.MIN_VALUE;
  }

  public int getMaxSize() {
    return GeneSetLayoutGenerator.MAX_GENES;
  }
  
  public int getSize() {
    return size;
  }

  public Group getGroup() {
    return group;
  }

  public void addTaxon(Taxon taxon) {
    this.taxons.put(taxon.getAbbrev(), taxon);
  }

  public Collection<Taxon> getTaxons() {
    return taxons.values();
  }

  public BlastEdge getEdge(GenePair genePair) {
    return edges.get(genePair);
  }

  @Override
  public Collection<BlastEdge> getEdges() {
    // sort edges
    List<BlastEdge> list = new ArrayList<>(edges.values());
    Collections.sort(list);
    return list;
  }

  public Map<String, List<BlastEdge>> getEdgesByType() {
    Map<String, List<BlastEdge>> edgeMap = new HashMap<>();
    for (BlastEdge edge : getEdges()) {
      List<BlastEdge> list = edgeMap.get(edge.getType().name());
      if (list == null) {
        list = new ArrayList<>();
        edgeMap.put(edge.getType().name(), list);
      }
      list.add(edge);
    }
    return edgeMap;
  }

  public void addEdge(BlastEdge edge) {
    this.edges.put(edge, edge);
    // also calculate the evalue range
    String evalueA = edge.getEvalueA(), evalueB = edge.getEvalueB();
    int expA = Integer.valueOf(evalueA.split("[eE]")[1]);
    if (evalueB == null || evalueA.equals(evalueB)) {
      if (minEvalueExp > expA)
        minEvalueExp = expA;
      if (maxEvalueExp < expA)
        maxEvalueExp = expA;
    }
    else {
      int expB = Integer.valueOf(evalueB.split("[eE]")[1]);
      int minExp = Math.min(expA, expB);
      if (minEvalueExp > minExp)
        minEvalueExp = minExp;
      int maxExp = Math.max(expA, expB);
      if (maxEvalueExp < maxExp)
        maxEvalueExp = maxExp;
    }
  }

  @Override
  public Collection<GeneNode> getNodes() {
    // sort the nodes, so that the client can bind it by array;
    Integer[] keys = nodes.keySet().toArray(new Integer[0]);
    Arrays.sort(keys);
    List<GeneNode> list = new ArrayList<>(nodes.size());
    for (int key : keys) {
      list.add(nodes.get(key));
    }
    return list;
  }

  public Map<String, List<GeneNode>> getNodesByTaxon() {
    Map<String, List<GeneNode>> nodeMap = new HashMap<>();
    for (GeneNode node : nodes.values()) {
      Gene gene = node.getGene();
      Taxon taxon = gene.getTaxon();
      String abbrev = taxon.getAbbrev();
      List<GeneNode> list = nodeMap.get(abbrev);
      if (list == null) {
        list = new ArrayList<>();
        nodeMap.put(node.getGene().getTaxon().getAbbrev(), list);
      }
      list.add(node);
    }
    return nodeMap;
  }

  public GeneNode getNode(int index) {
    return nodes.get(index);
  }

  public void addNode(GeneNode node) {
    this.nodes.put(node.getIndex(), node);
    // also calculate the gene counts by taxons
    String abbrev = node.getGene().getTaxon().getAbbrev();
    if (taxonCounts.containsKey(abbrev)) {
      int count = taxonCounts.get(abbrev);
      taxonCounts.put(abbrev, count + 1);
    }
    else {
      taxonCounts.put(abbrev, 1);
    }
  }

  public Map<Taxon, Integer> getTaxonCounts() {
    // get the taxons with genes
    List<Taxon> list = new ArrayList<>(taxonCounts.size());
    for (String abbrev : taxonCounts.keySet()) {
      list.add(taxons.get(abbrev));
    }
    Collections.sort(list);

    // prepare the map, with the order of taxons preserved
    Map<Taxon, Integer> map = new LinkedHashMap<>(taxonCounts.size());
    for (Taxon taxon : list) {
      map.put(taxon, taxonCounts.get(taxon.getAbbrev()));
    }
    return map;
  }

  public int getMinEvalueExp() {
    return minEvalueExp;
  }

  public int getMaxEvalueExp() {
    return maxEvalueExp;
  }

  @Override
  public double getMaxPreferredLength() {
    return Layout.MAX_PREFERRED_LENGTH;
  }

  @Override
  public String toString() {
    JSONObject jsLayout = new JSONObject();

    try {
      // output genes
      JSONArray jsNodes = new JSONArray();
      for (GeneNode node : nodes.values()) {
        jsNodes.put(node.toJson());
      }
      jsLayout.put("N", jsNodes);

      // output scores
      JSONArray jsEdges = new JSONArray();
      for (BlastEdge edge : edges.values()) {
        jsEdges.put(edge.toJson());
      }
      jsLayout.put("E", jsEdges);
      return jsLayout.toString();
    }
    catch (JSONException ex) {
      throw new RuntimeException(ex);
    }
  }

  public JSONObject toJson() {
    return new JSONObject()
      .put("group", group.toJson())
      .put("size", size)
      .put("minEvalueExp", minEvalueExp)
      .put("maxEvalueExp", maxEvalueExp)
      .put("taxonCounts", new JSONObject(taxonCounts))
      .put("taxons", ToJson.mapToJson(taxons))
      .put("nodes", ToJson.mapToJson(nodes))
      .put("edges", ToJson.mapToJson(edges));
  }

}
