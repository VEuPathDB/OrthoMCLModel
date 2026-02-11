package org.orthomcl.service.core;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.TableValue;
import org.gusdb.wdk.model.record.attribute.AttributeValue;

public class TaxonManager {

  //private static final Logger LOG = Logger.getLogger(TaxonManager.class);

  private static final String TABLE_TAXONS = "Taxons";
  private static final String TABLE_ROOTS = "RootTaxons";

  // lazy-loaded cache of taxon data
  private static Map<String, Taxon> TAXONS;

  public static Map<String, Taxon> getTaxons(WdkModel wdkModel) throws WdkModelException {
    if (TAXONS == null) {
      TAXONS = loadTaxons(wdkModel);
      //LOG.info("Here's the tree:\n" + dumpTaxons(TAXONS.values(), "", new StringBuilder()));
      //LOG.info("Done.  Here's the JSON:\n" + ToJson.mapToJson(TAXONS).toString());
    }
    return TAXONS;
  }

  @SuppressWarnings("unused")
  private static String dumpTaxons(Collection<Taxon> taxons, String indent, StringBuilder out) {
    for (Taxon taxon : taxons) {
      out.append(indent + taxon.getId() + " (" + taxon.getAbbrev() + ")\n");
      dumpTaxons(taxon.getChildrenMap().values(), indent + "  ", out);
    }
    return out.toString();
  }

  private static synchronized Map<String, Taxon> loadTaxons(WdkModel wdkModel) throws WdkModelException {
    //LOG.info("Loading taxons...");
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

      //LOG.info("Dumping tree of clades:\n" + dumpTaxons(List.of(result.get("ALL")), "", new StringBuilder()));

      // load species (leaves)
      TableValue taxonTable = tables.get(TABLE_TAXONS);

      for (Map<String, AttributeValue> row : taxonTable) {
        Taxon taxon = new Taxon(Integer.valueOf(row.get("orthomcl_clade_id").getValue()));
        taxon.setAbbrev(row.get("three_letter_abbrev").getValue().trim());
        taxon.setParentId(Integer.valueOf(row.get("parent_id").getValue()));
        taxon.setTaxonGroup(row.get("taxon_group").getValue().trim());
        taxon.setSpecies(true);
        taxon.setColor(row.get("color").getValue());
        taxon.setName(row.get("name").getValue().trim());
        taxon.setCommonName(taxon.getName());
        taxon.setSortIndex(sortIndex++);

        // find the parent clade
        Taxon parent = cladeIdMap.get(taxon.getParentId());
        if (parent == null)
          throw new WdkModelException("Species with ID " + taxon.getId() + " has parent ID " + taxon.getParentId() + " which does not match any clade.");
        if (parent.getName().equals(taxon.getTaxonGroup())) {
          // taxon group and parent ID match; add this taxon as a child (don't add if taxon group is a higher ancestor, not the direct parent)
          taxon.setParent(parent);
          parent.addChild(taxon);
          result.put(taxon.getAbbrev(), taxon);
        }
        else {
          //LOG.info("Will skip adding duplicate of taxon " + taxon.getId() + " (" + taxon.getAbbrev() + ") because parent_id " + taxon.getParentId() + " does not match taxon group " + taxon.getTaxonGroup());
        }
      }

      return result;
    }
    catch (WdkUserException e) {
      throw new WdkModelException("Unable to generate taxon cache", e);
    }
  }

}
