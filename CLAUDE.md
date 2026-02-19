# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

OrthoMCLModel is the data model layer for OrthoMCL.org, defining data types, searches, attributes, queries, and tables for ortholog groups and protein sequences. The repository uses the WDK (Workflow Development Kit) framework with XML configuration files and Java implementations.

## Core Data Types

The model has two primary data types:
- **Sequences** (SequenceRecordClass): Individual protein sequences from various organisms
- **Groups** (GroupRecordClass): Ortholog groups containing related proteins

## Build System

This project uses a dual build system with both Ant and Maven:

### Building with Ant
```bash
# Build the entire project
bld OrthoMCLModel

# After changes to WDK model XML files, reload Tomcat instance
```

### Building with Maven
```bash
# Build from root directory (builds all modules)
mvn clean install

# Build only the Model module
cd Model && mvn clean install
```

### Running Tests
```bash
# Run all tests with Maven
mvn test

# Run a specific test
mvn test -Dtest=ExpressionTest
mvn test -Dtest=GeneSetLayoutTest
```

### Validation
```bash
# Verify installation success
wdkXml -model $MODEL_NAME
```

## Required Environment Setup

Before building, ensure these are configured:
- `GUS_HOME` environment variable
- `PROJECT_HOME` environment variable
- `WEBAPP_PROP_FILE` file with property: `webappTargetDir=BLAH`

## Architecture

### Directory Structure

```
Model/
├── lib/
│   ├── wdk/OrthoMCL/           # WDK model definitions
│   │   ├── records/            # Record class definitions (groupRecord.xml, sequenceRecord.xml)
│   │   ├── questions/          # Search definitions
│   │   │   ├── params/         # Search parameters
│   │   │   └── queries/        # SQL queries for searches
│   │   └── ontology/           # Ontology files for categorization
│   └── xml/                    # Additional configuration
│       └── orthomclSqlDict.xml # SQL query dictionary
├── src/main/java/org/orthomcl/
│   ├── shared/model/layout/    # Graph layout algorithms for cluster visualization
│   └── service/core/
│       ├── analysis/           # Analysis implementations
│       ├── layout/             # Layout management
│       ├── phyletic/           # Phyletic pattern search logic
│       ├── report/             # Custom reporters (FASTA, etc.)
│       └── wsfplugin/          # Web service plugins (BLAST, keyword search, motif)
└── config/                     # Configuration files
```

### Key Components

**WDK Model XML Files** (`Model/lib/wdk/OrthoMCL/`)
- Define record classes, searches (questions), parameters, and queries
- Organized hierarchically: records → questions → params/queries
- Changes to these files require Tomcat reload

**Java Service Layer** (`Model/src/main/java/org/orthomcl/service/core/`)
- **phyletic/**: Implements phyletic pattern search parser and expression evaluation
  - `ExpressionParser.java`: Parses boolean expressions like "and", "or", "not" with comparison operators
  - `ExpressionParamHandler.java`: Handles phyletic pattern search parameters
- **wsfplugin/**: Web service plugins for external tool integration
  - `OrthoMCLBlastPlugin.java`: BLAST search integration
  - `KeywordSearchPlugin.java`: Text-based keyword searching
  - `MotifPlugin.java`: Protein motif searching
- **layout/**: Cluster layout algorithms for graph visualization
- **report/**: Custom reporters for data export (FASTA format, etc.)

**Graph Layout System** (`Model/src/main/java/org/orthomcl/shared/model/layout/`)
- Force-directed graph layout for visualizing protein clusters
- `ForceGraph.java`: Main graph implementation with force-based node positioning
- Used for cluster graphs (groups with 2-499 proteins)

**Core Domain Classes** (`Model/src/main/java/org/orthomcl/service/core/`)
- `Group.java`, `GroupManager.java`: Ortholog group management
- `Gene.java`, `GeneSet.java`, `GeneSetManager.java`: Protein/gene management
- `Taxon.java`, `TaxonManager.java`: Organism taxonomy management
- `PFamDomain.java`, `EcNumber.java`: Functional annotation entities

### Ontology Generation

During installation, the build process generates OrthoMCL ontology files:
1. `OwlClassGenerator`: Generates `individuals.owl` from `individuals.txt`
2. `EuPathAnnotPropAdder`: Generates `annotation.owl` from `annotation.txt`
3. `OntologyMerger`: Merges ontologies into `categories_merged.owl`

These ontologies categorize searches and data for the web interface.

## Common Searches

The WDK model defines several search types (questions):
- **GroupsBySequenceCount**: Find groups by number of proteins (core/peripheral)
- **GroupsByGenomeCount**: Find groups by taxonomic distribution
- **Phyletic Pattern Search**: Boolean expression-based search across taxa
- **Keyword Search**: Text search across group annotations
- **BLAST Search**: Sequence similarity search
- **Protein Motif Search**: Find proteins by motif patterns

## Dependencies

Internal Dependencies:
- WDK (Workflow Development Kit) - wdk-model
- CBIL, ReFlow, EbrcModelCommon
- EbrcWebSvcCommon (parent dependency in build.xml)
- fgputil-core, fgputil-db, fgputil-json
- wsf-common, wsf-plugin (Web Service Framework)

External Dependencies:
- Maven 3.x
- Java (see parent pom for version)
- Ant for legacy build tasks

## Current Branch Context

Main branch: `master`
Current branch: `query-tuning`

Recent work includes:
- Using groupattr tuning table
- Phyletic pattern search default changes
- Resource summary issue resolution

