package CTLM.com.bnpp.pf.tools.codex.java.bean;

public class TechnicalComponent {

    private String groupId;
    private String artifactId;
    private String version;
    private String key;
    private String target; // TODO uniformiser avec type dans DependencyLight
    private String path;
    private String parent;

    public TechnicalComponent() {
    }

    public TechnicalComponent(String key, String groupId, String artifactId, String version, String parent,
            String target, String path) {
        super();
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.key = key;
        this.target = target;
        this.path = path;
        this.parent = parent;
    }

    public TechnicalComponent(DependencyLight dl, String path) {
        groupId = dl.getGroupId();
        artifactId = dl.getArtifactId();
        version = dl.getVersion();
        target = dl.getType();

    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String toString() {
        return key + ":" + version + "\t" + parent + "\t" + path;
    }

    public String getLastDirName() {
        String sep;
        if (this.path.contains("\\")) {
            sep = "\\";
        } else {
            sep = "/";
        }
        int lastIndex = path.lastIndexOf(sep);
        String toRtn = path.substring(lastIndex, path.length());
        return toRtn;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
        result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        System.out.println("Into equals");
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TechnicalComponent other = (TechnicalComponent) obj;
        if (artifactId == null) {
            if (other.artifactId != null)
                return false;
        } else if (!artifactId.equals(other.artifactId))
            return false;
        if (groupId == null) {
            if (other.groupId != null)
                return false;
        } else if (!groupId.equals(other.groupId))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

}