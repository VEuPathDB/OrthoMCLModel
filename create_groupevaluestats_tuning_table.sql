-- Create apidbtuning.groupevaluestats tuning table
-- This table pivots the tall apidb.orthologgroupstats table into a wide format
-- with 15 evalue columns (3 protein_subsets × 5 stat_types)

DROP TABLE IF EXISTS apidbtuning.groupevaluestats;

CREATE TABLE apidbtuning.groupevaluestats (
    group_name VARCHAR(50) PRIMARY KEY,
    -- Core statistics (protein_subset = 'C')
    core_min_evalue TEXT,
    core_25_evalue TEXT,
    core_median_evalue TEXT,
    core_75_evalue TEXT,
    core_max_evalue TEXT,
    -- Core + Peripheral statistics (protein_subset = 'C+P')
    core_peripheral_min_evalue TEXT,
    core_peripheral_25_evalue TEXT,
    core_peripheral_median_evalue TEXT,
    core_peripheral_75_evalue TEXT,
    core_peripheral_max_evalue TEXT,
    -- Residual statistics (protein_subset = 'R')
    residual_min_evalue TEXT,
    residual_25_evalue TEXT,
    residual_median_evalue TEXT,
    residual_75_evalue TEXT,
    residual_max_evalue TEXT
);

-- Populate the table using conditional aggregation to pivot the data
INSERT INTO apidbtuning.groupevaluestats
SELECT
    og.group_id AS group_name,
    -- Core statistics
    MAX(CASE WHEN ogs.protein_subset = 'C' AND ogs.stat_type = 'min' THEN ogs.evalue END) AS core_min_evalue,
    MAX(CASE WHEN ogs.protein_subset = 'C' AND ogs.stat_type = '25 PCT' THEN ogs.evalue END) AS core_25_evalue,
    MAX(CASE WHEN ogs.protein_subset = 'C' AND ogs.stat_type = 'median' THEN ogs.evalue END) AS core_median_evalue,
    MAX(CASE WHEN ogs.protein_subset = 'C' AND ogs.stat_type = '75 PCT' THEN ogs.evalue END) AS core_75_evalue,
    MAX(CASE WHEN ogs.protein_subset = 'C' AND ogs.stat_type = 'max' THEN ogs.evalue END) AS core_max_evalue,
    -- Core + Peripheral statistics
    MAX(CASE WHEN ogs.protein_subset = 'C+P' AND ogs.stat_type = 'min' THEN ogs.evalue END) AS core_peripheral_min_evalue,
    MAX(CASE WHEN ogs.protein_subset = 'C+P' AND ogs.stat_type = '25 PCT' THEN ogs.evalue END) AS core_peripheral_25_evalue,
    MAX(CASE WHEN ogs.protein_subset = 'C+P' AND ogs.stat_type = 'median' THEN ogs.evalue END) AS core_peripheral_median_evalue,
    MAX(CASE WHEN ogs.protein_subset = 'C+P' AND ogs.stat_type = '75 PCT' THEN ogs.evalue END) AS core_peripheral_75_evalue,
    MAX(CASE WHEN ogs.protein_subset = 'C+P' AND ogs.stat_type = 'max' THEN ogs.evalue END) AS core_peripheral_max_evalue,
    -- Residual statistics
    MAX(CASE WHEN ogs.protein_subset = 'R' AND ogs.stat_type = 'min' THEN ogs.evalue END) AS residual_min_evalue,
    MAX(CASE WHEN ogs.protein_subset = 'R' AND ogs.stat_type = '25 PCT' THEN ogs.evalue END) AS residual_25_evalue,
    MAX(CASE WHEN ogs.protein_subset = 'R' AND ogs.stat_type = 'median' THEN ogs.evalue END) AS residual_median_evalue,
    MAX(CASE WHEN ogs.protein_subset = 'R' AND ogs.stat_type = '75 PCT' THEN ogs.evalue END) AS residual_75_evalue,
    MAX(CASE WHEN ogs.protein_subset = 'R' AND ogs.stat_type = 'max' THEN ogs.evalue END) AS residual_max_evalue
FROM apidb.orthologgroup og
LEFT JOIN apidb.orthologgroupstats ogs ON og.group_id = ogs.group_id
GROUP BY og.group_id;

-- Create index for performance
CREATE INDEX groupevaluestats_group_name_idx ON apidbtuning.groupevaluestats(group_name);

-- Verify the results
SELECT 'Total groups in tuning table:' AS info, COUNT(*) AS count FROM apidbtuning.groupevaluestats
UNION ALL
SELECT 'Total groups in source table:', COUNT(DISTINCT group_id) FROM apidb.orthologgroup;

-- Sample a few rows to verify data looks correct
SELECT * FROM apidbtuning.groupevaluestats LIMIT 5;
