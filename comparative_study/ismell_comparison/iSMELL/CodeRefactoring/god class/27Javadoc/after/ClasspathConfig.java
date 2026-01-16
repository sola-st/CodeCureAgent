package org.apache.tools.ant.taskdefs;

import java.nio.file.Path;

import org.apache.tools.ant.types.Reference;

class ClasspathConfig {
    private Path classpath;

    public ClasspathConfig(Project project) {
        classpath = new Path(project);
    }

    public void setClasspath(Path path) {
        if (classpath == null) {
            classpath = path;
        } else {
            classpath.append(path);
        }
    }

    public Path createClasspath() {
        if (classpath == null) {
            classpath = new Path(getProject());
        }
        return classpath.createPath();
    }

    public void setClasspathRef(Reference r) {
        createClasspath().setRefid(r);
    }

    public void setBootclasspath(Path path) {
        if (bootclasspath == null) {
            bootclasspath = path;
        } else {
            bootclasspath.append(path);
        }
    }

    public Path createBootclasspath() {
        if (bootclasspath == null) {
            bootclasspath = new Path(getProject());
        }
        return bootclasspath.createPath();
    }

    public void setBootClasspathRef(Reference r) {
        createBootclasspath().setRefid(r);
    }

    public void setExtdirs(String path) {
        cmd.createArgument().setValue("-extdirs");
        cmd.createArgument().setValue(path);
    }

    public void setExtdirs(Path path) {
        cmd.createArgument().setValue("-extdirs");
        cmd.createArgument().setPath(path);
    }
}