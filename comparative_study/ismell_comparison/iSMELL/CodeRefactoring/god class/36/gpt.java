class MenuBarInitializer {
    private int menuShortcut;
    private GenericArgoMenuBar menuBar;

    public MenuBarInitializer(GenericArgoMenuBar menuBar, int menuShortcut) {
        this.menuBar = menuBar;
        this.menuShortcut = menuShortcut;
    }

    public void initMenus() {
        initMenuFile();
        initMenuEdit();
        initMenuView();
        initMenuCreate();
        initMenuArrange();
        initMenuGeneration();
        initMenuCritique();
        initMenuTools();
        initMenuHelp();
    }

    private void initMenuFile() {
        JMenu file = new JMenu(menuBar.menuLocalize("File"));
        menuBar.add(file);

        // Other menu items and actions
    }

    private void initMenuEdit() {
        JMenu edit = new JMenu(menuBar.menuLocalize("Edit"));
        menuBar.add(edit);

        // Other menu items and actions
    }

    private void initMenuView() {
        JMenu view = new JMenu(menuBar.menuLocalize("View"));
        menuBar.add(view);

        // Other menu items and actions
    }

    private void initMenuCreate() {
        JMenu create = new JMenu(menuBar.menuLocalize("Create"));
        menuBar.add(create);

        // Other menu items and actions
    }

    private void initMenuArrange() {
        JMenu arrange = new JMenu(menuBar.menuLocalize("Arrange"));
        menuBar.add(arrange);

        // Other menu items and actions
    }

    private void initMenuGeneration() {
        JMenu generate = new JMenu(menuBar.menuLocalize("Generation"));
        menuBar.add(generate);

        // Other menu items and actions
    }

    private void initMenuCritique() {
        JMenu critique = new JMenu(menuBar.menuLocalize("Critique"));
        menuBar.add(critique);

        // Other menu items and actions
    }

    private void initMenuTools() {
        JMenu tools = new JMenu(menuBar.menuLocalize("Tools"));
        menuBar.add(tools);

        // Other menu items and actions
    }

    private void initMenuHelp() {
        JMenu help = new JMenu(menuBar.menuLocalize("Help"));
        menuBar.add(help);

        // Other menu items and actions
    }
}