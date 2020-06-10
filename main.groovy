/**
    Dmitrijs Voronovs - dv18034
    Uzdevums 3
    Groovy

    ---------------------------

    This program Reads HTML file, extracts all HTML tags and creates a frequency chart.
    In this task I tried to use as many Groovy-specific features as possible.
    All of them are commented and explained!

    ---------------------------

    Step dy step description:
    1. Read HTML from file
    2. Get all HTML tags using RegEx
    3. Make a collection of Tag classes and sort them by popularity
    4. Create a frequency chart
    5. Print the chart in console and write it to file
**/

import groovy.transform.ToString

// The @ToString annotation instructs the compiler to add the necessary toString() method.
// Groovy introduced default accessors and mutators (getters, setters) for every attribute,
// therefore no need to define them explicitly
@ToString class Tag {
    static int totalTagsQty = 0
    String name
    int qty
    float percentage
}

/** Constants **/

final String INPUT_FILE_NAME = 'inputHTML.txt'
final String OUTPUT_FILE_NAME = 'chart.txt'
// used for graphical representation
final int MAX_WIDTH = 100
final int TAG_COLUMN_WIDTH = 10
final int QTY_COLUMN_WIDTH = 6
final int PERCENT_COLUMN_WIDTH = 7

// explained below. Leave 'false' to make it work with most of the HTML input files.
final Boolean USE_ADVANCED_PATTER = false;

/** Functions as closures **/

// get markers for the graphical representation
def getMarkers = { marker, count ->
    // (1..count) creates a list of values from 1 to count, e.g. [1, 2, 3, .., count]
    // inject is reducer method in groovy, initial values '', every iteration appends accumulator with marker
    (1..count).inject('') { acc, _ -> acc + marker }

    // the same result could be achieved using function String.multiply()
    // marker.multiply(count)
}

// Creates a frequency graph and returns it as a string
def getFrequencyGraphAsString = { list, header, paddings ->
    // for convenience result is stored in string array
    def resultLines = []

    // String with a placeholder expressions
    def totalString = "Total tags found: ${ Tag.totalTagsQty }"
    // Here using System.lineSeparator as on UNIX and Windows systems they are different '\n' and '\r\n' accordingly
    totalString += System.lineSeparator();
    totalString += "Tags: ${ (list*.name).join(', ') }"
    resultLines.add(totalString);

    // using method chaining
    def headerString = header
        // in order to get indexes of elemnts for collect method
        .withIndex()
        // using closure with explicit name of parameters
        .collect { val, i -> val.toString().center(paddings[i]) }
        .join('|')

    def delimiter = getMarkers('-', headerString.size() + paddings.size() + 1)

    // adding multiple elements to the list
    resultLines.addAll(delimiter, headerString, delimiter)

    // using a closure to specify the behaviour of the max function
    int maxTagQty = list.max{ it.percentage }.percentage

    list.each { tag ->
        // type casting
        def markerCount = (int) paddings[3] / maxTagQty * tag.percentage
        // convenien function pad(Left|Center|Right) for text padding
        def beautifulString = tag.name.padLeft(paddings[0])
        beautifulString += " ${ tag.qty.toString().padLeft(paddings[1]) }"
        beautifulString += " ${ tag.percentage.toString().padLeft(paddings[2]) }%"
        beautifulString += "  ${ getMarkers(".", markerCount).padRight(paddings[3]) }"
        // adding single element to the list
        resultLines.add(beautifulString)
    }

    resultLines.add(delimiter)
    result = resultLines.join(System.lineSeparator())

    // no need to explicitly type return
    result
}

// creates a frequency graph, writes it to file and to the console
def createGraph = { list, header, paddings, filename ->
    // if no tags were found - stop the program
    if (!list.size()) return

    def outFile = new File(filename)
    def result = getFrequencyGraphAsString(list, header, paddings)
    print result

    // this way we ensure that a NEW file gets created EVERY TIME before writing any data to it
    outFile.newWriter().withWriter { w ->
        w << result
    }
}

