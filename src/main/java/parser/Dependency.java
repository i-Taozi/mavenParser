package parser;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class Dependency {

    private String groupID;
    private String artifactID;
    private String version;

    public Dependency(String groupID, String artifactID, String version) {
        this.groupID = groupID;
        this.artifactID = artifactID;
        this.version = version;
    }

    public String getName() {
        return groupID + "/" + artifactID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return groupID.equals(that.groupID) && artifactID.equals(that.artifactID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupID,artifactID,version);
    }

    @Override
    public String toString() {
        return "groupID: " + groupID + " artifactID: " + artifactID + " version: " + version;
    }
}
