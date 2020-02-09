import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    public static Node parse(String file) {
        String content = "";
        try {
            content = readFile(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Node node;
        if (content.charAt(0) == '<') {
            content = content.replaceAll("<\\?.+?\\?>", "");
            node = parseXML(content);
            node.setType("XML");
        }
        else {
            node = parseJSON(content);
            node.setType("JSON");
        }
        return node;
    }

    public static Node parseJSON(String json) {
        json = json.replaceAll("\\s{2,}|\t|\n", "");
        json = json.substring(1, json.length()-1);
        Node node = new Node();
        parseJSON(json, node);
        return node;
    }

    public static void parseJSON(String json, Node parents) {
        final String parent = parents.getName();
        int numberAllegedItems = 0;

        List<String> headList = new ArrayList<>();
        List<String> allContentList = new ArrayList<>();
        List<Boolean> hasArray = new ArrayList<>();
        List<Boolean> hasObject = new ArrayList<>();
        while (!json.replaceAll(",", "").isEmpty()){

            StringBuilder commaCleaner = new StringBuilder(json);
            while (commaCleaner.charAt(0) == ',') {
                commaCleaner.deleteCharAt(0);
            }
            json = commaCleaner.toString();

            Matcher matcher = json.chars().parallel().filter(ch -> ch == ':').count() == 1 ? Pattern.compile("^,?\"([^\"]*)\"\\s*:\\s*([^{\\[}]+)$").matcher(json): Pattern.compile("^,?\"([^\"]*)\"\\s*:\\s*([^{\\[}]+?),").matcher(json);
            if (matcher.find()) {
                if (matcher.group(1).matches("(#|@|)[^@#]+")) {
                    headList.add(matcher.group(1));
                    allContentList.add(matcher.group(2));
                }
                json = json.replaceFirst(matcher.group(), "");
                numberAllegedItems += 1;
                hasArray.add(false);
                hasObject.add(false);
                continue;
            }
            Matcher matcherForArrays = json.chars().parallel().filter(ch -> ch == ',').count() == 0 ? Pattern.compile("^\\s*([^{\\[}]+)$").matcher(json): Pattern.compile("^\\s*([^{\\[}]+?),").matcher(json);
            if (matcherForArrays.find()) {
                headList.add("element");
                allContentList.add(matcherForArrays.group(1));
                json = json.replaceFirst(matcherForArrays.group(), "");
                numberAllegedItems += 1;
                hasArray.add(false);
                hasObject.add(false);
                continue;
            }

            String firstPartPattern = parents.hasArray() ? "": "\"([^\",]*)\"\\s*:";
            if (json.contains("[")) {
                Matcher emptyMatcher = Pattern.compile("^" + firstPartPattern + "\\s*\\[(\\s*)]").matcher(json);
                Matcher contentMatcher = Pattern.compile("^" + firstPartPattern + "\\s*\\[(.+?])").matcher(json);
                boolean emptyMatcherFind = emptyMatcher.find();
                boolean contentMatcherFind = contentMatcher.find();
                if (emptyMatcherFind && !contentMatcherFind || (contentMatcherFind && emptyMatcherFind && emptyMatcher.start() <= contentMatcher.start())) {
                    hasArray.add(false);
                    hasObject.add(false);
                    if (parents.hasArray()) {
                        headList.add("element");
                        allContentList.add("");
                    } else {
                        if (emptyMatcher.group(1).matches("(#|@|)[^@#]+")) {
                            headList.add(emptyMatcher.group(1));
                            allContentList.add("");
                        }
                    }
                    numberAllegedItems += 1;
                    json = json.replaceFirst(Pattern.quote(emptyMatcher.group()), "");
                    continue;
                }
                if (contentMatcherFind){
                    hasArray.add(true);
                    hasObject.add(false);
                    json = json.replaceFirst(Pattern.quote(contentMatcher.group()), "");
                    int group = parents.hasArray() ? 1: 2;

                    StringBuilder contentBuilder = new StringBuilder("[" + contentMatcher.group(group));
                    long openBracketCount = contentBuilder.chars().parallel().filter(ch -> ch == '[').count();
                    long closeBracketCount = contentBuilder.chars().parallel().filter(ch -> ch == ']').count();
                    Matcher additionalMatcher = Pattern.compile(".*?],?").matcher(json);
                    while (openBracketCount > closeBracketCount && additionalMatcher.find()){
                        String additionalContent = additionalMatcher.group();
                        contentBuilder.append(additionalContent);
                        json = json.replaceFirst(Pattern.quote(additionalContent), "");
                        closeBracketCount = contentBuilder.chars().filter(ch -> ch == ']').count();
                        openBracketCount = contentBuilder.chars().filter(ch -> ch == '[').count();
                    }
                    contentBuilder.deleteCharAt(0);
                    json = json.replaceFirst(Pattern.quote(contentBuilder.toString()), "");
                    if (contentBuilder.charAt(contentBuilder.length()-1) == ',') {
                        contentBuilder.deleteCharAt(contentBuilder.length()-1);
                    }
                    if (contentBuilder.charAt(contentBuilder.length()-1) == ']') {
                        contentBuilder.deleteCharAt(contentBuilder.length()-1);
                    }
                    String content = contentBuilder.toString();
                    if (parents.hasArray()) {
                        headList.add("element");
                        allContentList.add(content);
                    } else {
                        if (contentMatcher.group(1).matches("(#|@|)[^@#]+")) {
                            headList.add(contentMatcher.group(1));
                            allContentList.add(content);
                        }
                    }
                    numberAllegedItems += 1;
                    continue;
                }
            }

            if (json.contains("{")) {
                Matcher emptyMatcher = Pattern.compile(firstPartPattern + "\\s*\\{(\\s*)}").matcher(json);
                Matcher contentMatcher = Pattern.compile(firstPartPattern + "\\s*\\{(.+?})").matcher(json);
                boolean emptyMatcherFind = emptyMatcher.find();
                boolean contentMatcherFind = contentMatcher.find();
                if (emptyMatcherFind && !contentMatcherFind || (contentMatcherFind && emptyMatcherFind && emptyMatcher.start() <= contentMatcher.start())) {
                    if (parents.hasArray()) {
                        headList.add("element");
                        allContentList.add("");
                        hasArray.add(false);
                    } else {
                        if (emptyMatcher.group(1).matches("(#|@|)[^@#]+")) {
                            headList.add(emptyMatcher.group(1));
                            allContentList.add("");
                            hasArray.add(false);
                        }
                    }
                    hasObject.add(false);
                    numberAllegedItems += 1;
                    json = json.replaceFirst(Pattern.quote(emptyMatcher.group()), "");
                    continue;
                }
                if (contentMatcherFind){
                    json = json.replaceFirst(Pattern.quote(contentMatcher.group()), "");
                    int group = parents.hasArray() ? 1: 2;

                    StringBuilder contentBuilder = new StringBuilder("{" + contentMatcher.group(group));
                    long openBracketCount = contentBuilder.chars().parallel().filter(ch -> ch == '{').count();
                    long closeBracketCount = contentBuilder.chars().parallel().filter(ch -> ch == '}').count();
                    Matcher additionalMatcher = Pattern.compile(".*?},?").matcher(json);
                    while (openBracketCount > closeBracketCount && additionalMatcher.find()){
                        String additionalContent = additionalMatcher.group();
                        contentBuilder.append(additionalContent);
                        json = json.replaceFirst(Pattern.quote(additionalContent), "");
                        closeBracketCount = contentBuilder.chars().filter(ch -> ch == '}').count();
                        openBracketCount = contentBuilder.chars().filter(ch -> ch == '{').count();
                    }
                    contentBuilder.deleteCharAt(0);
                    json = json.replaceFirst(Pattern.quote(contentBuilder.toString()), "");
                    if (contentBuilder.charAt(contentBuilder.length()-1) == ',') {
                        contentBuilder.deleteCharAt(contentBuilder.length()-1);
                    }
                    if (contentBuilder.charAt(contentBuilder.length()-1) == '}') {
                        contentBuilder.deleteCharAt(contentBuilder.length()-1);
                    }
                    String content = contentBuilder.toString();
                    if (parents.hasArray()) {
                        headList.add("element");
                        allContentList.add(content);
                        hasArray.add(false);
                    } else {
                        if (contentMatcher.group(1).matches("(#|@|)[^@#]+")) {
                            headList.add(contentMatcher.group(1));
                            allContentList.add(content);
                            hasArray.add(false);
                        }
                    }
                    hasObject.add(true);
                    numberAllegedItems += 1;
                }
            }
        }

        boolean hasAttributes = false;
        if (headList.parallelStream().filter(s -> s.matches("@.+")).count() == allContentList.size() - 1 &&
            headList.parallelStream().filter(s -> s.matches("#" + parent)).count() == 1 &&
            allContentList.parallelStream().filter(s -> s.contains(":")).count() <= 1 &&
            headList.parallelStream().anyMatch(s -> s.contains("@"))) {
            hasAttributes = true;
        }

        for (int i = 0; i < allContentList.size(); i++) {
            String head = headList.get(i);
            if (head.charAt(0) == '@' && (hasArray.get(i) || hasObject.get(i))) {
                hasAttributes = false;
                break;
            }
        }

        if (allContentList.size() == 0) {
            parents.setValue("\"\"");
        } else if (hasAttributes) {
            int index = headList.indexOf("#" + parent);
            parents.setHasArray(hasArray.contains(true));
            String value = allContentList.get(index);
            allContentList.remove(index);
            headList.remove(index);
            if (!Pattern.compile("\".+?\"\\s*:.*").matcher(value).find()) {
                jsonValue(value, parents);
            }
            for (int i = 0; i < allContentList.size(); i++) {
                String attrVal = allContentList.get(i);
                if ("null".equals(attrVal)) {
                    attrVal = "\"\"";
                } else if (!attrVal.contains("\"")) {
                    attrVal = "\"" + attrVal + "\"";
                }
                parents.addAttribute(headList.get(i).substring(1), attrVal);
            }
            if (Pattern.compile("\".+?\"\\s*:.*").matcher(value).find()) {
                jsonValue(value, parents);
            }
        } else {
            int index = 0;
            String numberParent = "#" + parent;
            while (headList.contains(parent) && headList.contains(numberParent)) {
                int i = headList.indexOf(numberParent);
                allContentList.remove(i);
                headList.remove(i);
            }
            for (String s : new ArrayList<>(headList)) {
                String attr = "@" + s;
                while (headList.contains(s) && headList.contains(attr)) {
                    int i = headList.indexOf(attr);
                    allContentList.remove(i);
                    headList.remove(i);
                }
            }

            for(String content : allContentList){
                Node child;

                String head = headList.get(index);
                if (head.charAt(0) == '@' | head.charAt(0) == '#') {
                    head = head.substring(1);
                }
                if (!(numberAllegedItems == 1 && ("#" + parent).equals(headList.get(0)))) {
                    child = new Node();
                    parents.addChild(child);
                    child.setPath(parents.getPath() + " " + parents.getName());
                    child.setName(head);
                } else {
                    child = parents;
                }

                child.setHasArray(hasArray.get(index++));

                jsonValue(content, child);
            }
        }
    }

    public static void jsonValue(String value, Node parents) {
        if (Pattern.compile("\".+?\"\\s*:.*").matcher(value).find() || parents.hasArray()) {
            parseJSON(value, parents);
        } else {
            if (value.matches("\\s*")) {
                value = "\"\"";
            }
            if (!value.contains("\"") && !"null".equals(value)) {
                value = "\"" + value + "\"";
            }
            parents.setValue(value);
        }
    }

    public static Node parseXML(String xml){
        xml = xml.replaceAll("\\s{2,}|\t|\n", "");
        Node node = new Node();
        parseXML(xml, node);
        return node;
    }

    public static void parseXML(String xml, Node parent) {
        while (!xml.isEmpty()) {
            Matcher tagMatcher = Pattern.compile("<([^\\s<>/]+)(\\s?[^<>/]*)/?>").matcher(xml);
            if(tagMatcher.find()) {
                Node child = new Node();
                parent.addChild(child);
                String tag = tagMatcher.group(1);
                child.setName(tag);
                child.setPath(parent.getPath() + " " + parent.getName());
                String attributes = tagMatcher.group(2);
                Pattern valuePattern = Pattern.compile("<"+tag+attributes+">(.*?)</"+tag+">");
                Pattern endPattern = Pattern.compile("<"+tag+attributes+"/>");
                Matcher valueMatcher = valuePattern.matcher(xml);
                Matcher endMatcher = endPattern.matcher(xml);
                boolean valueFind = valueMatcher.find();
                int valueStart = valueFind ? valueMatcher.start(): Integer.MAX_VALUE;
                int endStart = endMatcher.find() ? endMatcher.start(): Integer.MAX_VALUE;
                if(valueFind && valueStart < endStart) {
                    String s = valueMatcher.group();

                    StringBuilder contentBuilder = new StringBuilder(s);
                    Pattern openPattern = Pattern.compile("<" + tag + ".*?>");
                    Pattern closePattern = Pattern.compile("</" + tag + ">");
                    Matcher open = openPattern.matcher(contentBuilder);
                    Matcher close = closePattern.matcher(contentBuilder);
                    int openCount = 0;
                    int closeCount = 0;
                    while (open.find()) openCount++;
                    while (close.find()) closeCount++;
                    xml = xml.replaceFirst(contentBuilder.toString(), "");

                    Pattern additionalValuePattern = Pattern.compile(".*?</" + tag + ">");
                    Matcher additionalValueMatcher = additionalValuePattern.matcher(xml);

                    while (additionalValueMatcher.find() && openCount > closeCount) {
                        contentBuilder.append(additionalValueMatcher.group());
                        open = openPattern.matcher(additionalValueMatcher.group());
                        close = closePattern.matcher(additionalValueMatcher.group());
                        while (open.find()) openCount++;
                        while (close.find()) closeCount++;
                        xml = xml.replaceFirst(Pattern.quote(additionalValueMatcher.group()), "");
                        additionalValueMatcher = additionalValuePattern.matcher(xml);
                    }

                    valuePattern = Pattern.compile("<"+tag+attributes+">(.*)</"+tag+">");
                    valueMatcher = valuePattern.matcher(contentBuilder);
                    if (valueMatcher.find()) {
                        s = valueMatcher.group(1);
                    }

                    if(s.contains("<")) {
                        parseXML(s, child);
                    } else {
                        child.setValue("\"" + s + "\"");
                    }
                } else {
                    child.setValue(null);
                    xml = xml.replaceFirst("<" + tag + attributes + "/>", "");
                }

                Pattern attributePattern = Pattern.compile("(\\w+)\\s*=\\s*([\"'][^\"]*[\"'])");
                Matcher attributeMatcher = attributePattern.matcher(attributes);
                boolean hasAttributes = attributeMatcher.find();
                if(hasAttributes){
                    while (hasAttributes){
                        String val = attributeMatcher.group(2);
                        if (val.charAt(0) == '\'') {
                            val = "\"" + val.substring(1, val.length()-1) + "\"";
                        }
                        child.addAttribute(attributeMatcher.group(1), val);
                        hasAttributes = attributeMatcher.find();
                    }
                }
            }
        }
    }

    /**
     * Read whole file in a String
     * @param fileName - file path
     * @return - file's content
     * @throws IOException - exception
     */

    public static String readFile(String fileName) throws IOException {
        return new String(Files.readAllBytes(Paths.get(fileName)));
    }
}
