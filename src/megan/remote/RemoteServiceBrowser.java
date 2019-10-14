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
package megan.remote;

import jloda.swing.commands.CommandManager;
import jloda.swing.director.IDirectableViewer;
import jloda.swing.director.IDirector;
import jloda.swing.director.IViewerWithFindToolBar;
import jloda.swing.director.ProjectManager;
import jloda.swing.find.EmptySearcher;
import jloda.swing.find.FindToolBar;
import jloda.swing.find.SearchManager;
import jloda.swing.util.RememberingComboBox;
import jloda.swing.util.StatusBar;
import jloda.swing.util.ToolBar;
import jloda.swing.window.MenuBar;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import megan.core.Director;
import megan.core.Document;
import megan.main.MeganProperties;
import megan.remote.client.RemoteServiceManager;
import megan.remote.commands.OpenRemoteServerCommand;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Remote services browser
 * <p/>
 * Daniel Huson, 10.2014
 */
public class RemoteServiceBrowser extends JFrame implements IDirectableViewer, IViewerWithFindToolBar {
    private final Director dir;
    private boolean uptoDate;
    private boolean locked = false;

    private final JPanel mainPanel;

    private final MenuBar menuBar;

    private boolean showFindToolBar = false;
    private final SearchManager searchManager;

    private final JTabbedPane tabbedPane;

    private final CommandManager commandManager;

    private RememberingComboBox urlComboBox;
    private final JTextField userTextField = new JTextField(30);

    private final JPasswordField passwordTextField = new JPasswordField(30);
    //private JTextField passwordTextField = new JTextField(30);

    private final JCheckBox saveCredentialsCBox = new JCheckBox();

    private final StatusBar statusBar;

    /**
     * constructor
     */
    public RemoteServiceBrowser(JFrame parent) {
        this.dir = new Director(new Document());
        dir.getDocument().setDirty(true); // prevent opening in this document
        dir.addViewer(this);

        commandManager = new CommandManager(dir, this, new String[]{"megan.commands", "megan.remote.commands"}, !ProgramProperties.isUseGUI());

        setTitle();

        setSize(500, 400);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setIconImages(ProgramProperties.getProgramIconImages());

        menuBar = new MenuBar(this, GUIConfiguration.getMenuConfiguration(), commandManager);
        setJMenuBar(menuBar);
        MeganProperties.addPropertiesListListener(menuBar.getRecentFilesListener());
        MeganProperties.notifyListChange(ProgramProperties.RECENTFILES);
        ProjectManager.addAnotherWindowWithWindowMenu(dir, menuBar.getWindowMenu());

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        final ToolBar toolBar = new ToolBar(this, GUIConfiguration.getToolBarConfiguration(), commandManager);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(toolBar, BorderLayout.NORTH);

        statusBar = new StatusBar();
        statusBar.setToolTipText("Status bar");
        getContentPane().add(statusBar, BorderLayout.SOUTH);

        searchManager = new SearchManager(dir, this, new EmptySearcher(), false, true);

        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(e -> updateView(IDirector.ENABLE_STATE));

        tabbedPane.add("Add Server", createOpenRemoteServerPanel());
        tabbedPane.setSelectedIndex(0);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        getContentPane().add(mainPanel, BorderLayout.CENTER);

        getContentPane().validate();

        commandManager.updateEnableState();
        getFrame().setLocationRelativeTo(parent);
    }

