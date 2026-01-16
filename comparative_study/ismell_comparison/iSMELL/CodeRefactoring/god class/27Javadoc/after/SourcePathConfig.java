package org.apache.tools.ant.taskdefs;

import java.nio.file.Path;

class SourcePathConfig {
    private Path sourcePath;

    public SourcePathConfig(Project project) {
        sourcePath = new Path(project);
    }

    public void setSourcepath(Path src) {
        if (sourcePath == null) {
            sourcePath = src;
        } else {
            sourcePath.append(src);
        }
    }

    public Path createSourcepath() {
        if (sourcePath == null) {
            sourcePath = new Path(getProject());
        }
        return sourcePath.createPath();
    }

    public void setSourcepathRef(Reference r) {
        createSourcepath().setRefid(r);
    }
}