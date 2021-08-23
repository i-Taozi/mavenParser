package parser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;

public class POMParser {

    private POM pom;
    private Document doc;
    private XPath xpath;

    /**
     * Parse the content of a pom.xml file.
     * @param path The path of a pom.xml file.
     * @return The POM instance.
     */
    public POM parse(String path) throws Exception {
        this.pom = new POM();
        String content = turnDocumentToString(path);
        this.pom.setRaw(content);
        this.pom.setPath(path);
        if (!content.equals("")) {
            this.doc = getDocument(content);
            XPathFactory xPathfactory = XPathFactory.newInstance();
            this.xpath = xPathfactory.newXPath();
            addPackaging(); // 设置POM打包类型，聚合模块为pom
            addProjectAttributes();//设置POM的三个坐标参数
            addParentPOMInfo();
            addProperties();
            addModules();
            addDependencies();
            addTestConfigurations();
            addRepositoryUrl();
        }
        return this.pom;
    }

    private Document getDocument(String content) throws Exception{
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(content));
        return builder.parse(inputSource);
    }

    private void addPackaging() throws XPathExpressionException {
        String packaging = xpath.evaluate("/project/packaging", doc);
        pom.setPackaging(packaging);
        //System.out.println(s.equals(""));如果不存在这个属性的话，是个空值
    }

    private void addRepositoryUrl() throws Exception {
        for (Node node : getNodes("/project/repositories/repository/url")) {
            String value = node.getTextContent();
            if (!value.endsWith("/"))
                value = value + "/";
            this.pom.addRepositoryUrl(value);
        }
        this.pom.addRepositoryUrl("https://repo1.maven.org/maven2/");
    }

    private String getProperty(String name) throws Exception {
        String value;
        // A dot (.) notated path in the POM
        String path1 = "/" + name.replace('.', '/');
        if (name.equals("parent.version")){
            path1 =  "/project/parent/version";
        }
        String path1Value = getValue(path1);

        // Set within a <properties /> element in the POM
        String path2 = "/project/properties/" + name;
        String path2Value = getValue(path2);

        if (!path1Value.equals("")) {
            value = path1Value;
        }else if (!path2Value.equals("")) {
            value = path2Value;
        }else {
            value = name;
        }
        return value;
    }

    private void addProjectAttributes() throws Exception {
        String groupId = getValue("/project/groupId");
        if(groupId.equals("")||groupId==null){
            groupId = getValue("/project/parent/groupId");
        }
        pom.setGroupId(groupId);

        String artifactId = getValue("/project/artifactId");
        if(artifactId.equals("")||artifactId==null){
            artifactId =  getValue("/project/parent/artifactId");
        }
        pom.setArtifactId(artifactId);

        String version = getValue("/project/version");
        if(version.equals("")||version==null){
            version =  getValue("/project/parent/version");
        }
        pom.setVersion(version);
    }


    private void addParentPOMInfo() throws Exception {
        String groupId = getValue("/project/parent/groupId");
        pom.setParentGroupId(groupId);
        String artifactId = getValue("/project/parent/artifactId");
        pom.setParentArtifactId(artifactId);
        String version = getValue("/project/parent/version");
        pom.setParentVersion(version);
    }

    private void addModules() throws Exception {
        for (Node node : getNodes("/project/modules/module")) {
            String value = node.getTextContent();
            pom.addModule(value);
        }
    }

    private void addProperties() throws Exception{
        for (Node node : getNodes("/project/properties/*")) {
            String name = node.getNodeName();
            String value = node.getTextContent();
            pom.addProperty(name, value);
        }
        /*可能会出现这种情况，即一个property的value值，需要从另外一个property里面找
         * <cuda.version>9.1</cuda.version>
         * <nd4j.backend>nd4j-cuda-${cuda.version}</nd4j.backend>
         */
/*        for (Node node : getNodes("/project/properties/*")) {
            String name = node.getNodeName();
            String value = node.getTextContent();
            if (value.matches("(.*?)\\$\\{(.+?)}([^$]*)")){
                value = replaceProperties(value);
                pom.addProperty(name, value);
            }
        }*/
    }



    private void addTestConfigurations() throws Exception {
        Node surefireNode = null;
        for (Node node : getNodes("//plugin")) {
            String artifactId = this.xpath.evaluate("artifactId", node);
            if (artifactId.equals("maven-surefire-plugin")) {
                surefireNode = node;
                break;
            }
        }
        if (surefireNode == null)
            return;
        Node configurationNode = (Node) this.xpath.evaluate("configuration", surefireNode, XPathConstants.NODE);
        NodeList configurationChildNodes = configurationNode.getChildNodes();
        for (int j = 0; j < configurationChildNodes.getLength(); j++) {
            //conItem是<configuration>的某个子结点
            Node conItem = configurationChildNodes.item(j);
            //如果conItem为ELEMENT_NODE，直接保存
            if (Node.ELEMENT_NODE == conItem.getNodeType() && !conItem.getTextContent().trim().equals("")) {
                System.out.println(conItem.getNodeName());
                System.out.println(conItem.getTextContent());
                this.pom.addTestConfigurations(conItem.getNodeName(), conItem.getTextContent().trim());
            }
        }
    }

    private void addDependencies() throws Exception{
        for (Node node : getNodes(
                "/project/dependencies/dependency|/project/dependencyManagement/dependencies/dependency")) {
            NodeList childNodes = node.getChildNodes();

            String groupID = "";
            String artificatID = "";
            String version = "";
            String versionContent = "";
            for (int i=0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                if (Node.ELEMENT_NODE == childNode.getNodeType()) {

                    String tag = childNode.getNodeName();

                    if (tag.equals("groupId")) {
                        groupID = childNode.getTextContent();
/*                        if (groupID.contains("project.groupId")){
                            groupID = pom.getGroupId();
                        }
                        groupID = replaceProperties(groupID);*/

                    } else if (tag.equals("artifactId")) {
                        artificatID = childNode.getTextContent();
/*                        if (artificatID.contains("project.artifactId")){
                            artificatID = pom.getArtifactId();
                        }
                        artificatID = replaceProperties(artificatID);*/
                    } else if (tag.equals("version")) {
                        version = childNode.getTextContent();
/*                        if (version.contains("project.version")){
                            version = pom.getVersion();
                        }
                        version = replaceProperties(version);*/
                    }
                }
            }
//            VersionSpecifier versionSpecifier = new VersionSpecifierParser().parse(version);
//            versionSpecifier.setVersionContent(versionContent);

            Dependency dependency = new Dependency(groupID, artificatID, version);
            if (!groupID.contains("project.groupId") && !artificatID.contains("project.artifactId") && !version.contains("project.version") && !version.contains("project.parent.version"))
                pom.addDependency(dependency);
        }
    }


    private String turnDocumentToString(String path) {
        try {
            // 读取 xml 文件
            File fileinput = new File(path);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fileinput);

            DOMSource domSource = new DOMSource(doc);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);


            return writer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private ArrayList<Node> getNodes(String name) throws Exception {
        XPathExpression expr = xpath.compile(name);
        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        ArrayList<Node> result = new ArrayList<>();

        for (int i=0; i < nodes.getLength(); i++) {
            Node childNode = nodes.item(i);
            if (Node.ELEMENT_NODE == childNode.getNodeType()) {
                result.add(childNode);
            }
        }
        return result;
    }

    private String getValue(String name) throws Exception {
        //System.out.println(name);
        XPathExpression expr = xpath.compile(name);
        return expr.evaluate(doc);
    }
}
