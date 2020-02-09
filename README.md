# XML-JSON-converter
Use static method "convert(String fileName)" of Converter class to convert XML to JSON and vice versa.
There are some another functions that can help you like nodeToJson(Node node) and nodeToXML(Node node).
Node is the adapter structure into which JSON and XML are parsed to.
Converter supports arrays and attributes in JSON(And XML of course).
JSON: 
{
  "data" : {
    "@attr" : "attrVal",
    "#data" : "someVal"
  }
}
XML:
<data attr = "attrVal">someVal</data>
