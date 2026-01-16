package org.apache.tools.ant.taskdefs;

class JavadocArguments {
    private Commandline cmd;

    public JavadocArguments() {
        cmd = new Commandline();
    }

    protected void addArgIf(boolean b, String arg) {
        if (b) {
            cmd.createArgument().setValue(arg);
        }
    }

    protected void addArgIfNotEmpty(String key, String value) {
        if (value != null && value.length() != 0) {
            cmd.createArgument().setValue(key);
            cmd.createArgument().setValue(value);
        } else {
            log("Warning: Leaving out empty argument '" + key + "'",
                Project.MSG_WARN);
        }
    }

    public Commandline.Argument createArg() {
        return cmd.createArgument();
    }

    public void setAdditionalparam(String add) {
        cmd.createArgument().setLine(add);
    }

    public void setMaxmemory(String max) {
        cmd.createArgument().setValue("-J-Xmx" + max);
    }
}