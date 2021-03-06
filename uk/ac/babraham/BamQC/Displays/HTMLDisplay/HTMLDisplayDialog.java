/**
 * Copyright 2012-15 Simon Andrews
 *
 *    This file is part of BamQC.
 *
 *    BamQC is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    BamQC is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with BamQC; if not, write to the Free Software
 *    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
/*
 * Changelog: 
 * - Piero Dalle Pezze: Code taken from SeqMonk.
 * - Simon Andrews: Class creation.
 */
package uk.ac.babraham.BamQC.Displays.HTMLDisplay;

import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import org.apache.log4j.Logger;

import uk.ac.babraham.BamQC.BamQCApplication;

/**
 * 
 * @author Simon Andrews
 *
 */
public class HTMLDisplayDialog extends JDialog {

	private static Logger log = Logger.getLogger(HTMLDisplayDialog.class);
	
	private static final long serialVersionUID = -665506733941526484L;

	public HTMLDisplayDialog (String html) {
		
		super(BamQCApplication.getInstance(),"Crash Report Help");
		
		log.debug("Making help dialog");
		
		JEditorPane jep = new JEditorPane("text/html", html);
		
		setContentPane(new JScrollPane(jep,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
		
		setSize(700,500);
		setLocationRelativeTo(BamQCApplication.getInstance());
		setVisible(true);
		
	}
	
	
	
}
