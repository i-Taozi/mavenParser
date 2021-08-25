package parser;

import lombok.Getter;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * @author i-Taozi
 */
public class POMTree {
    @Getter
    private ArrayList<POM> pomList = new ArrayList<>();

    private String repoDir;
    private String repoName;

    private POM rootPom = null;

    public POMTree(String repoDir) {
        this.repoDir = repoDir;
        this.repoName = Paths.get(repoDir).getFileName().toString();
    }

    public POM createPomTree() throws Exception {
        if (this.rootPom != null)
            return this.rootPom;

        String rootPomFilePath = Paths.get(this.repoDir, "pom.xml").normalize().toString();
        File file = new File(rootPomFilePath);
        if (!file.exists()) {
            throw new Exception("根目录pom文件" + rootPomFilePath + "不存在");
        }
        //生成聚合树
        this.rootPom = createAggregatorTree(rootPomFilePath);

        setPomList(this.rootPom, this.pomList);
        //生成继承树关系
        createInheritanceTree(this.rootPom, this.pomList);

        //setRelativePath(projectName);
        return rootPom;
    }

    public static POM createAggregatorTree(String pomFilePath) throws Exception {
        POMParser pomParser = new POMParser();
        //File file =
        POM rootPom = pomParser.parse(pomFilePath);
        if (rootPom.getPackaging().equals("pom") && rootPom.getModules().size() > 0) {
            for (String moduleName : rootPom.getModules()) {
                String modulePath = Paths.get(Paths.get(pomFilePath).getParent().toString(), moduleName, "pom.xml").toString();
                //System.out.println(modulePath);
                POM modulePom = createAggregatorTree(modulePath);
                rootPom.addAggregatorPom(modulePom);
            }
        }
        return rootPom;
    }

    public static void createInheritanceTree(POM pom, List<POM> pomList) {
        //对于每个pom，找到其父POM
        for (POM childPom : pomList) {
            //如果该POM有父POM
            if (childPom.hasParent()) {
                //去PomList里面找到这个父POM，并覆盖之前的Parent POM变量
                for (POM parentPom : pomList) {
                    if (parentPom.getGroupId().equals(childPom.getParentGroupId()) && parentPom.getArtifactId().equals(childPom.getParentArtifactId())) {
                        childPom.setParent(parentPom);
                        parentPom.addChildPom(childPom);
                    }
                }
            }
        }
    }


    public List<Dependency> getRawDependencies() {
        List<Dependency> list = new ArrayList<>();
        for (POM pom : pomList) {
            List<Dependency> dependencies = pom.getRawDependencies();
            for (Dependency dependency : dependencies) {
                if (!dependency.getVersion().equals("")) {
                    list.add(dependency);
                }
            }
        }
        return list;
    }

    public List<Dependency> getDependencies() {
        List<Dependency> list = new ArrayList<>();
        for (POM pom : pomList) {
            List<Dependency> dependencies = pom.getDependencies();
            list.addAll(dependencies);
        }
        return list;
    }

    public List<Dependency> getDynamicDependencies() {
        List<Dependency> dependencies = getDependencies();
        List<Dependency> list = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            String version = dependency.getVersion();
            if (version.equals("RELEASE") ||version.equals("LATEST") || version.endsWith("SNAPSHOT")) {
                list.add(dependency);
            }
        }
        return list;
    }

    public Map<String, List<Dependency>> getDependenciesMap() {
        Map<String, List<Dependency>> map = new LinkedHashMap<>();
        for (POM pom : this.pomList) {
            map.put(pom.getPath(), pom.getDependencies());
        }
        return map;
    }

    public static void setPomList(POM pom, List<POM> list){
        list.add(pom);
        if (pom.getAggregatorPoms().size() > 0) {
            for (POM aggregatorPom : pom.getAggregatorPoms()) {
                setPomList(aggregatorPom, list);
            }
        }
    }

    public List<String> getPathList() {
        List<String> list = new ArrayList<>();
        for (POM pom : pomList) {
            list.add(pom.getPath());
        }
        return list;
    }

    public List<String> getRepositoryUrls() {
        List<String> list = new ArrayList<>();
        for (POM pom : pomList) {
            list.addAll(pom.getRepositoryUrls());
        }
        return list.stream().distinct().collect(Collectors.toList());
    }
}