// get file contents as array of string and concatenate to one big string
def getText = { filename ->
    // Coercion Operator
    def array = new File(filename) as String[]
    array.join(System.lineSeparator());
}

/**
    Important notes!

    There is a regular expression used in the closure below.
    Advanced vs Basic pattern.

    * Advanced pattern

        This is a powerful patter, that does more than required in the program. It matches not only tag names, but also all of the attributes

        When testing on google search sometimes got
        -- Caught: java.lang.StackOverflowError
        -- java.lang.StackOverflowError
        Unfortunately, Java's builtin regex support has problems with regexes containing repetitive alternative paths (that is, (A|B)*).
        This is compiled into a recursive call, which results in a StackOverflow error when used on a very large string.

        Therefore there is nothing I can do about this error.

    * Basic pattern

        The only possible solution was to optimize the RegEx to look for only '<tag_name' values instead of the whole tag.
        This is exactly what basic pattern does.

    Patterns can be switched using the USE_ADVANCED_PATTER constant that is defined on the very top in constant section
**/
// extracts tags from the text
def extractTagsFromText = { text ->
    // It looks for all opening HTML tags.
    // ---------------------------
    // Example:
    // Full match	23-100	<html itemscope itemtype="http://schema.org/QAPage" class="html__responsive">
    // Group 1.	23-24	<
    // Group 2.	24-28	html
    // -- with Advanced Pattern --
    // Group 3.	29-99	itemscope itemtype="http://schema.org/QAPage" class="html__responsive"
    // Group 4.	99-100	>
    // ---------------------------
    // This task requires from us only tag names, therefore we will take only group 2 from the matcher object
    final int TAG_NAME_GROUP = 2
    // Using pattern operator (not required in this context) that is a syntactic sugar shortcut to Java's java.util.regex.Pattern.compile(string) method
    def advancedPattern = ~/(<)([A-Za-z1-6]+)(?:\s)?((?:.|\s)*?)?(>)/
    def basicPattern = ~/(<)([A-Za-z1-6]+?)(>|\/|\s)/

    // Ternary operator
    pattern = USE_ADVANCED_PATTER ? advancedPattern : basicPattern

    // Find operator that creates a java.util.regex.Matcher instance
    def matcher = text =~ pattern
    // Inside a closure using an implicit parameter (it)
    // Inside a closure using evlis operator (?:) to make sure that program does not fail in special cases
    //     (when, for instance, TAG_NAME_GROUP = 3, so user looks at tag attributes, that sometimes are not present)
    def matchedTags = matcher.collect { it[TAG_NAME_GROUP] ?: '' }

    // Setting static property of the class
    Tag.totalTagsQty = matchedTags.size

    matchedTags
}

def sortAndInstantiateTags = { tags ->
    def sortedTags = tags
        // groups tags by tag name
        .groupBy { it }
        // calculates how many tag entries are under certain group and instantiates tags
        .collect { tagName, values ->
            def qty = values.size()
            def percentage = (qty / Tag.totalTagsQty * 100).round(2)
            // Groovy has named parameters, using that in constructor
            new Tag(name: tagName, qty: qty, percentage: percentage )
        }
        // defining corting behaviour with closure
        .sort { it.qty }
        .reverse()

    sortedTags
}

/** MAIN **/

// as Groovy is optionally typed language, we can define variables with def, without explicitly indicating the type
def text = getText(INPUT_FILE_NAME)
def tags = extractTagsFromText(text)
def tagCollection = sortAndInstantiateTags(tags)
def tagHeader = ['Tag', 'qty', '%', ' graphical representation']
def paddings = [TAG_COLUMN_WIDTH, QTY_COLUMN_WIDTH, PERCENT_COLUMN_WIDTH, MAX_WIDTH]

// implicit closure call
// explicit call would be createGraph.call(...args)
createGraph(tagCollection, tagHeader, paddings, OUTPUT_FILE_NAME)
