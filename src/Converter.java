import java.util.Map;

public class Converter {

    public static String convert(String fileName) {
        Node node = Parser.parse(fileName);
        String ret;
        if ("XML".equals(node.getType())) {
            if ("".equals(node.getName()) && node.hasChildren()) {
                node = node.getChildren().get(0);
            }
            ret = "{" + NodeToJSON(node) + "\n}";
        } else {
            ret = NodeToXML(node);
        }
        return ret;
    }

    public static String XMLtoJSON(String fileName) {
        Node node = Parser.parse(fileName);
        if ("".equals(node.getName()) && node.hasChildren()) {
            node = node.getChildren().get(0);
        }
        return "{" + NodeToJSON(node) + "\n}";
    }

    public static String JSONtoXML(String fileName) {
        Node node = Parser.parse(fileName);
        return NodeToXML(node);
    }

    public static String NodeToJSON(Node node) {
        StringBuilder builder = new StringBuilder();

        boolean hasArray = node.checkOnArray();
        boolean parentHasArray = false;

        if (node.hasParent()) {
            parentHasArray = node.getParent().hasArray();
        }

        String tabs = "\t".repeat(node.getDepth());

        StringBuilder attributes = new StringBuilder();
        if (node.hasAttributes()) {
            for (Map.Entry<String, String> attr : node.getAttributes().entrySet()) {
                attributes.append("\n").append(tabs).append("\t\"@").append(attr.getKey()).append("\" : ").append(attr.getValue()).append(",");
            }
        }

        char open = hasArray ? '[': '{';
        char close = hasArray ? ']': '}';

        if (node.hasChildren()) {
            String name = parentHasArray && !node.hasAttributes() ? "": "\"" + (node.hasAttributes() ? "#": "") + node.getName() + "\" : ";
            String nameAttr = parentHasArray ? "": "\"" + node.getName() + "\" : ";
            if (node.hasAttributes()) {
                builder.append("\n").append(tabs)
                        .append(nameAttr).append('{')
                        .append(attributes);
            }
            builder.append("\n").append(tabs)
                    .append(node.hasAttributes() ? "\t": "").append(name).append(open);
            for (Node child : node.getChildren()) {
                if (node.hasChildren()) {
                    child.upDepth();
                }
                builder.append(NodeToJSON(child)).append(",");
            }
            builder.deleteCharAt(builder.length()-1);
            builder.append("\n").append(node.hasAttributes() ? "\t": "").append(tabs).append(close);
            if (node.hasAttributes()) {
                builder.append("\n").append(tabs).append("}");
            }
        } else {
            String nameAttr = "\"" + (node.hasAttributes() ? "#": "") + node.getName() + "\" : ";
            String name = parentHasArray ? "": "\"" + node.getName() + "\" : ";
            builder.append("\n").append(tabs);
            if (node.hasAttributes()) {
                builder.append(name).append("{")
                        .append(attributes.toString())
                        .append("\n").append(tabs).append("\t").append(nameAttr).append(node.getValue())
                        .append("\n").append(tabs).append("}");
            } else {
                builder.append(name).append(node.getValue());
            }
        }
        return builder.toString();
    }

    public static String NodeToXML(Node node) {
        StringBuilder builder = new StringBuilder();

        if (node.getChildren().size() > 1 && "".equals(node.getName())) {
            node.setName("root");
        } else if (node.hasChildren() && "".equals(node.getName())) {
            node = node.getChildren().get(0);
            node.downDepth();
        }

        StringBuilder attributes = new StringBuilder();
        if (node.hasAttributes()) {
            attributes.append(" ");
            for (Map.Entry<String, String> attr : node.getAttributes().entrySet()) {
                attributes.append(attr.getKey()).append(" = ").append(attr.getValue()).append(" ");
            }
            attributes.deleteCharAt(attributes.length()-1);
        }

        String tabs = "\t".repeat(node.getDepth());

        if (node.hasChildren()) {
            builder.append("\n").append(tabs).append("<")
                    .append(node.getName())
                    .append(attributes.toString())
                    .append(">");
            for (Node child : node.getChildren()) {
                builder.append(NodeToXML(child));
            }
            builder.append("\n").append(tabs).append("</").append(node.getName()).append(">");
        } else {
            if (node.hasValue() && !"null".equals(node.getValue())) {
                builder.append("\n").append(tabs).append("<").append(node.getName()).append(attributes.toString()).append(">")
                        .append(node.getValue(), 1, node.getValue().length()-1)
                        .append("</").append(node.getName()).append(">");
            } else {
                builder.append("\n").append(tabs).append("<")
                        .append(node.getName())
                        .append(attributes.toString())
                        .append(" />");
            }
        }
        return builder.toString();
    }
}