    /**
     * create the open remote server panel
     *
     * @return open remote panel
     */
    private JPanel createOpenRemoteServerPanel() {

        RemoteServiceManager.ensureCredentialsHaveBeenLoadedFromProperties();

        final JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Open remote server"));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel(" "));

        urlComboBox = new RememberingComboBox();
        urlComboBox.setBorder(BorderFactory.createBevelBorder(1));
        //  ((JTextComponent) urlComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(createDocumentListener());

        final ItemListener itemListener = e -> {
            final String shortServerName = e.getItem().toString().replace("http://", "").replaceAll("/$", "") + "::";
            final String user = RemoteServiceManager.getUser(shortServerName);
            userTextField.setText(user != null ? user : "");
            final String password = RemoteServiceManager.getPassword(shortServerName);
            passwordTextField.setText(password != null ? password : "");
        };
        urlComboBox.addItemListener(itemListener);
        urlComboBox.addItemsFromString(ProgramProperties.get("RemoteServers", ""), "%%%");
        for (int i = 0; i < urlComboBox.getItemCount(); i++) {
            if (urlComboBox.getItemAt(i).contains("megan-db.org")) { // remove old default
                urlComboBox.removeItemAt(i);
                break;
            }
        }
        urlComboBox.setMaximumSize(new Dimension(2000, 25));
        urlComboBox.setPreferredSize(new Dimension(400, 25));
        urlComboBox.setToolTipText("MEGAN server URL");

        JPanel aRow = new JPanel();
        aRow.setLayout(new BoxLayout(aRow, BoxLayout.X_AXIS));
        aRow.add(Box.createHorizontalGlue());
        aRow.add(new JLabel("Server:"));
        aRow.add(urlComboBox);
        panel.add(aRow);

        userTextField.setMaximumSize(new Dimension(2000, 25));
        userTextField.setPreferredSize(new Dimension(400, 20));
        userTextField.setToolTipText("User id required by server. Note that this is currently transmitted unencrypted.");
        JPanel bRow = new JPanel();
        bRow.setLayout(new BoxLayout(bRow, BoxLayout.X_AXIS));
        bRow.add(Box.createHorizontalGlue());
        bRow.add(new JLabel("User:"));
        bRow.add(userTextField);
        panel.add(bRow);

        passwordTextField.setMaximumSize(new Dimension(2000, 25));
        passwordTextField.setPreferredSize(new Dimension(400, 20));
        passwordTextField.setToolTipText("Password required by server. Note that this is currently transmitted unencrypted.");
        JPanel cRow = new JPanel();
        cRow.setLayout(new BoxLayout(cRow, BoxLayout.X_AXIS));
        cRow.add(Box.createHorizontalGlue());
        cRow.add(new JLabel("Password:"));
        cRow.add(passwordTextField);
        panel.add(cRow);

        panel.add(new JLabel(" "));
        JPanel dRow = new JPanel();
        dRow.setLayout(new BoxLayout(dRow, BoxLayout.X_AXIS));
        dRow.add(Box.createHorizontalGlue());
        dRow.add(commandManager.getButton(OpenRemoteServerCommand.ALT_NAME));
        panel.add(dRow);

        final JPanel outside = new JPanel();
        outside.setLayout(new BoxLayout(outside, BoxLayout.Y_AXIS));
        outside.add(Box.createVerticalGlue());
        outside.add(panel, BorderLayout.NORTH);
        outside.add(Box.createVerticalGlue());

        final JPanel aLine = new JPanel();
        aLine.setLayout(new BoxLayout(aLine, BoxLayout.LINE_AXIS));
        aLine.add(Box.createHorizontalGlue());
        aLine.add(new JLabel("Save credentials"));
        aLine.add(saveCredentialsCBox);
        saveCredentialsCBox.addActionListener(e -> ProgramProperties.put("SaveRemoteCredentials", saveCredentialsCBox.isSelected()));
        saveCredentialsCBox.setToolTipText("Save MeganServer credentials");
        outside.add(aLine, BorderLayout.SOUTH);

        urlComboBox.setSelectedIndex(0);

        return outside;
    }


    @Override
    public boolean isShowFindToolBar() {
        return showFindToolBar;
    }

    @Override
    public void setShowFindToolBar(boolean show) {
        this.showFindToolBar = show;
        updateView(IDirector.ENABLE_STATE);
    }

    @Override
    public SearchManager getSearchManager() {
        return searchManager;
    }

