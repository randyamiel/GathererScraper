package com.gelakinetic.GathererScraper;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * This class displays a list of formats in the main window. Each format can be
 * double clicked, which brings up a dialog where valid sets and banned or
 * restricted cards can be populated for that format. It also handles loading
 * and saving from json files
 *
 * @author AEFeinstein
 *
 */
public class LegalityListModel extends AbstractListModel<String> {

	/** Serializable version number */
	private static final long		serialVersionUID	= -7090656711463683238L;
	/**
	 * A JSON object which contains banned, restricted, and expansion
	 * information for each format
	 */
	private JSONObject				mLegalityJson;
	/** A list of formats to display. Each one populates the dialog differently */
	private ArrayList<String>		mFormats			= new ArrayList<String>();
	/** The number of dialog windows open. Needs to be 0 to scrape cards */
	int								mWindowsOpen		= 0;
	/** A list of all expansions to check input against */
	private ArrayList<Expansion>	mExpansions;
	/** Keep track of invalid (mistyped) set codes for error display */
	private ArrayList<String>		mInvalidExpansions	= new ArrayList<String>();

	/**
	 * Returns the format at the given index
	 *
	 * @return The string name of the format
	 */
	@Override
	public String getElementAt(int arg0) {
		return mFormats.get(arg0);
	}

	/**
	 * Returns the number of formats in this model
	 *
	 * @return the number of formats in this model
	 */
	@Override
	public int getSize() {
		return mFormats.size();
	}

	/**
	 * Sets the list of expansions to check input against
	 *
	 * @param expansions
	 *            A list of all expansions
	 */
	public void setExpansions(ArrayList<Expansion> expansions) {
		this.mExpansions = expansions;
	}

	/**
	 * Load all information about legality from a json file into this object
	 *
	 * @param legalJson
	 *            The file to read from
	 * @throws FileNotFoundException
	 *             If the file doesn't exist
	 * @throws IOException
	 *             If the file can't be read
	 * @throws ParseException
	 *             If the data in the file is corrupted
	 */
	public void loadLegalities(File legalJson) throws FileNotFoundException, IOException, ParseException {
		JSONParser parser = new JSONParser();
		mLegalityJson = (JSONObject) ((JSONObject) parser.parse(new FileReader(legalJson))).get("Formats");

		for (String key : mLegalityJson.keySet()) {
			mFormats.add(key);
		}
	}

