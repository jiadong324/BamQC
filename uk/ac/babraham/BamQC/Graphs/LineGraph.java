/**
 * Copyright Copyright 2010-14 Simon Andrews
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
 * - Piero Dalle Pezze: Added y axis label, antialiasing, axes numbers resizing to avoid overlapping.
 * - Simon Andrews: Class creation.
 */
package uk.ac.babraham.BamQC.Graphs;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.swing.JPanel;

import uk.ac.babraham.BamQC.Utilities.AxisScale;
import uk.ac.babraham.BamQC.Utilities.FormatNumber;

/**
 * 
 * @author Simon Andrews
 * @author Piero Dalle Pezze
 *
 */
public class LineGraph extends JPanel {

	private static final long serialVersionUID = -7893883434501058128L;
	private String [] xTitles;
	private String xLabel;
	private String yLabel;
	private String [] xCategories;
	private double [][] data;
	private String graphTitle;
	private double minY;
	private double maxY;
	private double yInterval;
	private int height = -1;
	private int width = -1;
	
	private static final Color [] COLOURS = new Color[] {new Color(220,0,0), new Color(0,0,220), new Color(0,220,0), Color.DARK_GRAY, Color.MAGENTA, Color.ORANGE,Color.YELLOW,Color.CYAN,Color.PINK,Color.LIGHT_GRAY};
	
	public LineGraph (double [] [] data, double minY, double maxY, String xLabel, String yLabel, String [] xTitles, int [] xCategories, String graphTitle) {
		this(data,minY,maxY,xLabel,yLabel,xTitles,new String[0],graphTitle);
		this.xCategories = new String [xCategories.length];
		for (int i=0;i<xCategories.length;i++) {
			this.xCategories[i] = ""+xCategories[i];
		}
	}
	
	public LineGraph (double [] [] data, double minY, double maxY, String xLabel, String yLabel, String [] xTitles, String [] xCategories, String graphTitle) {
		this.data = data;
		this.minY = minY;
		this.maxY = maxY;
		this.xTitles = xTitles;
		this.xLabel = xLabel;
		this.yLabel = yLabel;
		this.xCategories = xCategories;
		this.graphTitle = graphTitle;
		this.yInterval = new AxisScale (minY, maxY).getInterval();
	}
	
	@Override
	public Dimension getPreferredSize () {
		return new Dimension(800,600);
	}

	@Override
	public Dimension getMinimumSize () {
		return new Dimension(100,200);
	}

	
	@Override
	public int getHeight () {
		if (height <0) {
			return super.getHeight();
		}
		return height;
	}

	@Override
	public int getWidth () {
		if (width <0) {
			return super.getWidth();
		}
		return width;
	}

