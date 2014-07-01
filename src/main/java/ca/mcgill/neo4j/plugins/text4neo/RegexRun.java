package ca.mcgill.neo4j.plugins.text4neo;

/**
 * Copyright (c) 2014 "Mahmood S. Zargar"
 *
 * This file is a Neo4j REST API Server Plugin.
 *
 * This plugin is offered as free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.neo4j.graphdb.*;
import org.neo4j.server.plugins.*;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.ArrayList;

@Description("An extension to the Neo4j Server that can run regex on node properties and return or save the results.")
public class RegexRun extends ServerPlugin {
    @Name("rx_replace")
    @Description("Detects patterns in string properties, replaces them with a substitute string and returns or saves the resulting strings.")
    @PluginTarget(GraphDatabaseService.class)
    public Iterable<String> rxReplace(
            @Source GraphDatabaseService graphDb,
            @Description("The label by which nodes should be filtered.")
            @Parameter(name = "label", optional = false) String label,
            @Description("The property on which you would like to run the regexp statement.")
            @Parameter(name = "property", optional = false) String inProperty,
            @Description("The regex statement you would like to run (Java string formatted).")
            @Parameter(name = "statement", optional = false) String statement,
            @Description("The string with which you want to replace the pattern.")
            @Parameter(name = "replacement", optional = false) String replace,
            @Description("The node to which you would like to write the results. By default this parameter is empty and the data is returned instead of being saved.")
            @Parameter(name = "output", optional = true) String outProperty,
            @Description("Whether all the instances of the pattern should be replaced. Defaults to true.")
            @Parameter(name = "replaceall", optional = true) Boolean all
    ) {
        ArrayList<String> results = new ArrayList<>();
        Label searchLabel = DynamicLabel.label(label);
        int count = 0;

        try (Transaction tx = graphDb.beginTx()) {
            for (Node node : GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(searchLabel)) {
                String result;

                if (all == null || all) {
                    result = node.getProperty(inProperty, "").toString().replaceAll(statement, replace);
                } else {
                    result = node.getProperty(inProperty, "").toString().replaceFirst(statement, replace);
                }

                if (!result.isEmpty()) {
                    if (!(outProperty == null) && !(outProperty.isEmpty())) {
                        node.setProperty(outProperty, result);
                        count++;
                    } else {
                        results.add(result);
                    }
                }
            }
            tx.success();

        }
        if (count > 0) {
            results.add(count + " properties modified.");
        }
        return results;
    }

}
