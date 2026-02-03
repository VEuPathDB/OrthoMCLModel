package org.orthomcl.service.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.TableValue;
import org.gusdb.wdk.model.record.attribute.AttributeValue;

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
      // get helper record tables
      Map<String, TableValue> tables = HelperRecord.get(wdkModel).getTableValueMap();

      // map to hold return value (abbrev -> Taxon)
      Map<String, Taxon> result = new LinkedHashMap<>();

      // first load the clade tree
      TableValue cladeTable = tables.get(TABLE_ROOTS);
      Map<Integer,Taxon> cladeIdMap = new LinkedHashMap<>();
      for (Map<String, AttributeValue> row : cladeTable) {
        Taxon taxon = new Taxon(Integer.valueOf(row.get("orthomcl_clade_id").getValue()));
        taxon.setSpecies(false);
        String parentIdStr = row.get("parent_id").getValue();
        if (parentIdStr != null)
          taxon.setParentId(Integer.valueOf(parentIdStr));
        taxon.setAbbrev(row.get("taxon_abbrev").getValue().trim());
        taxon.setTaxonGroup(taxon.getAbbrev());
        taxon.setColor(row.get("color").getValue());
        taxon.setGroupColor(taxon.getColor());
        taxon.setName(row.get("name").getValue().trim());
        taxon.setCommonName(taxon.getName());
        // sort index will be assigned during tree creation
        cladeIdMap.put(taxon.getId(), taxon);
        result.put(taxon.getAbbrev(), taxon);
      }

      // build out clade tree
      for (Taxon taxon : cladeIdMap.values()) {
        Integer parentId = taxon.getParentId();
        if (parentId != null) {
          Taxon parent = cladeIdMap.get(taxon.getParentId());
          if (parent == null)
            throw new WdkModelException("Clade with ID " + taxon.getId() + " has parent ID " + parentId + " which does not match any clade.");
          taxon.setParent(parent);
          parent.addChild(taxon);
        }
      }

      // load species (leaves)
      TableValue taxonTable = tables.get(TABLE_TAXONS);
      List<Taxon> taxonList = new ArrayList<>();
      for (Map<String, AttributeValue> row : taxonTable) {
        Taxon taxon = new Taxon(Integer.valueOf(row.get("orthomcl_clade_id").getValue()));
        taxon.setSpecies(true);
        String parentIdStr = row.get("parent_id").getValue();
        if (parentIdStr != null)
          // setting this but current understanding is this is unused; species are assigned to clades based on name
          taxon.setParentId(Integer.valueOf(parentIdStr));
        else
          throw new WdkModelException("Species row with ID " + taxon.getId() + " does not have a parent (only clades cannot have parents).");
        taxon.setAbbrev(row.get("three_letter_abbrev").getValue().trim());
        taxon.setTaxonGroup(row.get("taxon_group").getValue().trim()); // must match a clade name
        taxon.setColor(row.get("color").getValue());
        taxon.setGroupColor(taxon.getColor());
        taxon.setName(row.get("name").getValue().trim());
        taxon.setCommonName(taxon.getName());
        // sort index will be assigned during tree creation
        taxonList.add(taxon);
        result.put(taxon.getAbbrev(), taxon);
      }

      // assign children
      for (Taxon taxon : taxonList) {
        Taxon parent = result.get(taxon.getTaxonGroup());
        if (parent == null)
          throw new WdkModelException("Species with ID " + taxon.getId() + " has taxon_group " + taxon.getTaxonGroup() + " which does not match any clade.");
        taxon.setParent(parent);
        parent.addChild(taxon);
      }

      return result;
    }
    catch (WdkUserException e) {
      throw new WdkModelException("Unable to generate taxon cache", e);
    }
  }

}
