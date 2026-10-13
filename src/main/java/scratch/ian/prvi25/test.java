package scratch.ian.prvi25;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.opensha.commons.data.CSVFile;
import org.opensha.commons.data.CSVReader;
import org.opensha.commons.data.CSVReader.Row;
import org.opensha.commons.logicTree.LogicTree;
import org.opensha.commons.logicTree.LogicTreeBranch;
import org.opensha.commons.logicTree.LogicTreeNode;

public class test {

    private static final String INPUT_DIR =
            "/home/imessa/projects/prvicomp/inputs/site_epi_hazard_curves_2025";

    public static void main(String[] args) throws IOException {
        // Get site/imt/index
        String site = args[0];
        String imt = args[1];
        Integer idx = Integer.parseInt(args[2]);

        // Read hazard curve from site.
        //   Equivalent py path: read_csv('inputs/site_epi_hazard_curves_2025/%s_%s.csv' % (site, imt))[idx]
        // These files are large (~100k rows), so stream with CSVReader and stop at the target
        // row rather than loading the whole file into memory.
        File hazardFile = new File(INPUT_DIR, site + "_" + imt + ".csv");
        List<String> hazardCurve = readRowByIndex(hazardFile, idx);
        System.out.println("Hazard curve (row " + idx + "): " + hazardCurve);

        // Read logic tree input.
        //   Equivalent py path: read_csv('inputs/site_epi_hazard_curves_2025/logic_tree.json')[idx]
        // The JSON logic tree is read with OpenSHA's LogicTree; getBranch(idx) is the row-index equivalent.
        File logicTreeFile = new File(INPUT_DIR, "logic_tree.json");
        LogicTree<LogicTreeNode> logicTree = LogicTree.read(logicTreeFile);
        LogicTreeBranch<LogicTreeNode> branch = logicTree.getBranch(idx);
        System.out.println("Logic tree branch (index " + idx + "): " + branch);
        System.out.println("Logic tree branch weight: " + logicTree.getBranchWeight(idx));

        // Read site lon/lat.
        //   Equivalent py path: read_csv('inputs/site_epi_hazard_curves_2025/sites.csv')
        //       [row where 'Site Filename Prefix' == site]
        // sites.csv is small, so just load it fully and look up the matching prefix column.
        File sitesFile = new File(INPUT_DIR, "sites.csv");
        double[] lonLat = readSiteLonLat(sitesFile, site);
        System.out.println("Site " + site + " -> lon=" + lonLat[0] + ", lat=" + lonLat[1]);

        // LEAVE THIS FOR LATER
        // File crustalDir = new File("/home/imessa/projects/prvicomp/inputs/2025_PRVI_NSHM_Crustal_Epistemic_Fault_System_Solutions.zip");
        // File crustalFile = new File(crustalDir, "logic_tree_full_gridded.json");
        // To read a branch from the zips, we need to use
        // LogicTree.read(file)
    }

    /**
     * Stream a CSV file and return the data row at the given 0-based index (excluding the header).
     * Uses CSVReader so we don't load the entire (large) file into memory.
     */
    private static List<String> readRowByIndex(File file, int idx) throws IOException {
        try (CSVReader reader = new CSVReader(new BufferedInputStream(new FileInputStream(file)))) {
            // Skip the header row.
            Row header = reader.read();
            if (header == null)
                throw new IOException("Empty CSV file: " + file.getAbsolutePath());

            int row = 0;
            Row current;
            while ((current = reader.read()) != null) {
                if (row == idx)
                    return current.getLine();
                row++;
            }
        }
        throw new IndexOutOfBoundsException(
                "Row index " + idx + " not found in " + file.getAbsolutePath());
    }

    /**
     * Load sites.csv and return {longitude, latitude} for the row whose
     * "Site Filename Prefix" column equals the given site.
     */
    private static double[] readSiteLonLat(File file, String site) throws IOException {
        CSVFile<String> csv = CSVFile.readFile(file, true);
        // Header: Site Name,Latitude,Longitude,Site Filename Prefix
        List<String> header = csv.getLine(0);
        int latCol = header.indexOf("Latitude");
        int lonCol = header.indexOf("Longitude");
        int prefixCol = header.indexOf("Site Filename Prefix");
        if (latCol < 0 || lonCol < 0 || prefixCol < 0)
            throw new IOException("Unexpected sites.csv header: " + header);

        for (int row = 1; row < csv.getNumRows(); row++) {
            if (site.equals(csv.get(row, prefixCol))) {
                double lat = Double.parseDouble(csv.get(row, latCol));
                double lon = Double.parseDouble(csv.get(row, lonCol));
                return new double[] { lon, lat };
            }
        }
        throw new IllegalArgumentException(
                "Site prefix '" + site + "' not found in " + file.getAbsolutePath());
    }
}
