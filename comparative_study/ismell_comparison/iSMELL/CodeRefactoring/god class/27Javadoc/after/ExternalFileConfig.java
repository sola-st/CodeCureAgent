package org.apache.tools.ant.taskdefs;

class ExternalFileConfig {
    private boolean useExternalFile;

    public ExternalFileConfig() {
        useExternalFile = false;
    }

    public void setUseExternalFile(boolean b) {
        useExternalFile = b;
    }
}