    public boolean isUptoDate() {
        return uptoDate;
    }

    public JFrame getFrame() {
        return this;
    }

    public void updateView(String what) {
        uptoDate = false;
        setTitle();
        saveCredentialsCBox.setSelected(ProgramProperties.get("SaveRemoteCredentials", true));
        commandManager.updateEnableState();
        if (tabbedPane.getSelectedComponent() instanceof ServicePanel) {
            final ServicePanel servicePanel = (ServicePanel) tabbedPane.getSelectedComponent();
            searchManager.setSearcher(servicePanel.getjTreeSearcher());
            statusBar.setText2("Number of files: " + servicePanel.getService().getAvailableFiles().size());
            statusBar.setToolTipText(servicePanel.getToolTipText() + (servicePanel.getSelectedFiles().size() > 0 ?
                    " (" + servicePanel.getSelectedFiles().size() + " selected)" : ""));

            servicePanel.updateFonts();
        } else {
            searchManager.setSearcher(new EmptySearcher());
            statusBar.setText2("");
            statusBar.setToolTipText("");
        }

        final FindToolBar findToolBar = searchManager.getFindDialogAsToolBar();
        if (findToolBar.isClosing()) {
            showFindToolBar = false;
            findToolBar.setClosing(false);
        }
        if (!findToolBar.isEnabled() && showFindToolBar) {
            mainPanel.add(findToolBar, BorderLayout.NORTH);
            findToolBar.setEnabled(true);
            getContentPane().validate();
            getCommandManager().updateEnableState();
        } else if (findToolBar.isEnabled() && !showFindToolBar) {
            mainPanel.remove(findToolBar);
            findToolBar.setEnabled(false);
            getContentPane().validate();
            getCommandManager().updateEnableState();
        }
        if (findToolBar.isEnabled())
            findToolBar.clearMessage();

        uptoDate = true;
    }

