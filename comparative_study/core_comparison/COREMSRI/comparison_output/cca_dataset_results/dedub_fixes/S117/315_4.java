package gui;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextScrollPane;

import app.helpers.Config;
import model.JWTSuiteTabModel;
import model.Strings;

public class JWTSuiteTab extends JPanel {

	private static final long serialVersionUID = 1L;
	private JTextArea jwtInputField;
	private RSyntaxTextArea jwtOuputField;
	private JButton jwtSignatureButton;
	private JTextArea jwtKeyArea;
	private JLabel lblEnterSecret;
	private JWTSuiteTabModel jwtSTM;
	private JButton creditButton;
	private JButton configButton;
	private JLabel lbRegisteredClaims;
	private JLabel lblExtendedVerificationInfo;

	public JWTSuiteTab(JWTSuiteTabModel jwtSTM) {
		drawGui();
		this.jwtSTM = jwtSTM;
	}

	public void updateSetView() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (!jwtInputField.getText().equals(jwtSTM.getJwtInput())) {
					jwtInputField.setText(jwtSTM.getJwtInput());
				}
				if (!jwtSignatureButton.getText().equals(jwtSTM.getVerificationLabel())) {
					jwtSignatureButton.setText(jwtSTM.getVerificationLabel());
				}
				if (!jwtOuputField.getText().equals(jwtSTM.getJwtJSON())) {
					jwtOuputField.setText(jwtSTM.getJwtJSON());
				}
				if (!jwtKeyArea.getText().equals(jwtSTM.getJwtKey())) {
					jwtKeyArea.setText(jwtSTM.getJwtKey());
				}
				if (!jwtSignatureButton.getBackground().equals(jwtSTM.getJwtSignatureColor())) {
					jwtSignatureButton.setBackground(jwtSTM.getJwtSignatureColor());
				}
				if (jwtKeyArea.getText().equals("")) {
					jwtSTM.setJwtSignatureColor(new JButton().getBackground());
					jwtSignatureButton.setBackground(jwtSTM.getJwtSignatureColor());
				}
				lblExtendedVerificationInfo.setText(jwtSTM.getVerificationResult());
				lbRegisteredClaims.setText(jwtSTM.getTimeClaimsAsText());
				jwtOuputField.setCaretPosition(0);
			}
		});
	}

	public void registerDocumentListener(DocumentListener jwtInputListener, DocumentListener jwtKeyListener) {
		jwtInputField.getDocument().addDocumentListener(jwtInputListener);
		jwtKeyArea.getDocument().addDocumentListener(jwtKeyListener);
	}

	private void drawGui() {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 10, 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		setLayout(gridBagLayout);

		JLabel lblPasteJwtToken = new JLabel(Strings.enterJWT);
		lblPasteJwtToken.setFont(new Font("Tahoma", Font.BOLD, 12));
		GridBagConstraints gbcLblPasteJwtToken = new GridBagConstraints();
		gbcLblPasteJwtToken.anchor = GridBagConstraints.SOUTHWEST;
		gbcLblPasteJwtToken.insets = new Insets(0, 0, 5, 5);
		gbcLblPasteJwtToken.gridx = 1;
		gbcLblPasteJwtToken.gridy = 1;
		add(lblPasteJwtToken, gbcLblPasteJwtToken);

		creditButton = new JButton("About");
		creditButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				JLabelLink jLabelLink = new JLabelLink(Strings.creditTitle, 530, 555);

				jLabelLink.addText("<h2>About JWT4B</h2>JSON Web Tokens (also known as JWT4B) is developed by Oussama Zgheb and Matthias Vetsch.<br>");
				jLabelLink.addURL("<a href=\"https://zgheb.com/\">Mantainer Website</a>","zgheb.com");
				jLabelLink.addURL("<a href=\"https://github.com/mvetsch/JWT4B\">GitHub Repository</a>","github.com/mvetsch/JWT4B");
				jLabelLink.addText("<br><br>");
				jLabelLink.addText("JWT4B, excluding the libraries mentioned below and the Burp extender classes, uses the GPL 3 license.");
				jLabelLink.addURL("* <a href=\"https://github.com/bobbylight/RSyntaxTextArea/blob/master/src/main/dist/RSyntaxTextArea.License.txt\">RSyntaxTextArea</a>","github.com/bobbylight/RSyntaxTextArea");
				jLabelLink.addURL("* <a href=\"https://github.com/auth0/java-jwt/blob/master/LICENSE\">Auth0 -java-jwt</a>","github.com/auth0/java-jwt");
				jLabelLink.addURL("* <a href=\"https://www.apache.org/licenses/\">Apache Commons Lang</a>","apache.org");
				jLabelLink.addText("<br><br>");
				jLabelLink.addText("Thanks to Compass Security AG for providing development time for the initial version<br>");
				jLabelLink.addURL("<a href=\"https://www.compass-security.com\">compass-security.com</a><br>","compass-security.com");
				jLabelLink.addText("and to Brainloop for providing broader token support!");
				jLabelLink.addURL("<a href=\"https://www.brainloop.com\">brainloop.com</a><br><br>","brainloop.com");

				jLabelLink.addLogoImage();
			}
		});
		GridBagConstraints gbcCreditButton = new GridBagConstraints();
		gbcCreditButton.insets = new Insets(0, 0, 5, 0);
		gbcCreditButton.gridx = 2;
		gbcCreditButton.gridy = 1;
		gbcCreditButton.fill = GridBagConstraints.HORIZONTAL;
		add(creditButton, gbcCreditButton);
		
		
		configButton = new JButton("Change Config");
		configButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				File file = new File (Config.configPath);
				Desktop desktop = Desktop.getDesktop();
				try {
					desktop.open(file);
				} catch (IOException e) {
					System.err.println("Error using Desktop API - "+e.getMessage()+" - "+e.getCause());
				}
			}
		});

		GridBagConstraints gbcConfigButton = new GridBagConstraints();
		gbcConfigButton.insets = new Insets(0, 0, 5, 0);
		gbcConfigButton.gridx = 2;
		gbcConfigButton.gridy = 2;
		gbcConfigButton.fill = GridBagConstraints.HORIZONTAL;
		add(configButton, gbcConfigButton);

		jwtInputField = new JTextArea();
		jwtInputField.setRows(2);
		jwtInputField.setLineWrap(true);
		jwtInputField.setWrapStyleWord(true);
		
		GridBagConstraints gbcJwtInputField = new GridBagConstraints();
		gbcJwtInputField.insets = new Insets(0, 0, 5, 5);
		gbcJwtInputField.fill = GridBagConstraints.BOTH;
		gbcJwtInputField.gridx = 1;
		gbcJwtInputField.gridy = 2;
		add(jwtInputField, gbcJwtInputField);

		lblEnterSecret = new JLabel(Strings.enterSecretKey);
		lblEnterSecret.setFont(new Font("Tahoma", Font.BOLD, 12));
		GridBagConstraints gbcLblEnterSecret = new GridBagConstraints();
		gbcLblEnterSecret.anchor = GridBagConstraints.WEST;
		gbcLblEnterSecret.insets = new Insets(0, 0, 5, 5);
		gbcLblEnterSecret.gridx = 1;
		gbcLblEnterSecret.gridy = 3;
		add(lblEnterSecret, gbcLblEnterSecret);

		jwtKeyArea = new JTextArea();
		GridBagConstraints gbcJwtKeyField = new GridBagConstraints();
		gbcJwtKeyField.insets = new Insets(0, 0, 5, 5);
		gbcJwtKeyField.fill = GridBagConstraints.HORIZONTAL;
		gbcJwtKeyField.gridx = 1;
		gbcJwtKeyField.gridy = 4;
		add(jwtKeyArea, gbcJwtKeyField);
		jwtKeyArea.setColumns(10);

		jwtSignatureButton = new JButton("");
		Dimension preferredSize = new Dimension(400, 30);
		jwtSignatureButton.setPreferredSize(preferredSize);

		GridBagConstraints gbcJwtSignatureButton = new GridBagConstraints();
		gbcJwtSignatureButton.insets = new Insets(0, 0, 5, 5);
		gbcJwtSignatureButton.gridx = 1;
		gbcJwtSignatureButton.gridy = 6;
		add(jwtSignatureButton, gbcJwtSignatureButton);

		GridBagConstraints gbcJwtOuputField = new GridBagConstraints();
		gbcJwtOuputField.insets = new Insets(0, 0, 5, 5);
		gbcJwtOuputField.fill = GridBagConstraints.BOTH;
		gbcJwtOuputField.gridx = 1;
		gbcJwtOuputField.gridy = 9;

		jwtOuputField = new RSyntaxTextArea();
		SyntaxScheme scheme = jwtOuputField.getSyntaxScheme();
		Style style = new Style();
		style.foreground = new Color(222, 133, 10);
		scheme.setStyle(Token.LITERAL_STRING_DOUBLE_QUOTE, style);
		jwtOuputField.revalidate();
		jwtOuputField.setHighlightCurrentLine(false);
		jwtOuputField.setCurrentLineHighlightColor(Color.WHITE);
		jwtOuputField.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
		jwtOuputField.setEditable(false);
		// no context menu on right-click
		jwtOuputField.setPopupMenu(new JPopupMenu());
		
		// hopefully fixing:
		// java.lang.ClassCastException: class javax.swing.plaf.nimbus.DerivedColor$UIResource cannot be cast to class 
		// java.lang.Boolean (javax.swing.plaf.nimbus.DerivedColor$UIResource is in module java.desktop of loader 'bootstrap'; 
		// java.lang.Boolean is in module java.base of loader 'bootstrap')
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				RTextScrollPane sp = new RTextScrollPane(jwtOuputField);
				sp.setLineNumbersEnabled(false);
				add(sp, gbcJwtOuputField);
			}
		});

		lblExtendedVerificationInfo = new JLabel("");
		GridBagConstraints gbcLblExtendedVerificationInfo = new GridBagConstraints();
		gbcLblExtendedVerificationInfo.insets = new Insets(0, 0, 5, 5);
		gbcLblExtendedVerificationInfo.gridx = 1;
		gbcLblExtendedVerificationInfo.gridy = 7;
		add(lblExtendedVerificationInfo, gbcLblExtendedVerificationInfo);

		JLabel lblDecodedJwt = new JLabel(Strings.decodedJWT);
		lblDecodedJwt.setFont(new Font("Tahoma", Font.BOLD, 12));
		GridBagConstraints gbcLblDecodedJwt = new GridBagConstraints();
		gbcLblDecodedJwt.anchor = GridBagConstraints.WEST;
		gbcLblDecodedJwt.insets = new Insets(0, 0, 5, 5);
		gbcLblDecodedJwt.gridx = 1;
		gbcLblDecodedJwt.gridy = 8;
		add(lblDecodedJwt, gbcLblDecodedJwt);


		lbRegisteredClaims = new JLabel();
		lbRegisteredClaims.setBackground(new Color(238, 238, 238));
		GridBagConstraints gbcLbRegisteredClaims = new GridBagConstraints();
		gbcLbRegisteredClaims.fill = GridBagConstraints.BOTH;
		gbcLbRegisteredClaims.insets = new Insets(0, 0, 5, 5);
		gbcLbRegisteredClaims.gridx = 1;
		gbcLbRegisteredClaims.gridy = 11;
		add(lbRegisteredClaims, gbcLbRegisteredClaims);

	}

	public String getJWTInput() {
		return jwtInputField.getText();
	}

	public String getKeyInput() {
		return jwtKeyArea.getText();
	}
}