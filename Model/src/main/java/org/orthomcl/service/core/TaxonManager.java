package org.orthomcl.service.core;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.TableValue;
import org.gusdb.wdk.model.record.attribute.AttributeValue;

public class TaxonManager {

  private static final Logger LOG = Logger.getLogger(TaxonManager.class);

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
    LOG.info("Loading taxons...");
    try {
      // get helper record tables
      Map<String, TableValue> tables = HelperRecord.get(wdkModel).getTableValueMap();

      // map to hold return value (abbrev -> Taxon)
      Map<String, Taxon> result = new LinkedHashMap<>();

      // initialize universal sort index
      int sortIndex = 1;

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
        taxon.setGroupColor(row.get("color").getValue());
        taxon.setName(row.get("name").getValue().trim());
        taxon.setCommonName(taxon.getName());
        taxon.setSortIndex(sortIndex++);
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
      //List<Taxon> taxonList = new ArrayList<>();
      for (Map<String, AttributeValue> row : taxonTable) {
        Taxon taxon = new Taxon(Integer.valueOf(row.get("orthomcl_clade_id").getValue()));
        taxon.setAbbrev(row.get("three_letter_abbrev").getValue().trim());
        taxon.setParentId(Integer.valueOf(row.get("parent_id").getValue()));
        taxon.setTaxonGroup(row.get("taxon_group").getValue().trim());

        if (result.containsKey(taxon.getAbbrev())) {
          Taxon old = result.get(taxon.getAbbrev());
          LOG.info("Found duplicate taxon and will skip new one!");
          LOG.info("Old: "+ old.getId() + "(" + old.getAbbrev() + "), parent " + old.getParentId() + ", group " + old.getTaxonGroup());
          LOG.info("New: "+ taxon.getId() + "(" + taxon.getAbbrev() + "), parent " + taxon.getParentId() + ", group " + taxon.getTaxonGroup());
          continue;
        }

        taxon.setSpecies(true);
        taxon.setColor(row.get("color").getValue());
        taxon.setName(row.get("name").getValue().trim());
        taxon.setCommonName(taxon.getName());
        taxon.setSortIndex(sortIndex++);

        //taxonList.add(taxon);
        result.put(taxon.getAbbrev(), taxon);

        // assign parent (same logic as above)
        Taxon parent = cladeIdMap.get(taxon.getParentId());
        if (parent == null)
          throw new WdkModelException("Species with ID " + taxon.getId() + " has parent ID " + taxon.getParentId() + " which does not match any clade.");
        taxon.setParent(parent);
        parent.addChild(taxon);
      }

      // assign children
/*      for (Taxon taxon : taxonList) {
        Taxon parent = result.values().stream()
            .filter(Taxon::isClade)
            .filter(t -> t.getName().equals(taxon.getTaxonGroup()))
            .findFirst()
            .orElseThrow(() -> new WdkModelException("Species with ID " + taxon.getId() + " has taxon_group " +
                taxon.getTaxonGroup() + " which does not match any clade."));
        taxon.setParent(parent);
        parent.addChild(taxon);
      }*/

      return result;
    }
    catch (WdkUserException e) {
      throw new WdkModelException("Unable to generate taxon cache", e);
    }
  }

}
