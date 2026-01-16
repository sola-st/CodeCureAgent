// New class to handle FTP actions
class FTPAction {
    private FTPClient ftp;

    public FTPAction(FTPClient ftp) {
        this.ftp = ftp;
    }

    public void sendFile(String dir, String filename) {
        // Implementation of sending a file
    }

    public void getFile(String dir, String filename) {
        // Implementation of getting a file
    }

    public void delFile(String filename) {
        // Implementation of deleting a file
    }

    public void listFile(BufferedWriter bw, String filename) {
        // Implementation of listing a file
    }

    public void makeRemoteDir(String dir) {
        // Implementation of making remote directory
    }
}

// The FTP class now delegates responsibility to the new class
public class FTP extends Task {
    // ... (Other existing code and member variables remain unchanged)

    // Use the new class within the FTP methods
    private FTPAction ftpAction;

    public FTP() {
        // ... (Other existing constructor code remains unchanged)
        this.ftpAction = new FTPAction(ftp);
    }

    // Example method usage after refactoring
    protected void transferFiles(FTPClient ftp, FileSet fs)
            throws IOException, BuildException {
        // ... (Code before action on files)

        for (int i = 0; i < dsfiles.length; i++) {
            switch (action) {
                case SEND_FILES:
                    ftpAction.sendFile(dir, dsfiles[i]);
                    break;
                case GET_FILES:
                    ftpAction.getFile(dir, dsfiles[i]);
                    break;
                case DEL_FILES:
                    ftpAction.delFile(dsfiles[i]);
                    break;
                case LIST_FILES:
                    ftpAction.listFile(bw, dsfiles[i]);
                    break;
                default:
                    throw new BuildException("unknown ftp action " + action);
            }
        }

        // ... (Code after action on files)
    }

    protected void makeRemoteDir(FTPClient ftp, String dir)
            throws IOException, BuildException {
        ftpAction.makeRemoteDir(dir);
    }

    // ... (Other methods remain unchanged, but may also delegate to the new class)
}