	/**
	 * This file writes all legality information collected in the dialogs into a
	 * single json file
	 *
	 * @param frame
	 *            If an error dialog needs to be shown, this frame will manage
	 *            it
	 * @param path
	 *            The path of the file to be written
	 * @param date
	 *            A date string, MM/dd/yyyy
	 * @return false if the file was not written, true if it was
	 * @throws IOException
	 *             Thrown if something goes horribly wrong
	 */
	public boolean writeJsonToFile(JFrame frame, String path, String date) throws IOException {
		/*
		 * If there are invalid expansions, don't write the file. Show an error
		 * dialog instead
		 */
		if (mInvalidExpansions.size() > 0) {
			String invalidFormats = "";
			for (String s : mInvalidExpansions) {
				if (invalidFormats.length() > 0) {
					invalidFormats += ", ";
				}
				invalidFormats += s;
			}
			String errorStr = "";
			if (mInvalidExpansions.size() > 1) {
				errorStr = invalidFormats + " have invalid expansions";
			}
			if (mInvalidExpansions.size() == 1) {
				errorStr = invalidFormats + " has invalid expansions";
			}
			JOptionPane.showMessageDialog(frame, errorStr, "Invalid Expansions", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else {
			JSONObject topLevel = new JSONObject();

			topLevel.put("Date", date);
			topLevel.put("Formats", mLegalityJson);

			FileWriter file;

			file = new FileWriter(new File(path, GathererScraperUi.LEGAL_FILE_NAME));
			file.write(topLevel.toJSONString());
			file.flush();
			file.close();
			return true;
		}
	}

	/**
	 * When an entry is double clicked a dialog is shown with text areas for
	 * allowed expansions, banned cards, and restricted cards. This function creates,
	 * fills, and displays the dialog. It also handles saving information when
	 * the dialog is closed.
	 *
	 * @param index
	 *            The index of the format to display
	 * @wbp.parser.entryPoint
	 */
	public void showDialog(final int index) {
		/* Set up the UI and stuff */
		JFrame legalityFrame = new JFrame(mFormats.get(index) + " Legality");
		legalityFrame.setBounds(100, 100, 768, 512);

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 116, 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 22, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		legalityFrame.getContentPane().setLayout(gridBagLayout);

		JLabel lblLegalExpansions = new JLabel("Legal Expansions");
		GridBagConstraints gbc_lblLegalExpansions = new GridBagConstraints();
		gbc_lblLegalExpansions.insets = new Insets(0, 0, 5, 5);
		gbc_lblLegalExpansions.gridx = 0;
		gbc_lblLegalExpansions.gridy = 0;
		legalityFrame.getContentPane().add(lblLegalExpansions, gbc_lblLegalExpansions);

		JLabel lblRestrictedinDeck = new JLabel("Restricted (Banned Commanders)");
		GridBagConstraints gbc_lblRestrictedinDeck = new GridBagConstraints();
		gbc_lblRestrictedinDeck.insets = new Insets(0, 0, 5, 5);
		gbc_lblRestrictedinDeck.gridx = 1;
		gbc_lblRestrictedinDeck.gridy = 0;
		legalityFrame.getContentPane().add(lblRestrictedinDeck, gbc_lblRestrictedinDeck);

		JLabel lblBannedasCommander = new JLabel("Banned");
		GridBagConstraints gbc_lblBannedasCommander = new GridBagConstraints();
		gbc_lblBannedasCommander.insets = new Insets(0, 0, 5, 0);
		gbc_lblBannedasCommander.gridx = 2;
		gbc_lblBannedasCommander.gridy = 0;
		legalityFrame.getContentPane().add(lblBannedasCommander, gbc_lblBannedasCommander);

		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 1;
		legalityFrame.getContentPane().add(scrollPane, gbc_scrollPane);

		/* Valid Expansions */

		final JTextArea expansionsTextArea = new JTextArea();
		final JSONArray expansionsJson = (JSONArray) ((JSONObject) mLegalityJson.get(mFormats.get(index))).get("Sets");
		String expansionsString = "";
		for (Object o : expansionsJson) {
			expansionsString += (String) o + "\n";
		}
		expansionsTextArea.setText(expansionsString);

		scrollPane.setViewportView(expansionsTextArea);

		JScrollPane scrollPane_1 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_1 = new GridBagConstraints();
		gbc_scrollPane_1.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_1.insets = new Insets(0, 0, 0, 5);
		gbc_scrollPane_1.gridx = 1;
		gbc_scrollPane_1.gridy = 1;
		legalityFrame.getContentPane().add(scrollPane_1, gbc_scrollPane_1);

		/* Restricted Cards */

		final JTextArea restrictedTextArea = new JTextArea();
		final JSONArray restrictedJson = (JSONArray) ((JSONObject) mLegalityJson.get(mFormats.get(index)))
				.get("Restrictedlist");
		String restrictedString = "";
		for (Object o : restrictedJson) {
			restrictedString += (String) o + "\n";
		}
		restrictedTextArea.setText(restrictedString);

		scrollPane_1.setViewportView(restrictedTextArea);

		JScrollPane scrollPane_2 = new JScrollPane();
		GridBagConstraints gbc_scrollPane_2 = new GridBagConstraints();
		gbc_scrollPane_2.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_2.gridx = 2;
		gbc_scrollPane_2.gridy = 1;
		legalityFrame.getContentPane().add(scrollPane_2, gbc_scrollPane_2);

		/* Banned Cards */

		final JTextArea bannedTextArea = new JTextArea();
		final JSONArray bannedJson = (JSONArray) ((JSONObject) mLegalityJson.get(mFormats.get(index))).get("Banlist");
		String bannedString = "";
		for (Object o : bannedJson) {
			bannedString += (String) o + "\n";
		}
		bannedTextArea.setText(bannedString);

		scrollPane_2.setViewportView(bannedTextArea);

		/* What happens when the frame closes */
		legalityFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		legalityFrame.addWindowListener(new java.awt.event.WindowAdapter() {
			/**
			 * When the frame closes, check to make sure the input is good, then
			 * save it. The information is written to the final variables
			 * expansionJson, bannedJson, and restrictedJson, which are all
			 * references into mLegalityJson
			 *
			 * @param windowEvent
			 *            An event that indicates this window has changed it's
			 *            status
			 */
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				/* Make some JSON out of the text in the box */
				String[] expansionsString = expansionsTextArea.getText().split("[\\r\\n]+");
				expansionsJson.clear();

				/* only add non-empty lines */
				for (String expansion : expansionsString) {
					if (expansion.length() > 0) {
						expansionsJson.add(expansion);
					}
				}

				/* Check to make sure expansions exist */
				mInvalidExpansions.remove(mFormats.get(index));
				for (String thisExpansion : expansionsString) {
					if (!thisExpansion.isEmpty()) {
						boolean contains = false;
						for (Expansion expansion : mExpansions) {
							if (expansion.mCode_gatherer.equals(thisExpansion)) {
								contains = true;
								break;
							}
						}
						if (!contains) {
							mInvalidExpansions.add(mFormats.get(index));
							break;
						}
					}
				}

				/*
				 * No sanity check on cards, since they may not have been
				 * scraped now Also, these should be copy/pasted from the
				 * Internet
				 */
				String[] bannedString = bannedTextArea.getText().split("[\\r\\n]+");
				bannedJson.clear();
				for (String banned : bannedString) {
					if (!banned.isEmpty()) {
						bannedJson.add(banned);
					}
				}

				String[] restrictedString = restrictedTextArea.getText().split("[\\r\\n]+");
				restrictedJson.clear();
				for (String restricted : restrictedString) {
					if (!restricted.isEmpty()) {
						restrictedJson.add(restricted);
					}
				}
				mWindowsOpen--;
			}
		});

		/* show it */
		legalityFrame.setVisible(true);
		mWindowsOpen++;
	}
}
