package parser;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @author i-Taozi
 */
@Getter
@Setter
public class POM {

    private String raw;
    private String groupId;
    private String artifactId;
    private String version;
    private String path;  //绝对路径
    private String relativePath; //相对路径
    private int relativeLens;

    private String packaging;

    //存储父POM节点
    private POM parent;
    private String parentGroupId;
    private String parentArtifactId;
    private String parentVersion;


    private HashMap<String, String> rawProperties = new HashMap<>();

    private HashMap<String, String> properties;

    private HashMap<String, String> testConfigurations = new HashMap<>();

    private ArrayList<Dependency> dependencies = new ArrayList<>();

    private ArrayList<String> modules = new ArrayList<>();

    private ArrayList<String> repositoryUrls = new ArrayList<>();

    //存储子POM节点，POM之间的关系分聚合跟继承，所以存两棵子树, 聚合树自顶向下建立，继承树自底向上建立
    @Getter
    private List<POM> aggregatorPoms = new ArrayList<>();
    @Getter
    private List<POM> childrenPoms = new ArrayList<>();

    public void addAggregatorPom(POM aggregatorPom) {
        aggregatorPom.setParent(this);
        this.aggregatorPoms.add(aggregatorPom);
    }

    public void addChildPom(POM child) {
        child.setParent(this);
        this.childrenPoms.add(child);
    }

    public void addRepositoryUrl(String url) {
        if (!this.repositoryUrls.contains(url))
            this.repositoryUrls.add(url);
    }

    public void addProperty(String name, String value) {
        this.rawProperties.put(name, value);
    }

    public String getRawProperty(String name) {
        return this.rawProperties.get(name);
    }

    public void addTestConfigurations(String name, String value) {
        testConfigurations.put(name,value);
    }

    public void addDependency(Dependency dep) {
        dependencies.add(dep);
    }

    public HashMap<String, String> getProperties() {
        if (this.properties != null)
            return this.properties;

        this.properties = new LinkedHashMap<>();
        for (Map.Entry<String, String> item : this.rawProperties.entrySet()) {
            if (item.getValue().contains("${")) {

            } else {
                this.properties.put(item.getKey(), item.getValue());
            }
        }
        return this.rawProperties;
    }

    //如果字符串包含$符号，进行插值替换
    public static String interpolateString(String s, POM pom) {
        if (!s.contains("$"))
            return s;
        Pattern pattern = Pattern.compile("\\$\\{(.*?)}");
        Matcher matcher = pattern.matcher(s);
        while (matcher.find()) {
            String value = findPropertyValue(matcher.group(1).trim(), pom);
            if (value != null) {
                s = s.replace(matcher.group(0), value);
            }
        }
        return s;
    }

    public String getProperty(String name) {
        if (this.properties == null)
            getProperties();
        return this.properties.get(name);
    }

    private static String findPropertyValue(String s, POM pom) {
        //获取${xxx}变量的真实值，并且这个值已经确定不在该POM的property里面，那么应该在父POM里面
        String value = null;
        while (true) {
            value = pom.getRawProperty(s);
//            while (value != null && value.contains("${")) {
//                String tempValue = pom.getRawProperty(value);
//                if (tempValue == null) {
//                    break;
//                } else {
//                    value = tempValue;
//                }
//            }
            if ((value != null && !value.contains("${")) || !pom.hasParent()) {
                break;
            }
            pom = pom.getParent();
        }
        return value;
    }

    public ArrayList<Dependency> getRawDependencies() {
        return dependencies;
    }

    public ArrayList<Dependency> getDependencies() {
        ArrayList<Dependency> deps = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\$\\{(.*?)}");
        //遍历父节点得到所有property定义
        for (Dependency dep : this.dependencies) {
            // resolve version if missing
            if (!dep.getVersion().equals("")) {
                if (dep.getVersion().contains("$")) {
                    String version = dep.getVersion();
                    version = interpolateString(version, this);
                    Dependency dependency = new Dependency(dep.getGroupID(), dep.getArtifactID(), version);
                    deps.add(dependency);
                } else {
                    deps.add(dep);
                }
            }
        }
        return deps;
    }

    public void addModule(String module) {
        modules.add(module);
    }

    public Boolean hasParent() {
//        return parent != null
//                && parent.getGroupId() != null
//                && parent.getArtifactId() != null
//                && parent.getVersion() != null;
        return parent != null
                && !parent.getGroupId().equals("")
                && !parent.getArtifactId().equals("")
                && !parent.getVersion().equals("");
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "POM{" +
                "groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        POM pom = (POM) o;
        //不比较version
        return ( Objects.equals(groupId, pom.groupId) && Objects.equals(artifactId, pom.artifactId) );
//        return Objects.equals(groupId, pom.groupId) &&
//                Objects.equals(artifactId, pom.artifactId) &&
//                Objects.equals(version, pom.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }
}