    public void lockUserInput() {
        locked = true;
        commandManager.setEnableCritical(false);
        searchManager.getFindDialogAsToolBar().setEnableCritical(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        urlComboBox.setEnabled(false);
        userTextField.setEnabled(false);
        passwordTextField.setEnabled(false);
        saveCredentialsCBox.setEnabled(false);

        menuBar.setEnableRecentFileMenuItems(false);
    }

    public void unlockUserInput() {
        commandManager.setEnableCritical(true);
        searchManager.getFindDialogAsToolBar().setEnableCritical(true);
        setCursor(Cursor.getDefaultCursor());

        urlComboBox.setEnabled(true);
        userTextField.setEnabled(true);
        passwordTextField.setEnabled(true);
        saveCredentialsCBox.setEnabled(true);

        menuBar.setEnableRecentFileMenuItems(true);

        locked = false;
    }

    /**
     * is viewer currently locked?
     *
     * @return true, if locked
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * destroy the view. todo: this does not get called at present
     *
     * @throws jloda.util.CanceledException
     */
    public void destroyView() throws CanceledException {
        MeganProperties.removePropertiesListListener(menuBar.getRecentFilesListener());
        ProgramProperties.put("RemoteServers", urlComboBox.getItemsAsString(20, "%%%"));
        dir.removeViewer(this);
        searchManager.getFindDialogAsToolBar().close();
        if (!ProgramProperties.get("SaveRemoteCredentials", false))
            ProgramProperties.put("MeganServerCredentials", new String[0]);
        dispose();
    }

    public void setUptoDate(boolean flag) {
        uptoDate = flag;
    }

    /**
     * set the title of the window
     */
    private void setTitle() {
        setTitle("Remote MEGAN Files");
    }


    public CommandManager getCommandManager() {
        return commandManager;
    }

    /**
     * add a remote file service
     *
     * @param service
     */
    public void addService(final IRemoteService service) {
        final ServicePanel servicePanel = new ServicePanel(service, this);
        servicePanel.setToolTipText(service.getShortName());

        tabbedPane.add(servicePanel, 0);
        tabbedPane.setTitleAt(0, abbreviateName(service.getShortName()));
        tabbedPane.setSelectedIndex(0);
    }

    /**
     * abbreviate name
     *
     * @param name
     * @return name of length <=18
     */
    private String abbreviateName(String name) {
        name = name.replace("http://", "").replace(":8080", "").replaceAll("/MeganServer$", "");
        if (name.length() <= 18)
            return name;
        return "..." + name.substring(name.length() - 15);
    }

    /**
     * get the URL of a service
     *
     * @return URL
     */
    public String getURL() {
        Component component = tabbedPane.getSelectedComponent();

        if (isServiceSelected()) {
            return ((ServicePanel) component).getURL();
        } else {
            String url = urlComboBox.getCurrentText(false);
            if (url != null)
                return url.trim();
            else
                return "";
        }
    }

    public void setURL(String URL) {
        urlComboBox.setSelectedItem(URL);
    }

    public String getUser() {
        return userTextField.getText().trim();
    }

    public void setUser(String user) {
        userTextField.setText(user);
    }

    public String getPasswd() {
        return passwordTextField.getText().trim();
    }

    public void setPasswd(String user) {
        passwordTextField.setText(user);
    }

    /**
     * get name for this type of viewer
     *
     * @return name
     */
    public String getClassName() {
        return "RemoteBrowser";
    }

    /**
     * get the number of currently chosen documents
     *
     * @return currently chosen documents
     */
    public int getNumberOfChosenDocuments() {
        return 0;
    }

    public Director getDir() {
        return dir;
    }

    public DocumentListener createDocumentListener() {
        return new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            public void removeUpdate(DocumentEvent e) {
                changedUpdate(e);
            }

            public void changedUpdate(DocumentEvent e) {
                try {
                    final String shortServerName = e.getDocument().getText(0, e.getDocument().getLength()).replace("http://", "").replaceAll("/$", "") + "::";
                    final String user = RemoteServiceManager.getUser(shortServerName);
                    userTextField.setText(user != null ? user : "");
                    final String password = RemoteServiceManager.getPassword(shortServerName);
                    passwordTextField.setText(password != null ? password : "");
                } catch (BadLocationException ignored) {
                }
                updateView(IDirector.ENABLE_STATE);
            }
        };
    }

    /**
     * save the current configuation
     */
    public void saveConfig() {
        urlComboBox.getCurrentText(true);
        ProgramProperties.put("RemoteServers", urlComboBox.getItemsAsString(20, "%%%"));
    }

    /**
     * close the named service
     *
     * @param url
     * @return true, if service found and closed
     */
    public boolean closeRemoteService(String url) {
        url = url.replace(".*://", "");
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getComponentAt(i) instanceof ServicePanel) {
                final ServicePanel panel = (ServicePanel) tabbedPane.getComponentAt(i);
                if (panel.getService().getServerURL().equalsIgnoreCase(url)) {
                    {
                        RemoteServiceManager.removeNode(url);
                        tabbedPane.remove(panel);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * is the currently selected panel a service panel?
     *
     * @return true if service panel currently selected
     */
    public boolean isServiceSelected() {
        return tabbedPane != null && tabbedPane.getSelectedComponent() != null && tabbedPane.getSelectedComponent() instanceof ServicePanel;

    }

    /**
     * select the given service tab, if present
     *
     * @param url
     */
    public boolean selectServiceTab(String url) {
        url = url.replaceAll(".*://", "");
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getTitleAt(i).equalsIgnoreCase(url)) {
                tabbedPane.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    public MenuBar getMenu() {
        return menuBar;
    }

    public ServicePanel getServicePanel() {
        if (tabbedPane != null && tabbedPane.getSelectedComponent() instanceof ServicePanel)
            return (ServicePanel) tabbedPane.getSelectedComponent();
        else
            return null;
    }
}
