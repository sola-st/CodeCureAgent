```java
package gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Style;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextScrollPane;

import app.helpers.Config;
import model.Strings;
import model.JWTInterceptModel;

public class JWTInterceptTab extends JPanel {

	private static final long serialVersionUID = 1L;
	private JWTInterceptModel jwtIM;
	private RSyntaxTextArea jwtArea;
	private String jwtAreaOriginalContent = "none";
	private JRadioButton rdbtnRecalculateSignature;
	private JRadioButton rdbtnRandomKey;
	private JRadioButton rdbtnOriginalSignature;
	private JRadioButton rdbtnChooseSignature;

	private JTextArea jwtKeyArea;
	private JLabel lblSecretKey;
	private JSeparator separator;
	private JRadioButton rdbtnDontModifySignature;
	private JLabel lblProblem;
	private JComboBox<String> noneAttackComboBox;
	private JLabel lblNewLabel;
	private JLabel lblCookieFlags;
	private JLabel lbRegisteredClaims;
	private JCheckBox chkbxCveAttack;
	private JButton btnCopyPubPrivKeyCveAttack;

	public JWTInterceptTab(JWTInterceptModel jwtIM) {
		this.jwtIM = jwtIM;
		drawGui();
	}
	
	public void registerActionListeners(ActionListener dontModify, ActionListener randomKeyListener, ActionListener originalSignatureListener, ActionListener recalculateSignatureListener, ActionListener chooseSignatureListener, ActionListener algAttackListener, ActionListener cveAttackListener){
		rdbtnDontModifySignature.addActionListener(dontModify);
		rdbtnRecalculateSignature.addActionListener(randomKeyListener);
		rdbtnOriginalSignature.addActionListener(originalSignatureListener);
		rdbtnChooseSignature.addActionListener(chooseSignatureListener);
		rdbtnRandomKey.addActionListener(recalculateSignatureListener);
		noneAttackComboBox.addActionListener(algAttackListener);
		chkbxCveAttack.addActionListener(cveAttackListener);
	}
	
	private void drawGui() {	
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] {0, 0, 0, 0, 0};
		gridBagLayout.rowHeights = new int[]{10, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, 1.0, 0.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		jwtArea = new RSyntaxTextArea(20,60);
		jwtArea.setMinimumSize(new Dimension(300, 300));
		jwtArea.setColumns(90);
		SyntaxScheme scheme = jwtArea.getSyntaxScheme();
		Style style = new Style();
		style.foreground = new Color(222,133,10);
		scheme.setStyle(Token.LITERAL_STRING_DOUBLE_QUOTE, style);
		jwtArea.revalidate();
		jwtArea.setHighlightCurrentLine(false);
		jwtArea.setCurrentLineHighlightColor(Color.WHITE);
		jwtArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
		jwtArea.setEditable(true);
		jwtArea.setPopupMenu(new JPopupMenu()); 
		RTextScrollPane sp = new RTextScrollPane(jwtArea);
		sp.setLineNumbersEnabled(false);
		
		GridBagConstraints gbcJwtArea = new GridBagConstraints();
		gbcJwtArea.gridheight = 7;
		gbcJwtArea.gridwidth = 1;
		gbcJwtArea.insets = new Insets(0, 0, 5, 5);
		gbcJwtArea.fill = GridBagConstraints.BOTH;
		gbcJwtArea.gridx = 1;
		gbcJwtArea.gridy = 1;
		add(sp, gbcJwtArea);

		
		rdbtnDontModifySignature = new JRadioButton(Strings.dontModify);
		rdbtnDontModifySignature.setToolTipText(Strings.dontModifyToolTip);
		rdbtnDontModifySignature.setSelected(true);
		rdbtnDontModifySignature.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbcRdbtnDontModifySignature = new GridBagConstraints();
		gbcRdbtnDontModifySignature.anchor = GridBagConstraints.WEST;
		gbcRdbtnDontModifySignature.insets = new Insets(0, 0, 5, 5);
		gbcRdbtnDontModifySignature.gridx = 2;
		gbcRdbtnDontModifySignature.gridy = 1;
		add(rdbtnDontModifySignature, gbcRdbtnDontModifySignature);
		
		rdbtnRecalculateSignature = new JRadioButton(Strings.recalculateSignature);
		rdbtnRecalculateSignature.setToolTipText(Strings.recalculateSignatureToolTip);
		rdbtnRecalculateSignature.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbcRdbtnRecalculateSignature = new GridBagConstraints();
		gbcRdbtnRecalculateSignature.anchor = GridBagConstraints.WEST;
		gbcRdbtnRecalculateSignature.insets = new Insets(0, 0, 5, 5);
		gbcRdbtnRecalculateSignature.gridx = 2;
		gbcRdbtnRecalculateSignature.gridy = 2;
		add(rdbtnRecalculateSignature, gbcRdbtnRecalculateSignature);
		
		rdbtnOriginalSignature = new JRadioButton(Strings.keepOriginalSignature);
		rdbtnOriginalSignature.setToolTipText(Strings.keepOriginalSignatureToolTip);
		rdbtnOriginalSignature.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbcRdbtnOriginalSignature = new GridBagConstraints();
		gbcRdbtnOriginalSignature.insets = new Insets(0, 0, 5, 5);
		gbcRdbtnOriginalSignature.anchor = GridBagConstraints.WEST;
		gbcRdbtnOriginalSignature.gridx = 2;
		gbcRdbtnOriginalSignature.gridy = 3;
		add(rdbtnOriginalSignature, gbcRdbtnOriginalSignature);
		
		rdbtnRandomKey = new JRadioButton(Strings.randomKey);
		rdbtnRandomKey.setToolTipText(Strings.randomKeyToolTip);
		rdbtnRandomKey.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbcRdbtnRandomKey = new GridBagConstraints();
		gbcRdbtnRandomKey.anchor = GridBagConstraints.WEST;
		gbcRdbtnRandomKey.insets = new Insets(0, 0, 5, 5);
		gbcRdbtnRandomKey.gridx = 2;
		gbcRdbtnRandomKey.gridy = 4;
		add(rdbtnRandomKey, gbcRdbtnRandomKey);
		
		
		rdbtnChooseSignature = new JRadioButton(Strings.chooseSignature);
		rdbtnChooseSignature.setToolTipText(Strings.chooseSignatureToolTip);
		rdbtnChooseSignature.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbcRdbtnChooseSignature = new GridBagConstraints();
		gbcRdbtnChooseSignature.anchor = GridBagConstraints.WEST;
		gbcRdbtnChooseSignature.insets = new Insets(0, 0, 5, 5);
		gbcRdbtnChooseSignature.gridx = 2;
		gbcRdbtnChooseSignature.gridy = 5;
		add(rdbtnChooseSignature, gbcRdbtnChooseSignature);
		
		ButtonGroup btgrp = new ButtonGroup();
		btgrp.add(rdbtnDontModifySignature);
		btgrp.add(rdbtnOriginalSignature);
		btgrp.add(rdbtnRandomKey);
		btgrp.add(rdbtnRecalculateSignature);
		btgrp.add(rdbtnChooseSignature);

		separator = new JSeparator();
		GridBagConstraints gbcSeparator = new GridBagConstraints();
		gbcSeparator.insets = new Insets(0, 0, 5, 5);
		gbcSeparator.gridx = 2;
		gbcSeparator.gridy = 5;
		add(separator, gbcSeparator);
		
		lblSecretKey = new JLabel(Strings.interceptRecalculationKey);
		GridBagConstraints gbcLblSecretKey = new GridBagConstraints();
		gbcLblSecretKey.insets = new Insets(0, 0, 5, 5);
		gbcLblSecretKey.anchor = GridBagConstraints.SOUTHWEST;
		gbcLblSecretKey.gridx = 2;
		gbcLblSecretKey.gridy = 6;
		add(lblSecretKey, gbcLblSecretKey);
		
		jwtKeyArea = new JTextArea("");
		jwtKeyArea.setEnabled(false);

		GridBagConstraints gbcKeyField = new GridBagConstraints();
		gbcKeyField.anchor = GridBagConstraints.NORTH;
		gbcKeyField.insets = new Insets(0, 0, 5, 5);
		gbcKeyField.fill = GridBagConstraints.HORIZONTAL;
		gbcKeyField.gridx = 2;
		gbcKeyField.gridy = 7;
		jwtKeyArea.setRows(5);
		
        JScrollPane jp = new JScrollPane(jwtKeyArea);
		
		add(jp, gbcKeyField);
		jwtKeyArea.setColumns(2);
		jwtKeyArea.setRows(2);
		jwtKeyArea.setLineWrap(false);
		
		lblProblem = new JLabel("");
		GridBagConstraints gbcLblProblem = new GridBagConstraints();
		gbcLblProblem.insets = new Insets(0, 0, 5, 5);
		gbcLblProblem.gridx = 1;
		gbcLblProblem.gridy = 8;
		add(lblProblem, gbcLblProblem);
		
		lblNewLabel = new JLabel("Alg None Attack:");
		GridBagConstraints gbcLblNewLabel = new GridBagConstraints();
		gbcLblNewLabel.anchor = GridBagConstraints.WEST;
		gbcLblNewLabel.insets = new Insets(0, 0, 5, 5);
		gbcLblNewLabel.gridx = 2;
		gbcLblNewLabel.gridy = 9;
		add(lblNewLabel, gbcLblNewLabel);
		
		lblCookieFlags = new JLabel("");
		GridBagConstraints gbcLblCookieFlags = new GridBagConstraints();
		gbcLblCookieFlags.insets = new Insets(0, 0, 5, 5);
		gbcLblCookieFlags.anchor = GridBagConstraints.WEST;
		gbcLblCookieFlags.gridx = 1;
		gbcLblCookieFlags.gridy = 10;
		add(lblCookieFlags, gbcLblCookieFlags);
		
		noneAttackComboBox = new JComboBox<String>();
		GridBagConstraints gbcNoneAttackComboBox = new GridBagConstraints();
		gbcNoneAttackComboBox.insets = new Insets(0, 0, 5, 5);
		gbcNoneAttackComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbcNoneAttackComboBox.gridx = 2;
		gbcNoneAttackComboBox.gridy = 10;
		add(noneAttackComboBox, gbcNoneAttackComboBox);
		
		chkbxCveAttack = new JCheckBox("CVE-2018-0114 Attack");
		chkbxCveAttack.setToolTipText("The public and private key used can be found in src/app/helpers/Strings.java");
		chkbxCveAttack.setHorizontalAlignment(SwingConstants.LEFT);
		GridBagConstraints gbcChkbxCveAttack = new GridBagConstraints();
		gbcChkbxCveAttack.anchor = GridBagConstraints.WEST;
		gbcChkbxCveAttack.insets = new Insets(0, 0, 5, 5);
		gbcChkbxCveAttack.gridx = 2;
		gbcChkbxCveAttack.gridy = 11;
		add(chkbxCveAttack, gbcChkbxCveAttack);
		
		lbRegisteredClaims = new JLabel();
		lbRegisteredClaims.setBackground(SystemColor.controlHighlight);
		GridBagConstraints gbcLbRegisteredClaims = new GridBagConstraints();
		gbcLbRegisteredClaims.insets = new Insets(0, 0, 5, 5);
		gbcLbRegisteredClaims.fill = GridBagConstraints.BOTH;
		gbcLbRegisteredClaims.gridx = 2;
		gbcLbRegisteredClaims.gridy = 12;
		add(lbRegisteredClaims, gbcLbRegisteredClaims);
		
		btnCopyPubPrivKeyCveAttack = new JButton("Copy used public &private key to clipboard used in CVE attack");
		btnCopyPubPrivKeyCveAttack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Toolkit.getDefaultToolkit()
		        .getSystemClipboard()
		        .setContents(
		                new StringSelection("Public Key:\r\n"+Config.cveAttackModePublicKey+"\r\n\r\nPrivate Key:\r\n"+Config.cveAttackModePrivateKey),
		                null
		        );
			}
		});
		btnCopyPubPrivKeyCveAttack.setVisible(false);
		GridBagConstraints gbcButton = new GridBagConstraints();
		gbcButton.insets = new Insets(0, 0, 0, 5);
		gbcButton.gridx = 2;
		gbcButton.gridy = 13;
		add(btnCopyPubPrivKeyCveAttack, gbcButton);
		
		noneAttackComboBox.addItem("  -");
		noneAttackComboBox.addItem("Alg: none");
		noneAttackComboBox.addItem("Alg: None");
		noneAttackComboBox.addItem("Alg: nOnE");
		noneAttackComboBox.addItem("Alg: NONE");
		
	}
	
	public AbstractButton getRdbtnDontModify() {
		return rdbtnDontModifySignature;
	}
	
	public JRadioButton getRdbtnChooseSignature() {
		return rdbtnChooseSignature;
	}
	
	public JRadioButton getRdbtnRecalculateSignature() {
		return rdbtnRecalculateSignature;
	}
	
	public JComboBox<String> getNoneAttackComboBox() {
		return noneAttackComboBox;
	}
	
	public JCheckBox getCVEAttackCheckBox() {
		return chkbxCveAttack;
	}

	public JRadioButton getRdbtnRandomKey() {
		return rdbtnRandomKey;
	}

	public JButton getCVECopyBtn(){
		return btnCopyPubPrivKeyCveAttack;
	}
	
	public JRadioButton getRdbtnOriginalSignature() {
		return rdbtnOriginalSignature;
	}

	public void updateSetView(final boolean reset) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if(!jwtArea.getText().equals(jwtIM.getJWTJSON())){
					jwtArea.setText(jwtIM.getJWTJSON());
					jwtAreaOriginalContent = jwtIM.getJWTJSON();
				}
				jwtKeyArea.setText(jwtIM.getJWTKey());
				if(reset){
					rdbtnDontModifySignature.setSelected(true);
					jwtKeyArea.setText("");
					jwtKeyArea.setEnabled(false);
				}
				jwtArea.setCaretPosition(0);
				lblProblem.setText(jwtIM.getProblemDetail());
				
				if(jwtIM.getcFW().isCookie()){
					lblCookieFlags.setText(jwtIM.getcFW().toHTMLString());
				}else{
					lblCookieFlags.setText("");
				}
				lbRegisteredClaims.setText(jwtIM.getTimeClaimsAsText());
			}
		});
	}
	
	public JTextArea getJwtArea() {
		return jwtArea;
	}
	
	public  RSyntaxTextArea getJwtAreaAsRSyntax() {
		return jwtArea;
	}
	
	public void setKeyFieldState(boolean state){
		jwtKeyArea.setEnabled(state);
	}
	
	public boolean jwtWasChanged() {
		if(jwtArea.getText()==null) {
			return false;
		}
		return !jwtAreaOriginalContent.equals(jwtArea.getText());
	}
	
	public String getJWTfromArea(){
		return jwtArea.getText();
	}
	
	public String getSelectedData() {
		return jwtArea.getSelectedText();
	}

	public String getKeyFieldValue() {
		return jwtKeyArea.getText();
	}
	
	public void setKeyFieldValue(String string) {
		jwtKeyArea.setText(string);
	}
}
