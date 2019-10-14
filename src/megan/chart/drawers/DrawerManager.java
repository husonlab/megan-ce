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
package megan.chart.drawers;

import jloda.util.PluginClassLoader;
import megan.chart.IChartDrawer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * manages the chart drawers
 * <p/>
 * Daniel Huson, 4.2015
 */
public class DrawerManager {
    private static final Set<String> allSupportedChartDrawers = new TreeSet<>(); // alphabetical order...
    public static String[] paths = new String[]{"megan.chart.drawers"};

    /**
     * creates a copy of all chart drawers
     *
     * @return chart drawers
     */
    public static Map<String, IChartDrawer> createChartDrawers() {
        synchronized (allSupportedChartDrawers) { // only want to fill allSupportedChartDrawers once
            final boolean fillAllSupportedDrawers = (allSupportedChartDrawers.size() == 0);

            final Map<String, IChartDrawer> name2DrawerInstance = new HashMap<>();
            for (Object object : PluginClassLoader.getInstances(IChartDrawer.class,paths)) {
                if (object instanceof IChartDrawer) {
                    final IChartDrawer drawer = (IChartDrawer) object;
                    if (!(drawer instanceof MultiChartDrawer) && drawer.isEnabled()) {
                        name2DrawerInstance.put(drawer.getChartDrawerName(), drawer);
                        if (fillAllSupportedDrawers && !(object instanceof Plot2DDrawer))
                            allSupportedChartDrawers.add(drawer.getChartDrawerName());
                    }
                }
            }
            return name2DrawerInstance;
        }
    }

    /**
     * gets the list of all supported drawers
     *
     * @return all supported drawers
     */
    public static Set<String> getAllSupportedChartDrawers() {
        if (allSupportedChartDrawers.size() == 0)
            createChartDrawers();
        return allSupportedChartDrawers;
    }
}
