package ca.mcgill.neo4j.plugins.regx4neo;

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

import me.xuender.unidecode.Unidecode;
import org.neo4j.graphdb.*;
import org.neo4j.server.plugins.*;
import org.neo4j.tooling.GlobalGraphOperations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Description("An extension to the Neo4j Server that can run regex on node properties and return or save the results.")
public class Regexer extends ServerPlugin {
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
            @Description("The property to which you would like to write the results. By default this parameter is empty and the data is returned instead of being saved.")
            @Parameter(name = "output", optional = true) String outProperty,
            @Description("Whether all the instances of the pattern should be replaced. Defaults to true.")
            @Parameter(name = "replaceall", optional = true) Boolean all
    ) {
        ArrayList<String> results = new ArrayList<>();
        Label searchLabel = DynamicLabel.label(label);
        Pattern pattern = Pattern.compile(statement);
        int count = 0;

        try (Transaction tx = graphDb.beginTx()) {
            for (Node node : GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(searchLabel)) {
                String result;
                String property = node.getProperty(inProperty, "").toString();
                Matcher matcher = pattern.matcher(property);

                if (all == null || all) {
                    result = matcher.replaceAll(replace);
                } else {
                    result = matcher.replaceFirst(replace);
                }

                if (!(outProperty == null) && !(outProperty.isEmpty())) {
                    node.setProperty(outProperty, result);
                    count++;
                } else {
                    results.add(result);
                }
            }
            tx.success();

        }
        if (count > 0) {
            results.add(count + " properties modified.");
        }
        return results;
    }

    @Name("rx_split")
    @Description("Detects patterns in string properties, splits the string properties on the detected patterns returns or saves the results.")
    @PluginTarget(GraphDatabaseService.class)
    public Iterable<String> rxSplit(
            @Source GraphDatabaseService graphDb,
            @Description("The label by which nodes should be filtered.")
            @Parameter(name = "label", optional = false) String label,
            @Description("The property on which you would like to run the regexp statement.")
            @Parameter(name = "property", optional = false) String inProperty,
            @Description("The regex statement to identify the split points (Java string formatted).")
            @Parameter(name = "statement", optional = false) String statement,
            @Description("The property to which you would like to write the results. By default this parameter is empty and the data is returned instead of being saved.")
            @Parameter(name = "output", optional = true) String outProperty,
            @Description("Limit for the number of times the regex is applied.")
            @Parameter(name = "limit", optional = true) Integer limit
    ) {
        ArrayList<String> results = new ArrayList<>();
        Label searchLabel = DynamicLabel.label(label);
        int count = 0;

        try (Transaction tx = graphDb.beginTx()) {
            for (Node node : GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(searchLabel)) {
                String[] result;

                result = node.getProperty(inProperty, "").toString().split(statement, limit == null ? 0 : limit);

                if (!(outProperty == null) && !(outProperty.isEmpty())) {
                    node.setProperty(outProperty, result);
                    count++;
                } else {
                    // This does not return a real Json. Requires a generalized list-of-list representation object to get there.
                    results.add(Arrays.asList(result).toString());
                }
            }

            tx.success();

        }
        if (count > 0) {
            results.add(count + " properties modified.");
        }
        return results;
    }

    @Name("rx_extract")
    @Description("Detects patterns in string properties and returns or saves the results.")
    @PluginTarget(GraphDatabaseService.class)
    public Iterable<String> rxExtract(
            @Source GraphDatabaseService graphDb,
            @Description("The label by which nodes should be filtered.")
            @Parameter(name = "label", optional = false) String label,
            @Description("The property on which you would like to run the regexp statement.")
            @Parameter(name = "property", optional = false) String inProperty,
            @Description("The regex statement to identify the patterns (Java string formatted).")
            @Parameter(name = "statement", optional = false) String statement,
            @Description("The property to which you would like to write the results. By default this parameter is empty and the data is returned instead of being saved.")
            @Parameter(name = "output", optional = true) String outProperty,
            @Description("The index of the matches to be returned. By default this array is empty and all the matches are returned.")
            @Parameter(name = "matches", optional = true) Integer[] matches
    ) {
        ArrayList<String> results = new ArrayList<>();
        Label searchLabel = DynamicLabel.label(label);
        int count = 0;
        Pattern pattern = Pattern.compile(statement);
        List<Integer> matchesList = Arrays.asList(matches == null ? new Integer[]{} : matches);


        try (Transaction tx = graphDb.beginTx()) {
            for (Node node : GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(searchLabel)) {
                ArrayList<String> result = new ArrayList<>();
                String property = node.getProperty(inProperty, "").toString();
                Matcher matcher = pattern.matcher(property);
                int innerCount = 0;

                while (matcher.find()) {
                    innerCount++;
                    if (matchesList.size() == 0 || matchesList.contains(innerCount)) {
                        result.add(property.substring(matcher.start(), matcher.end()));
                    }
                }

                if (!(outProperty == null) && !(outProperty.isEmpty())) {
                    if (result.size() > 0) {
                        String[] tmpArr = result.toArray(new String[result.size()]);
                        node.setProperty(outProperty, tmpArr);
                        count++;
                    }
                } else {
                    // This does not return a real Json. Requires a generalized list-of-list representation object to get there.
                    if (result.size() > 0) {
                        results.add(result.toString());
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

    @Name("uc_unide")
    @Description("Transliterates unicode characters into their ASCII equivalent using Unidecode library.")
    @PluginTarget(GraphDatabaseService.class)
    public Iterable<String> ucUnide(
            @Source GraphDatabaseService graphDb,
            @Description("The label by which nodes should be filtered.")
            @Parameter(name = "label", optional = false) String label,
            @Description("The property you would like to transliterate.")
            @Parameter(name = "property", optional = false) String inProperty,
            @Description("The property to which you would like to write the results. By default this parameter is empty and the data is returned instead of being saved.")
            @Parameter(name = "output", optional = true) String outProperty
    ) {
        ArrayList<String> results = new ArrayList<>();
        Label searchLabel = DynamicLabel.label(label);
        int count = 0;

        try (Transaction tx = graphDb.beginTx()) {
            for (Node node : GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(searchLabel)) {
                String property = node.getProperty(inProperty, "").toString();

                String result = Unidecode.decode(property);

                if (!(outProperty == null) && !(outProperty.isEmpty())) {
                    node.setProperty(outProperty, result);
                    count++;
                } else {
                    // TODO: This does not return a real Json. Requires a generalized list-of-list representation object to get there.
                    results.add(result);
                }
            }

            tx.success();

        }
        if (count > 0) {
            results.add(count + " properties modified.");
        }
        return results;
    }


    @Name("uc_debone")
    @Description("Detects patterns in string properties, replaces them with a substitute string and returns or saves the resulting strings. In combination with Unide this can be used to provide the right tokens to make suggestions such as email address suggestions for users.")
    @PluginTarget(GraphDatabaseService.class)
    public Iterable<String> ucDebone(
            @Source GraphDatabaseService graphDb,
            @Description("The label by which nodes should be filtered.")
            @Parameter(name = "label", optional = false) String label,
            @Description("The property on which you would like to run the regexp statement.")
            @Parameter(name = "property", optional = false) String inProperty,
            @Description("The property to which you would like to write the results. By default this parameter is empty and the data is returned instead of being saved.")
            @Parameter(name = "output", optional = true) String outProperty,
            @Description("Defines whether numbers should be removed as well (false by default).")
            @Parameter(name = "numbers", optional = true) Boolean numbers,
            @Description("Defines whether spaces should be removed as well (false by default).")
            @Parameter(name = "spaces", optional = true) Boolean spaces,
            @Description("Whether you would like the sentence to be converted to lowercase a well.")
            @Parameter(name = "lower", optional = true) Boolean lower
    ) {
        numbers = numbers == null ? false : numbers;
        spaces = spaces == null ? false : spaces;
        lower = lower == null ? false : lower;

        String removeStr = "[^A-Za-z0-9 ]+";
        if (numbers) removeStr.replace("0-9", "");
        if (spaces) removeStr.replace(" ", "");

        ArrayList<String> results = new ArrayList<>();
        Label searchLabel = DynamicLabel.label(label);
        Pattern ptSpace = Pattern.compile("(\\p{P}|\\s)+");
        Pattern ptRemove = Pattern.compile(removeStr);

        int count = 0;

        try (Transaction tx = graphDb.beginTx()) {
            for (Node node : GlobalGraphOperations.at(graphDb).getAllNodesWithLabel(searchLabel)) {

                String property = node.getProperty(inProperty, "").toString();

                Matcher matcher = ptSpace.matcher(property);
                String result = matcher.replaceAll(" ");

                matcher = ptRemove.matcher(result);
                result = matcher.replaceAll("");

                matcher = ptSpace.matcher(result);
                result = matcher.replaceAll(" ");

                result.trim();

                result = lower == true ? result.toLowerCase(Locale.ENGLISH) : result;

                if (!(outProperty == null) && !(outProperty.isEmpty())) {
                    node.setProperty(outProperty, result);
                    count++;
                } else {
                    results.add(result);
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
