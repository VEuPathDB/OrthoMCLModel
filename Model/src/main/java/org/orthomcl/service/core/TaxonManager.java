package org.orthomcl.service.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableValue;
import org.gusdb.wdk.model.record.attribute.AttributeValue;
import org.orthomcl.service.core.layout.RenderingHelper;

public class TaxonManager {

  private static final String TABLE_TAXONS = "Taxons";
  private static final String TABLE_ROOTS = "RootTaxons";

  // lazy-loaded cache of taxon data
  private static Map<String, Taxon> TAXONS;

  public static Map<String, Taxon> getTaxons(WdkModel wdkModel) throws WdkModelException {
    if (TAXONS == null) {
      TAXONS = loadTaxons(wdkModel);
    }
    return TAXONS;
  }

  private static synchronized Map<String, Taxon> loadTaxons(WdkModel wdkModel) throws WdkModelException {
    try {
      RecordInstance record = HelperRecord.get(wdkModel);
  
      Map<String, Taxon> newTaxons = new LinkedHashMap<>();
      Map<Integer, Integer> parents = new HashMap<>();
      Map<Integer, String> abbreviations = new HashMap<>();
      Map<String, TableValue> tables = record.getTableValueMap();
  
      TableValue taxonTable = tables.get(TABLE_TAXONS);
      for (Map<String, AttributeValue> row : taxonTable) {
        Taxon taxon = new Taxon(Integer.valueOf(row.get("taxon_id").getValue()));
        taxon.setAbbrev(row.get("abbreviation").getValue());
        taxon.setSpecies(row.get("is_species").getValue().toString().equals("1"));
        taxon.setName(row.get("name").getValue());
        taxon.setCommonName(row.get("name").getValue());
        taxon.setSortIndex(Integer.valueOf(row.get("sort_index").getValue()));
        newTaxons.put(taxon.getAbbrev(), taxon);
        abbreviations.put(taxon.getId(), taxon.getAbbrev());
  
        int parentId = Integer.valueOf(row.get("parent_id").getValue());
        parents.put(taxon.getId(), parentId);
      }
  
      // resolve parent/children
      for (Taxon taxon : newTaxons.values()) {
        int parentId = parents.get(taxon.getId());
        if (taxon.getId() != parentId || abbreviations.containsKey(parentId)) {
          Taxon parent = newTaxons.get(abbreviations.get(parentId));
          if (taxon != parent) {
            taxon.setParent(parent);
            parent.addChildren(taxon);
          }
        }
      }
  
      // assign root to each taxon
      TableValue rootTable = tables.get(TABLE_ROOTS);
      assignRoots(newTaxons, rootTable);
  
      // assign colors
      assignColors(newTaxons);
  
      return newTaxons;
    }
    catch (WdkUserException e) {
      throw new WdkModelException("Could not load taxons", e);
    }
  }

  private static void assignRoots(Map<String, Taxon> taxons, TableValue rootTable) throws WdkModelException,
      WdkUserException {
    Map<String, String> roots = new HashMap<>();
    for (Map<String, AttributeValue> row : rootTable) {
      String abbrev = row.get("taxon_abbrev").getValue();
      String groupColor = row.get("color").getValue();
      roots.put(abbrev, groupColor);
      taxons.get(abbrev).setGroupColor(groupColor);
    }

    for (Taxon taxon : taxons.values()) {
      if (!taxon.isSpecies())
        continue;
      Taxon parent = taxon.getParent();
      while (parent != null) {
        if (roots.containsKey(parent.getAbbrev())) {
          taxon.setRoot(parent);
          break;
        }
        parent = parent.getParent();
      }
    }
  }

  private static void assignColors(Map<String, Taxon> taxons) {
    // only assign colors to species, and group species by roots
    Map<String, List<Taxon>> species = new HashMap<>();
    for (Taxon taxon : taxons.values()) {
      if (taxon.isSpecies()) {
        String rootId = taxon.getRoot().getAbbrev();
        List<Taxon> list = species.get(rootId);
        if (list == null) {
          list = new ArrayList<>();
          species.put(rootId, list);
        }
        list.add(taxon);
      }
    }
    for (List<Taxon> list : species.values()) {
      RenderingHelper.assignRandomColors(list);
    }
  }
}
