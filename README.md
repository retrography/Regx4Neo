Regx4Neo
========

A simple Neo4j server plugin that makes it possible to execute server-side regex-based text transformations on string node properties. This fills a void in Cypher Query Language that I am sure will be addressed soon. 

### Installation 

In order to install the plugin, copy the [`jar` file][1] to `plugins` directory of your Neo4j installation and subsequently restart Neo4j server. This will add three methods to the REST API of Neo4j. To learn more about server plugins and how they work check the [server plugins page][2] from Neo4j's manual.

### Use

First make sure the plugin has been installed correctly by running the following in Neo4j browser:

    // REST API
    :GET /db/data

The response must include the following lines:

    "extensions": {
      "Regexer": {
        "rx_split": "http://localhost:7474/db/data/ext/Regexer/graphdb/rx_split",
        "rx_extract": "http://localhost:7474/db/data/ext/Regexer/graphdb/rx_extract",
        "rx_replace": "http://localhost:7474/db/data/ext/Regexer/graphdb/rx_replace"
      }
    }

Sending a GET request with each of this addresses will inform you about the supported parameters. A POST request along with parameters will run the method.

For instance running `:GET /db/data/ext/Regexer/graphdb/rx_split` will return the following:

    {
      "extends": "graphdb",
      "description": "Detects patterns in string properties, splits the string properties on the detected patterns returns or saves the results.",
      "name": "rx_split",
      "parameters": [
        {
          "name": "label",
          "type": "string",
          "optional": false,
          "description": "The label by which nodes should be filtered."
        },
        {
          "name": "property",
          "type": "string",
          "optional": false,
          "description": "The property on which you would like to run the regexp statement."
        },
        {
          "name": "statement",
          "type": "string",
          "optional": false,
          "description": "The regex statement to identify the split points (Java string formatted)."
        },
        {
          "name": "output",
          "type": "string",
          "optional": true,
          "description": "The node to which you would like to write the results. By default this parameter is empty and the data is returned instead of being saved."
        },
        {
          "name": "limit",
          "type": "integer",
          "optional": true,
          "description": "Limit for the number of times the regex is applied."
        }
      ]
    }

And accordingly, a sample split query would be as follows:

    // REST API
    :POST /db/data/ext/Regexer/graphdb/rx_split 
    {"target":"/db/data", "label":"Split", "property":"name", "statement":"\\s", "limit":2, "output":"tempname"}

This will tokenize the `name` property of all nodes labeled `Split` by breaking the strings on the white spaces. The `limit` parameter restricts this behavior by indicating that only the first pattern match within the string should be used to split the string (a maximum of two strings generated, hence limit = 2).  The query will save the results in an array property called `tempname`.

The server response will be similar to the following:

    [
      "681 properties modified."
    ]

If `output` is omitted, server will simply return the results to the REST client. 

### To-Do

1. Return valid JSON arrays instead of string representations when the output of the query is an array. 
2. Filter by multiple labels
3. Filter by property contents
4. Accept relationships and relationship properties


  [1]: https://github.com/mszargar/Regx4Neo/releases/download/v1.0.0/regx4neo-1.0.0.jar
  [2]: http://docs.neo4j.org/chunked/stable/server-plugins.html