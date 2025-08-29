package amidst.gui.export;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.EAST;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.SOUTH;
import static java.awt.GridBagConstraints.SOUTHEAST;
import static java.awt.GridBagConstraints.SOUTHWEST;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import amidst.documentation.NotThreadSafe;
import amidst.gui.main.Actions;
import amidst.gui.main.PNGFileFilter;
import amidst.gui.main.menu.AmidstMenu;
import amidst.gui.main.viewer.FragmentGraphToScreenTranslator;
import amidst.gui.main.viewer.widget.ProgressWidget.ProgressEntryType;
import amidst.logging.AmidstLogger;
import amidst.logging.AmidstMessageBox;
import amidst.mojangapi.minecraftinterface.MinecraftInterfaceException;
import amidst.mojangapi.world.World;
import amidst.mojangapi.world.WorldOptions;
import amidst.mojangapi.world.biome.Biome;
import amidst.mojangapi.world.biome.UnknownBiomeIdException;
import amidst.mojangapi.world.coordinates.CoordinatesInWorld;
import amidst.mojangapi.world.oracle.BiomeDataOracle;
import amidst.settings.Setting;
import amidst.settings.biomeprofile.BiomeProfileSelection;
import amidst.util.SwingUtils;

@NotThreadSafe
public class BiomeExporterDialog {
	private static final int PREVIEW_SIZE = 100;
	private static final ExecutorService previewUpdater = Executors.newSingleThreadExecutor(r -> new Thread(r, "BiomePreviewUpdater"));

	private final Setting<String> lastBiomeExportPath;
	private final BiomeExporter biomeExporter;
	private final Frame parentFrame;
	private final Supplier<AmidstMenu> menuBarSupplier;
	private final BiomeProfileSelection biomeProfileSelection;
	private final GridBagConstraints constraints;
	private final GridBagConstraints labelPaneConstraints;
	private final JSpinner leftSpinner, topSpinner, rightSpinner, bottomSpinner;
	private final JCheckBox fullResCheckBox;
	private final JTextField pathField;
	private final JButton browseButton;
	private final JButton exportButton;
	private final BufferedImage previewImage;
	private final ImageIcon previewIcon;
	private final JLabel previewLabel;
	private final JDialog dialog;

	private WorldOptions worldOptions;
	private BiomeDataOracle biomeDataOracle;
	private Consumer<Entry<ProgressEntryType, Integer>> progressListener;

	public BiomeExporterDialog(BiomeExporter biomeExporter, Frame parentFrame, BiomeProfileSelection biomeProfileSelection,
			Supplier<AmidstMenu> menuBarSupplier, Setting<String> lastBiomeExportPath) {
		// @formatter:off
		this.lastBiomeExportPath   = lastBiomeExportPath;
		this.biomeExporter         = biomeExporter;
		this.parentFrame           = parentFrame;
		this.menuBarSupplier       = menuBarSupplier;
		this.biomeProfileSelection = biomeProfileSelection;
		this.constraints           = new GridBagConstraints();
		this.labelPaneConstraints  = new GridBagConstraints();

		this.leftSpinner           = createCoordinateSpinner();
		this.topSpinner            = createCoordinateSpinner();
		this.rightSpinner          = createCoordinateSpinner();
		this.bottomSpinner         = createCoordinateSpinner();
		this.fullResCheckBox       = createFullResCheckbox();
		this.pathField             = createPathField();
		this.browseButton          = createBrowseButton();
		this.exportButton          = createExportButton();
		this.previewImage          = new BufferedImage(PREVIEW_SIZE, PREVIEW_SIZE, BufferedImage.TYPE_INT_ARGB);
		this.previewIcon           = new ImageIcon(new BufferedImage(PREVIEW_SIZE * 2, PREVIEW_SIZE * 2, BufferedImage.TYPE_INT_ARGB));
		this.previewLabel          = createPreviewLabel();
		this.dialog                = createDialog();
		// @formatter:on
	}

	private JCheckBox createFullResCheckbox() {
		JCheckBox newCheckBox = new JCheckBox("Full Resolution");
		newCheckBox.addChangeListener(e -> {
			renderPreview();
		});
		return newCheckBox;
	}

	private JTextField createPathField() {
		JTextField newTextField = new JTextField();
		newTextField.setPreferredSize(new JTextField(String.join("", Collections.nCopies(50, "_"))).getPreferredSize());
		return newTextField;
	}

	private JSpinner createCoordinateSpinner() {
		JSpinner newSpinner = new JSpinner(new SpinnerNumberModel(0, -30000000, 30000000, 25));
		newSpinner.addChangeListener(e -> {
			renderPreview();
		});
		return newSpinner;
	}

	private JLabel createPreviewLabel() {
		JLabel newLabel = new JLabel();

		newLabel.setIcon(previewIcon);
		newLabel.setBorder(new LineBorder(Color.BLACK, 2));
		return newLabel;
	}

