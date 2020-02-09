import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Node {

    private Map<String, String> attributes = new LinkedHashMap<>();
    private List<Node> children = new ArrayList<>();
    private String value;
    private String path;
    private String name;
    private int depth;
    private String type;
    private boolean hasArray;
    private Node parent;

    public Node(){
        path = "";
        name = "";
        value = "";
        depth = 0;
        type = "XML";
        hasArray = false;
    }

    public void addAttribute(String attributeName, String attributeValue) {
        attributes.put(attributeName, attributeValue);
    }

    public void addChild(Node child) {
        child.depth = depth + 1;
        children.add(child);
        child.parent = this;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setHasArray(boolean hasArray) {
        this.hasArray = hasArray;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public List<Node> getChildren() {
        return children;
    }

    public String getValue() {
        return value;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public int getDepth() {
        return depth;
    }

    public Node getParent() {
        return parent;
    }

    public boolean hasAttributes() {
        return attributes.size() > 0;
    }

    public boolean hasChildren(){
        return children.size() > 0;
    }

    public boolean hasArray() {
        return hasArray;
    }

    public boolean hasParent() {
        return parent != null && !"".equals(parent.name);
    }

    public boolean hasValue() {
        return value != null;
    }

    public void downDepth() {
        depth -= 1;
        children.forEach(Node::downDepth);
    }

    public void upDepth() {
        depth += 1;
        children.forEach(Node::upDepth);
    }

    public boolean checkOnArray() {
        if (children.size() > 1) {
            String name = children.get(0).getName();
            for (Node node : children) {
                if (!name.equals(node.getName())) {
                    hasArray = false;
                    return false;
                }
            }
            hasArray = true;
            return true;
        }
        hasArray = false;
        return false;
    }

    public static void outputTree(Node node) {
        for(Node child : node.getChildren()) {
            System.out.println("\nElement:");
            System.out.println("path = " + (child.path + " " + child.name).trim().replaceAll(" ", ", "));
            if (child.children.size() > 0) {
                if (child.attributes.size() > 0) {
                    System.out.println("attributes:");
                    for (Map.Entry<String, String> attr : child.attributes.entrySet()) {
                        System.out.println(attr.getKey() + " = " + attr.getValue());
                    }
                }
                outputTree(child);
            } else {
                System.out.println("value = " + child.value);
                if (child.attributes.size() > 0) {
                    System.out.println("attributes:");
                    for (Map.Entry<String, String> attr : child.attributes.entrySet()) {
                        System.out.println(attr.getKey() + " = " + attr.getValue());
                    }
                }
            }
        }
    }
}
