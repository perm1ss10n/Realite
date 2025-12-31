package ru.realite.city.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiSessionStore {

    private final Map<UUID, GuiSession> sessions = new ConcurrentHashMap<>();

    public GuiSession getOrCreate(UUID playerId) {
        return sessions.computeIfAbsent(playerId, id -> new GuiSession());
    }

    public void clear(UUID playerId) {
        sessions.remove(playerId);
    }

    public static final class GuiSession {
        private MenuType menu = MenuType.ADMIN_MAIN;
        private int plotsPage;
        private int accessPage;
        private String selectedPlotId;
        private boolean deleteConfirmation;

        public MenuType menu() {
            return menu;
        }

        public void menu(MenuType menu) {
            this.menu = menu == null ? MenuType.ADMIN_MAIN : menu;
        }

        public int plotsPage() {
            return plotsPage;
        }

        public void plotsPage(int plotsPage) {
            this.plotsPage = Math.max(0, plotsPage);
        }

        public int accessPage() {
            return accessPage;
        }

        public void accessPage(int accessPage) {
            this.accessPage = Math.max(0, accessPage);
        }

        public String selectedPlotId() {
            return selectedPlotId;
        }

        public void selectedPlotId(String selectedPlotId) {
            this.selectedPlotId = selectedPlotId;
        }

        public boolean deleteConfirmation() {
            return deleteConfirmation;
        }

        public void deleteConfirmation(boolean deleteConfirmation) {
            this.deleteConfirmation = deleteConfirmation;
        }
    }
}