	private JButton createExportButton() {
	private static final String MENU_ITEM_EXPORT_BIOMES = "Export Biomes to Image ...";
	private static final String MENU_ITEM_BIOME_PROFILE = "Biome Profile";
	private static final String ERROR_TITLE = "Error";
	private static final String MESSAGE_UNABLE_TO_EXPORT_PATH = "Unable to export to path";
	private static final String MESSAGE_REASON_NOT_A_FILE = "Reason: Not a file";
	private static final String MESSAGE_REASON_NO_WRITING_PERMISSIONS = "Reason: No writing permissions";
	private static final String MESSAGE_REASON_INVALID_PATH = "Reason: Invalid path given";
	private static final String MESSAGE_REASON_ERROR_CREATING_DIRECTORIES = "Reason: Error creating directories";
	private static final String MESSAGE_FILE_ALREADY_EXISTS = "File already exists. Do you want to replace it?\n";

	// ... rest of your code ...

		JButton exportButton = new JButton("Export");
		exportButton.addActionListener((e) -> {
			try {
				topSpinner.commitEdit();
				leftSpinner.commitEdit();
				bottomSpinner.commitEdit();
				rightSpinner.commitEdit();
			} catch (ParseException e1) {
				// resets itself to previous value
			}

			CoordinatesInWorld topLeft = getTopLeftCoordinates();
			CoordinatesInWorld bottomRight = getBottomRightCoordinates();
			if (verifyImageCoordinates(topLeft, bottomRight) && verifyPathString(pathField.getText())) {
				Path path = Paths.get(pathField.getText());
				lastBiomeExportPath.set(path.toAbsolutePath().getParent().toString());
				biomeExporter.export(
						biomeDataOracle,
						new BiomeExporterConfiguration(
								path,
								!fullResCheckBox.isSelected(),
								topLeft,
								bottomRight,
								biomeProfileSelection
							),
						progressListener,
						menuBarSupplier.get()
					);
				dialog.dispose();
			}
		});
		return exportButton;
	}

	private boolean verifyPathString(String path) {
		try {
			Path p = Paths.get(path);
			Files.createDirectories(p.getParent());
			boolean fileExists = Files.exists(p);
			if (fileExists && !Files.isRegularFile(p)) {
				String message = MESSAGE_UNABLE_TO_EXPORT_PATH + ": " + p.toString() + "\n" + MESSAGE_REASON_NOT_A_FILE;
				AmidstLogger.warn(message);
				AmidstMessageBox.displayError(dialog, ERROR_TITLE, message);
			} else if (!Actions.canWriteToFile(p)) {
				String message = MESSAGE_UNABLE_TO_EXPORT_PATH + ": " + p.toString() + "\n" + MESSAGE_REASON_NO_WRITING_PERMISSIONS;
				AmidstLogger.warn(message);
				AmidstMessageBox.displayError(dialog, ERROR_TITLE, message);
			} else if (!fileExists || AmidstMessageBox.askToConfirmYesNo(dialog, "Replace file?",
					MESSAGE_FILE_ALREADY_EXISTS + p.toString())) {
				return true;
			}
			return false;
		} catch (InvalidPathException e) {
			String message = MESSAGE_UNABLE_TO_EXPORT_PATH + "\n" + MESSAGE_REASON_INVALID_PATH;
			AmidstLogger.warn(message);
			AmidstMessageBox.displayError(dialog, ERROR_TITLE, message);
		} catch (IOException e) {
			String message = MESSAGE_UNABLE_TO_EXPORT_PATH + "\n" + MESSAGE_REASON_ERROR_CREATING_DIRECTORIES;
			AmidstLogger.warn(message);
			AmidstMessageBox.displayError(dialog, ERROR_TITLE, message);
		}
		return false;
	}

	public boolean verifyImageCoordinates(CoordinatesInWorld topLeft, CoordinatesInWorld bottomRight) {
		if((topLeft != null && bottomRight != null) &&
		   (topLeft.getX() >= bottomRight.getX() || topLeft.getY() >= bottomRight.getY())) {
			String message = "Unable to create image: Invalid image coordinates detected.";
			AmidstLogger.warn(message);
			AmidstMessageBox.displayError(dialog, ERROR_TITLE, message);
			return false;
		} else {
			return true;
		}
	}

	private JButton createBrowseButton() {
		JButton newButton = new JButton("Browse...");
		newButton.addActionListener(e -> {
			Path exportPath = getExportPath();
			if (exportPath != null) {
				pathField.setText(exportPath.toAbsolutePath().toString());
			}
		});
		return newButton;
	}

	// ... rest of your code ...

	private JDialog createDialog() {
		// ... your existing code ...

		newDialog.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				/*
				 * This executes only when it's closed with the x button, alt f4, etc.
				 * When this happens we know that the user did not press the ok button
				 * to continue, so we re enable the export biomes menu button.
				 */
				menuBarSupplier.get().setMenuItemsEnabled(new String[] { MENU_ITEM_EXPORT_BIOMES, MENU_ITEM_BIOME_PROFILE }, true);
				newDialog.dispose();
			}
		});

		// ... your existing code ...
	}

	public void createAndShow(World world, FragmentGraphToScreenTranslator translator,
			Consumer<Entry<ProgressEntryType, Integer>> progressListener) {

		menuBarSupplier.get().setMenuItemsEnabled(new String[] { MENU_ITEM_EXPORT_BIOMES, MENU_ITEM_BIOME_PROFILE }, false);

		// ... rest of your code ...
	}

	public void dispose() {
		menuBarSupplier.get().setMenuItemsEnabled(new String[] { MENU_ITEM_EXPORT_BIOMES, MENU_ITEM_BIOME_PROFILE }, true);
		SwingUtils.destroyComponentTree(dialog);
	}
	
	public void softDispose() {
		menuBarSupplier.get().setMenuItemsEnabled(new String[] { MENU_ITEM_EXPORT_BIOMES, MENU_ITEM_BIOME_PROFILE }, true);
		dialog.dispose();
	}

	// ... rest of your code ...