	@Override
	protected void paintComponent(Graphics g) {
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(Color.BLACK);
		
		if (g instanceof Graphics2D) {
			((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		
		int lastY = 0;
		
		double yStart;
		
		if (minY % yInterval == 0) {
			yStart = minY;
		}
		else {
			yStart = yInterval * (((int)minY/yInterval)+1);
		}
		
		int xOffset = 0;
		
		// Draw the yLabel on the left of the yAxis
		int yLabelRightShift = 12;
		if(yLabel == null || yLabel.isEmpty()) {
			yLabelRightShift = 0;
		} else {
			if (g instanceof Graphics2D) {
				Graphics2D g2 = (Graphics2D)g;
				AffineTransform orig = g2.getTransform();
				g2.rotate(-Math.PI/2);
				g2.setColor(Color.BLACK);
				g2.drawString(yLabel, -getY(-yInterval)/2 - (g.getFontMetrics().stringWidth(yLabel)/2), yLabelRightShift);
				g2.setTransform(orig);
			}
		}

		
		// Draw the y axis labels
		int lastYLabelEnd = Integer.MAX_VALUE;
		for (double i = yStart; i <= maxY; i += yInterval) {
			String label = "" + new BigDecimal(i).setScale(
					FormatNumber.getFirstSignificantNonNullDecimalPosition(yInterval), RoundingMode.HALF_UP).doubleValue();	
			label = label.replaceAll(".0$", ""); // Don't leave trailing .0s where we don't need them.	
			// Calculate the new xOffset depending on the widest ylabel.
			int width = g.getFontMetrics().stringWidth(label);
			if (width > xOffset) {
				xOffset = width;
			}
			// place the y axis labels so that they don't overlap when the plot is resized.
			int baseNumberHeight = g.getFontMetrics().getHeight();
			int baseNumberPosition = getY(i)+(baseNumberHeight/2);
			if (baseNumberPosition + baseNumberHeight < lastYLabelEnd) {
				// Draw the y axis labels
				g.drawString(label, yLabelRightShift+6, baseNumberPosition);
				lastYLabelEnd = baseNumberPosition + 2;
			}
		}
		

		// Give the x axis a bit of breathing space
		xOffset = xOffset + yLabelRightShift + 8;
		
		
		// Draw the graph title
		int titleWidth = g.getFontMetrics().stringWidth(graphTitle);
		g.drawString(graphTitle, (xOffset + ((getWidth()-(xOffset+10))/2)) - (titleWidth/2), 30);
		
		

		
		
		// Now draw the data points
		double baseWidth = 1.0*(getWidth()-(xOffset+10))/data[0].length;

		
		// System.out.println("Base Width is "+baseWidth);
		// First draw faint boxes over alternating bases so you can see which is which
		// Let's find the longest label, and then work out how often we can draw labels
		
		int lastXLabelEnd = 0;
		
		for (int i=0;i<data[0].length;i++) {
			if (i%2 != 0) {
				g.setColor(new Color(230, 230, 230));
				g.fillRect((int)(xOffset+(baseWidth*i)), 40, (int)(baseWidth), getHeight()-80);
			}
			g.setColor(Color.BLACK);
			
			//String baseNumber = ""+xCategories[i];
			//baseNumber = FormatNumber.compactInteger(baseNumber);
			String baseNumber = FormatNumber.convertToScientificNotation(xCategories[i]);
			baseNumber = baseNumber.replaceAll(".0$", ""); // Don't leave trailing .0s where we don't need them.
			int baseNumberWidth = g.getFontMetrics().stringWidth(baseNumber);
			int baseNumberPosition =  (int)((baseWidth/2)+xOffset+(baseWidth*i)-(baseNumberWidth/2));
			
			if (baseNumberPosition > lastXLabelEnd) {
				g.drawString(baseNumber,baseNumberPosition, getHeight()-25);
				lastXLabelEnd = baseNumberPosition+baseNumberWidth+5;
			}
		}
		
		// Now draw horizontal lines across from the y axis

		g.setColor(new Color(180,180,180));
		for (double i=yStart;i<=maxY;i+=yInterval) {
			g.drawLine(xOffset, getY(i), getWidth()-10, getY(i));
		}
		g.setColor(Color.BLACK);
		
		// Now draw the datasets
		
		if (g instanceof Graphics2D) {
			((Graphics2D)g).setStroke(new BasicStroke(2));
			//((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		}
		
		for (int d=0;d<data.length;d++) {
			g.setColor(COLOURS[d % COLOURS.length]);
						
			lastY = getY(data[d][0]);
			for (int i=1;i<data[d].length;i++) {
				if (Double.isNaN(data[d][i])) break;
				
				int thisY = getY(data[d][i]);
				
				g.drawLine((int)((baseWidth/2)+xOffset+(baseWidth*(i-1))), lastY, (int)((baseWidth/2)+xOffset+(baseWidth*i)), thisY);
				lastY = thisY;
			}
			
		}
		

		
		// Now draw the data legend

		if (g instanceof Graphics2D) {
			((Graphics2D)g).setStroke(new BasicStroke(1));
			//((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		}

		g.setColor(Color.BLACK);
		// Now draw the axes
		g.drawLine(xOffset, getHeight()-40, getWidth()-10,getHeight()-40);
		g.drawLine(xOffset, getHeight()-40, xOffset, 40);
		
		// Draw the xLabel under the xAxis
		g.drawString(xLabel, (getWidth()/2) - (g.getFontMetrics().stringWidth(xLabel)/2), getHeight()-5);
		
		
		
		// First we need to find the widest label
		int widestLabel = 0;
		for (int t=0;t<xTitles.length;t++) {
			int width = g.getFontMetrics().stringWidth(xTitles[t]);
			if (width > widestLabel) widestLabel = width;
		}
		
		// Add 3px either side for a bit of space;
		widestLabel += 6;
		
		// First draw a box to put the legend in
		g.setColor(Color.WHITE);
		g.fillRect((getWidth()-10)-widestLabel, 40, widestLabel, 3+(20*xTitles.length));
		g.setColor(Color.LIGHT_GRAY);
		g.drawRect((getWidth()-10)-widestLabel, 40, widestLabel, 3+(20*xTitles.length));

		// Now draw the actual labels
		for (int t=0;t<xTitles.length;t++) {
			g.setColor(COLOURS[t%COLOURS.length]);
			g.drawString(xTitles[t], ((getWidth()-10)-widestLabel)+3, 40+(20*(t+1)));
		}
		

		
		
	}

	private int getY(double y) {
		return (getHeight()-40) - (int)(((getHeight()-80)/(maxY-minY))*(y-minY));
	}
	
}
