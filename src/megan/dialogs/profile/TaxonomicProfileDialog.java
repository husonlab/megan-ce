/*
 *  Copyright (C) 2019 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.dialogs.profile;

import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirector;
import jloda.swing.util.RememberingComboBox;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import megan.dialogs.profile.commands.ApplyCommand;
import megan.dialogs.profile.commands.CancelCommand;
import megan.dialogs.profile.commands.UseProjectionMethodCommand;
import megan.dialogs.profile.commands.UseReadSpreadingMethodCommand;
import megan.viewer.TaxonomicLevels;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Objects;

/**
 * dialog to set up taxonomic profile calculation
 * Daniel Huson, 5.2014
 */
public class TaxonomicProfileDialog extends JDialog {
    public enum ProfileMethod {Projection, ReadSpreading}

    private boolean canceled = true;

    private final RememberingComboBox rankCbox = new RememberingComboBox();
    private final JTextField minPercentageField = new JTextField(8);

    private ProfileMethod method;

    private String rank;

    private final CommandManager commandManager;

    /**
     * setup and display the compare dialog
     *
     * @param dir
     */
    public TaxonomicProfileDialog(JFrame parent, IDirector dir) {
        super(parent);
        setIconImages(ProgramProperties.getProgramIconImages());

        method = ProfileMethod.Projection;

        commandManager = new CommandManager(dir, this, new String[]{"megan.dialogs.profile.commands"}, !ProgramProperties.isUseGUI());

        setTitle("Compute Taxonomic Profile - " + ProgramProperties.getProgramVersion());

        if (parent != null)
            setLocationRelativeTo(parent);
        else
            setLocation(300, 300);
        setSize(300, 200);

        setModal(true);

        rankCbox.setEditable(false);
        rankCbox.addItems(TaxonomicLevels.getAllMajorRanks());
        rank = ProgramProperties.get("ProfileRank", TaxonomicLevels.Genus);
        rankCbox.setSelectedItem(rank);

        float minPercent = Math.min(100f, Math.max(0f, (float) ProgramProperties.get("ProfileMinPercent", 1f)));
        minPercentageField.setText("" + minPercent);
        minPercentageField.setMaximumSize(new Dimension(100, 20));

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        final JPanel outerMiddlePanel = new JPanel();
        outerMiddlePanel.setLayout(new BoxLayout(outerMiddlePanel, BoxLayout.Y_AXIS));
        outerMiddlePanel.setBorder(new EmptyBorder(4, 2, 4, 2));

        final JPanel middlePanel = new JPanel();
        middlePanel.setLayout(new BoxLayout(middlePanel, BoxLayout.Y_AXIS));
        outerMiddlePanel.add(middlePanel);
        mainPanel.add(outerMiddlePanel, BorderLayout.CENTER);

        JPanel linePanel = new JPanel();
        linePanel.setLayout(new BoxLayout(linePanel, BoxLayout.X_AXIS));
        linePanel.add(new JLabel("Rank:"));
        linePanel.add(rankCbox);
        rankCbox.setToolTipText("Taxonomic rank at which profile is to be computed");
        linePanel.add(Box.createHorizontalGlue());
        middlePanel.add(linePanel);
        rankCbox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED)
                setRank(e.getItem().toString());
        });

        linePanel = new JPanel();
        linePanel.setLayout(new BoxLayout(linePanel, BoxLayout.X_AXIS));
        linePanel.add(new JLabel("Min Support Percent:"));
        linePanel.add(minPercentageField);
        minPercentageField.setToolTipText("Minimal percentage of assigned reads for a taxon to appear in the profile. Reads assigned to taxa that does not meet this threshold are dropped.");
        linePanel.add(Box.createHorizontalGlue());
        middlePanel.add(linePanel);

        middlePanel.add(new JLabel(" "));

        final ButtonGroup group = new ButtonGroup();
        linePanel = new JPanel();
        linePanel.setLayout(new BoxLayout(linePanel, BoxLayout.X_AXIS));
        AbstractButton button = commandManager.getButton(UseProjectionMethodCommand.NAME);
        if (getMethod() == ProfileMethod.Projection)
            button.setSelected(true);
        linePanel.add(button);
        group.add(button);
        linePanel.add(Box.createHorizontalGlue());

        middlePanel.add(linePanel);

        linePanel = new JPanel();
        linePanel.setLayout(new BoxLayout(linePanel, BoxLayout.X_AXIS));
        button = commandManager.getButton(UseReadSpreadingMethodCommand.NAME);
        if (getMethod() == ProfileMethod.ReadSpreading)
            button.setSelected(true);
        linePanel.add(button);
        group.add(button);
        linePanel.add(Box.createHorizontalGlue());
        middlePanel.add(linePanel);

        middlePanel.add(Box.createVerticalGlue());

        final JPanel bottomPanel = new JPanel();
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(commandManager.getButton(CancelCommand.NAME));
        bottomPanel.add(Box.createHorizontalStrut(5));
        button = commandManager.getButton(ApplyCommand.NAME);
        getRootPane().setDefaultButton((JButton) button);
        bottomPanel.add(button);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        commandManager.updateEnableState();
        setVisible(true);
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * get the command string
     */
    public String getCommand() {
        rank = Objects.requireNonNull(rankCbox.getSelectedItem()).toString();
        ProgramProperties.put("ProfileRank", rankCbox.getSelectedItem().toString());
        ProgramProperties.put("ProfileMinPercent", Basic.parseDouble(minPercentageField.getText()));
        return "compute profile=" + getMethod().toString() + " rank=" + rank + " minPercent=" + minPercentageField.getText() + ";";
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public ProfileMethod getMethod() {
        return method;
    }

    public void setMethod(ProfileMethod method) {
        this.method = method;
    }

    public String getRank() {
        return rank;
    }

    private void setRank(String rank) {
        this.rank = rank;
    }
}
