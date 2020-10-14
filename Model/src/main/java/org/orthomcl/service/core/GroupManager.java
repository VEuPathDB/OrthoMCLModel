package org.orthomcl.service.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gusdb.fgputil.runtime.InstanceManager;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.record.DynamicRecordInstance;
import org.gusdb.wdk.model.record.RecordClass;
import org.gusdb.wdk.model.record.RecordInstance;
import org.gusdb.wdk.model.record.TableValue;
import org.gusdb.wdk.model.record.attribute.AttributeValue;
import org.gusdb.wdk.model.user.User;
import org.orthomcl.service.core.layout.RenderingHelper;

public class GroupManager {

  private static final String RECORD_CLASS = "GroupRecordClasses.GroupRecordClass";
  private static final String GROUP_NAME_KEY = "group_name";

  private static final String GENES_TABLE = "Sequences";
  private static final String PFAMS_TABLE = "PFams";
  private static final String PROTEIN_PFAMS_TABLE = "ProteinPFams";

  private final WdkModel _wdkModel;

  public GroupManager(WdkModel wdkModel) {
    _wdkModel = wdkModel;
  }

  public static void main(String[] args) throws WdkModelException, WdkUserException {
    WdkModel wdkModel = InstanceManager.getInstance(WdkModel.class, "OrthoMCL");
    GroupManager manager = new GroupManager(wdkModel);
    User user = wdkModel.getSystemUser();
    manager.getGroup(user, "OG5_133610");
    System.exit(0);
  }

  public RecordInstance getGroupRecord(User user, String name) throws WdkModelException, WdkUserException {
    RecordClass recordClass = _wdkModel.getRecordClassByFullName(RECORD_CLASS).get();
    Map<String, Object> pkValues = new LinkedHashMap<>();
    pkValues.put(GROUP_NAME_KEY, name);
    RecordInstance instance = new DynamicRecordInstance(user, recordClass, pkValues);
    return instance;
  }

  public Group getGroup(User user, String name) throws WdkModelException, WdkUserException {
    RecordInstance groupRecord = getGroupRecord(user, name);
    return getGroup(groupRecord);
  }

  public Group getGroup(RecordInstance groupRecord) throws WdkModelException, WdkUserException {
    String name = groupRecord.getPrimaryKey().getValues().get(GROUP_NAME_KEY);
    Group group = new Group(name);

    // get Taxons
    Map<String, Taxon> taxons = TaxonManager.getTaxons(_wdkModel);

    // load genes
    Map<String, Integer> ecNumbers = new HashMap<>();
    TableValue genesTable = groupRecord.getTableValue(GENES_TABLE);
    for (Map<String, AttributeValue> row : genesTable) {
      Gene gene = new Gene(row.get("full_id").getValue());
      gene.setDescription(row.get("description").getValue());
      gene.setLength(Long.valueOf(row.get("length").getValue().toString()));

      // get taxon
      String taxonAbbrev = row.get("taxon_abbrev").getValue();
      gene.setTaxon(taxons.get(taxonAbbrev));

      String ecNumberString = row.get("ec_numbers").getValue();
      if (ecNumberString != null) {
        for (String ecNumber : ecNumberString.split(",")) {
          ecNumber = ecNumber.trim();
          if (ecNumber.length() > 0) {
            gene.addEcNumber(ecNumber);
            if (ecNumbers.containsKey(ecNumber)) {
              ecNumbers.put(ecNumber, ecNumbers.get(ecNumber) + 1);
            }
            else {
              ecNumbers.put(ecNumber, 1);
            }
          }
        }
      }
      group.addGene(gene);
    }

    // load pfam info
    loadPFamDomains(group, groupRecord);

    // TODO - process ec numbers to form tree
    processEcNumbers(group, ecNumbers);

    return group;
  }

  private void loadPFamDomains(Group group, RecordInstance groupRecord) throws WdkModelException,
      WdkUserException {
    // load pfam domian information
    List<PFamDomain> pfams = new ArrayList<>();
    TableValue pfamsTable = groupRecord.getTableValue(PFAMS_TABLE);
    for (Map<String, AttributeValue> row : pfamsTable) {
      PFamDomain pfam = new PFamDomain(row.get("accession").getValue());
      pfam.setSymbol(row.get("symbol").getValue());
      pfam.setDescription(row.get("description").getValue());
      pfam.setCount(Integer.valueOf(row.get("occurrences").getValue().toString()));
      pfam.setIndex(Integer.valueOf(row.get("domain_index").getValue().toString()));
      group.addPFamDomain(pfam);
      pfams.add(pfam);
    }

    // generate random color for the domains
    Collections.sort(pfams);
    RenderingHelper.assignSpectrumColors(pfams);

    // load protein pfam information to each gene
    TableValue proteinPFamsTable = groupRecord.getTableValue(PROTEIN_PFAMS_TABLE);
    for (Map<String, AttributeValue> row : proteinPFamsTable) {
      String sourceId = row.get("full_id").getValue();
      String accession = row.get("accession").getValue();
      if (accession != null) {
        int[] location = new int[3];
        location[0] = Integer.valueOf(row.get("start_min").getValue().toString());
        location[1] = Integer.valueOf(row.get("end_max").getValue().toString());
        location[2] = Integer.valueOf(row.get("protein_length").getValue().toString());
        Gene gene = group.getGene(sourceId);
        gene.addPFamDomain(accession, location);
      }
    }
  }

  private void processEcNumbers(Group group, Map<String, Integer> ecNumberCodes) {
    // determine the index of each ecNumber
    List<EcNumber> ecNumbers = new ArrayList<>();
    for (String code : ecNumberCodes.keySet()) {
      EcNumber ecNumber = new EcNumber(code);
      ecNumber.setCount(ecNumberCodes.get(code));
      group.addEcNumber(ecNumber);
      ecNumbers.add(ecNumber);
    }

    Collections.sort(ecNumbers);
    for (int i = 0; i < ecNumbers.size(); i++) {
      ecNumbers.get(i).setIndex(i);
    }

    // assign spectrum colors to the ec numbers
    RenderingHelper.assignSpectrumColors(ecNumbers);
  }
}
