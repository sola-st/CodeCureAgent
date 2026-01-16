package org.apache.tools.ant.taskdefs;

import java.nio.file.Path;

import org.apache.tools.ant.taskdefs.Javadoc.DocletInfo;
import org.apache.tools.ant.taskdefs.Javadoc.ExtensionInfo;

class DocletConfig {
    private DocletInfo doclet;

    public DocletConfig(Project project) {
        doclet = new DocletInfo();
        doclet.setProject(project);
    }

    public void setDoclet(String docletName) {
        if (doclet == null) {
            doclet = new DocletInfo();
            doclet.setProject(getProject());
        }
        doclet.setName(docletName);
    }

    public void setDocletPath(Path docletPath) {
        if (doclet == null) {
            doclet = new DocletInfo();
            doclet.setProject(getProject());
        }
        doclet.setPath(docletPath);
    }

    public void setDocletPathRef(Reference r) {
        if (doclet == null) {
            doclet = new DocletInfo();
            doclet.setProject(getProject());
        }
        doclet.createPath().setRefid(r);
    }

    public DocletInfo createDoclet() {
        if (doclet == null) {
            doclet = new DocletInfo();
        }
        return doclet;
    }

    public void addTaglet(ExtensionInfo tagletInfo) {
        tags.addElement(tagletInfo);
    }
}