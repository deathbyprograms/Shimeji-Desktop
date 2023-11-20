package com.group_finity.mascot;

import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.MascotEnvironment;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.hotspot.Hotspot;
import com.group_finity.mascot.image.MascotImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.group_finity.mascot.menu.JLongMenu;
import com.group_finity.mascot.sound.Sounds;

import javax.sound.sampled.Clip;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
// TODO Correct the many translation errors in this doc comment (and all others), and then fix the broken method link by figuring out what replaced the animate() method

/**
 * Mascot object.
 * <p>
 * Mascots' long-term, complex behaviors are represented by {@link Behavior Behaviors},
 * and their short-term, simple actions are represented by {@link Action Actions}.
 * <p>
 * Mascots have an internal timer which calls {@link Action} at a constant interval.
 * {@link Action}'s {@link #animate(Point, MascotImage, boolean)} method is called
 * to animate the mascot.
 * <p>
 * {@link Action} or exits, the other at a certain time is called {@link Behavior}, the next move to {@link Action}.
 * <p>
 * Original Author: Yuki Yamada of <a href="http://www.group-finity.com/Shimeji/">Group Finity</a>
 * <p>
 * Currently developed by Shimeji-ee Group.
 */
public class Mascot implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(Mascot.class.getName());

    private static AtomicInteger lastId = new AtomicInteger();

    private final int id;

    private String imageSet;
    /**
     * A window that displays the mascot.
     */
    private final TranslucentWindow window = NativeFactory.getInstance().newTransparentWindow();

    /**
     * Managers are managing the mascot.
     */
    private Manager manager = null;

    /**
     * Mascot ground coordinates.
     * Or feet, for example, when part of the hand is hanging.
     */
    private Point anchor = new Point(0, 0);

    /**
     * Image to display.
     */
    private MascotImage image = null;

    /**
     * Whether looking right or left.
     * The original image is treated as left, true means picture must be inverted.
     */
    private boolean lookRight = false;

    /**
     * Object representing the long-term behavior.
     */
    private Behavior behavior = null;

    /**
     * Increases with each tick of the timer.
     */
    private int time = 0;

    /**
     * Whether the animation is running.
     */
    private boolean animating = true;

    private boolean paused = false;

    /**
     * Set by behaviours when the shimeji is being dragged by the mouse cursor,
     * as opposed to hotspots or the like.
     */
    private boolean dragging = false;

    private MascotEnvironment environment = new MascotEnvironment(this);

    private String sound = null;

    protected DebugWindow debugWindow = null;

    private ArrayList<String> affordances = new ArrayList<>(5);

    private ArrayList<Hotspot> hotspots = new ArrayList<>(5);

    /**
     * Set by behaviours when the user has triggered a hotspot on this shimeji,
     * so that the shimeji knows to check for any new hotspots that emerge while
     * the mouse is held down.
     */
    private Point cursor = null;

    public Mascot(final String imageSet) {
        id = lastId.incrementAndGet();
        this.imageSet = imageSet;

        log.log(Level.INFO, "Created a mascot ({0})", this);

        // Always show on top
        getWindow().setAlwaysOnTop(true);

        // Register the mouse handler
        getWindow().asComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                Mascot.this.mousePressed(e);
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                Mascot.this.mouseReleased(e);
            }
        });
        getWindow().asComponent().addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseMoved(final MouseEvent e) {
                if (paused) {
                    refreshCursor(false);
                } else {
                    if (isHotspotClicked()) {
                        setCursorPosition(e.getPoint());
                    } else {
                        refreshCursor(e.getPoint());
                    }
                }
            }

            @Override
            public void mouseDragged(final MouseEvent e) {
                if (paused) {
                    refreshCursor(false);
                } else {
                    if (isHotspotClicked()) {
                        setCursorPosition(e.getPoint());
                    } else {
                        refreshCursor(e.getPoint());
                    }
                }
            }
        });
    }

    @Override
    public String toString() {
        return "mascot" + id;
    }

    private void mousePressed(final MouseEvent event) {
        // Switch to drag the animation when the mouse is down
        if (!isPaused() && getBehavior() != null) {
            try {
                getBehavior().mousePressed(event);
            } catch (final CantBeAliveException e) {
                log.log(Level.SEVERE, "Fatal Error", e);
                Main.showError(Main.getInstance().getLanguageBundle().getString("SevereShimejiErrorErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getString("SeeLogForDetails"));
                dispose();
            }
        }
    }

    private void mouseReleased(final MouseEvent event) {
        if (event.isPopupTrigger()) {
            SwingUtilities.invokeLater(() -> showPopup(event.getX(), event.getY()));
        } else {
            if (!isPaused() && getBehavior() != null) {
                try {
                    getBehavior().mouseReleased(event);
                } catch (final CantBeAliveException e) {
                    log.log(Level.SEVERE, "Fatal Error", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("SevereShimejiErrorErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getString("SeeLogForDetails"));
                    dispose();
                }
            }
        }
    }

    private void showPopup(final int x, final int y) {
        final JPopupMenu popup = new JPopupMenu();

        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(final PopupMenuEvent e) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
                setAnimating(true);
            }

            @Override
            public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
                setAnimating(false);
            }
        });

        // "Another One!" menu item
        final JMenuItem increaseMenu = new JMenuItem(Main.getInstance().getLanguageBundle().getString("CallAnother"));
        increaseMenu.addActionListener(event -> Main.getInstance().createMascot(imageSet));

        // "Bye Bye!" menu item
        final JMenuItem disposeMenu = new JMenuItem(Main.getInstance().getLanguageBundle().getString("Dismiss"));
        disposeMenu.addActionListener(e -> dispose());

        // "Follow Mouse!" menu item
        final JMenuItem gatherMenu = new JMenuItem(Main.getInstance().getLanguageBundle().getString("FollowCursor"));
        gatherMenu.addActionListener(event -> getManager().setBehaviorAll(Main.getInstance().getConfiguration(imageSet), Main.BEHAVIOR_GATHER, imageSet));

        // "Reduce to One!" menu item
        final JMenuItem oneMenu = new JMenuItem(Main.getInstance().getLanguageBundle().getString("DismissOthers"));
        oneMenu.addActionListener(event -> getManager().remainOne(imageSet, this));

        // "Reduce to One!" menu item
        final JMenuItem onlyOneMenu = new JMenuItem(Main.getInstance().getLanguageBundle().getString("DismissAllOthers"));
        onlyOneMenu.addActionListener(event -> getManager().remainOne(this));

        // "Restore IE!" menu item
        final JMenuItem restoreMenu = new JMenuItem(Main.getInstance().getLanguageBundle().getString("RestoreWindows"));
        restoreMenu.addActionListener(event -> NativeFactory.getInstance().getEnvironment().restoreIE());

        // Debug menu item
        final JMenuItem debugMenu = new JMenuItem(Main.getInstance().getLanguageBundle().getString("RevealStatistics"));
        debugMenu.addActionListener(event -> {
            if (debugWindow == null) {
                debugWindow = new DebugWindow();
            }
            debugWindow.setVisible(true);
        });

        // "Bye Everyone!" menu item
        final JMenuItem closeMenu = new JMenuItem(Main.getInstance().getLanguageBundle().getString("DismissAll"));
        closeMenu.addActionListener(e -> Main.getInstance().exit());

        // "Paused" Menu item
        final JMenuItem pauseMenu = new JMenuItem(isAnimating() ? Main.getInstance().getLanguageBundle().getString("PauseAnimations") : Main.getInstance().getLanguageBundle().getString("ResumeAnimations"));
        pauseMenu.addActionListener(event -> setPaused(!isPaused()));

        // Add the Behaviors submenu. It is currently slightly buggy; sometimes the menu ghosts.
        JLongMenu submenu = new JLongMenu(Main.getInstance().getLanguageBundle().getString("SetBehaviour"), 30);
        // The MenuScroller would look better than the JLongMenu, but the initial positioning is not working correctly.
        // MenuScroller.setScrollerFor(submenu, 30, 125);
        submenu.setAutoscrolls(true);
        JMenuItem item;
        Configuration config = Main.getInstance().getConfiguration(getImageSet());
        Behavior behaviour;
        for (String behaviorName : config.getBehaviorNames()) {
            final String command = behaviorName;
            try {
                behaviour = Main.getInstance().getConfiguration(getImageSet()).buildBehavior(command);
                if (!behaviour.isHidden()) {
                    item = new JMenuItem(Main.getInstance().getLanguageBundle().containsKey(behaviorName) ?
                            Main.getInstance().getLanguageBundle().getString(behaviorName) :
                            behaviorName.replaceAll("([a-z])(IE)?([A-Z])", "$1 $2 $3").replaceAll("  ", " "));
                    item.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(final ActionEvent e) {
                            try {
                                setBehavior(Main.getInstance().getConfiguration(getImageSet()).buildBehavior(command));
                            } catch (Exception err) {
                                log.log(Level.SEVERE, "Error ({0})", this);
                                Main.showError(Main.getInstance().getLanguageBundle().getString("CouldNotSetBehaviourErrorMessage") + "\n" + err.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getString("SeeLogForDetails"));
                            }
                        }
                    });
                    submenu.add(item);
                }
            } catch (Exception err) {
                // just skip if something goes wrong
            }
        }

        popup.add(increaseMenu);
        popup.add(new JSeparator());
        popup.add(gatherMenu);
        popup.add(restoreMenu);
        popup.add(debugMenu);
        popup.add(new JSeparator());
        popup.add(submenu);
        popup.add(new JSeparator());
        popup.add(pauseMenu);
        popup.add(new JSeparator());
        popup.add(disposeMenu);
        popup.add(oneMenu);
        popup.add(onlyOneMenu);
        popup.add(closeMenu);

        // TODO Get the popup to close when clicking outside of it
        getWindow().asComponent().requestFocus();

        // lightweight popups expect the shimeji window to draw them if they fall inside the shimeji window boundary
        // as the shimeji window can't support this we need to set them to heavyweight
        popup.setLightWeightPopupEnabled(false);
        popup.show(getWindow().asComponent(), x, y);
    }

    void tick() {
        if (isAnimating()) {
            if (getBehavior() != null) {
                try {
                    getBehavior().next();
                } catch (final CantBeAliveException e) {
                    log.log(Level.SEVERE, "Fatal Error.", e);
                    Main.showError(Main.getInstance().getLanguageBundle().getString("CouldNotGetNextBehaviourErrorMessage") + "\n" + e.getMessage() + "\n" + Main.getInstance().getLanguageBundle().getString("SeeLogForDetails"));
                    dispose();
                }

                setTime(getTime() + 1);
            }

            if (debugWindow != null) {
                debugWindow.setBehaviour(behavior.toString().substring(9, behavior.toString().length() - 1).replaceAll("([a-z])(IE)?([A-Z])", "$1 $2 $3").replaceAll("  ", " "));
                debugWindow.setShimejiX(anchor.x);
                debugWindow.setShimejiY(anchor.y);

                Area activeWindow = environment.getActiveIE();
                debugWindow.setWindowTitle(environment.getActiveIETitle());
                debugWindow.setWindowX(activeWindow.getLeft());
                debugWindow.setWindowY(activeWindow.getTop());
                debugWindow.setWindowWidth(activeWindow.getWidth());
                debugWindow.setWindowHeight(activeWindow.getHeight());

                Area workArea = environment.getWorkArea();
                debugWindow.setEnvironmentX(workArea.getLeft());
                debugWindow.setEnvironmentY(workArea.getTop());
                debugWindow.setEnvironmentWidth(workArea.getWidth());
                debugWindow.setEnvironmentHeight(workArea.getHeight());
            }
        }
    }

    public void apply() {
        if (isAnimating()) {
            // Make sure there's an image
            if (getImage() != null) {
                // Set the window region
                getWindow().asComponent().setBounds(getBounds());

                // Set Images
                getWindow().setImage(getImage().getImage());

                // Display
                if (!getWindow().asComponent().isVisible()) {
                    getWindow().asComponent().setVisible(true);
                }

                // Redraw
                getWindow().updateImage();
            } else {
                if (getWindow().asComponent().isVisible()) {
                    getWindow().asComponent().setVisible(false);
                }
            }

            // play sound if requested
            if (!Sounds.isMuted() && sound != null && Sounds.contains(sound)) {
                synchronized (log) {
                    Clip clip = Sounds.getSound(sound);
                    if (!clip.isRunning()) {
                        clip.stop();
                        clip.setMicrosecondPosition(0);
                        clip.start();
                    }
                }
            }
        }
    }

    public void dispose() {
        log.log(Level.INFO, "destroy mascot ({0})", this);

        if (debugWindow != null) {
            debugWindow.setVisible(false);
            debugWindow = null;
        }

        animating = false;
        getWindow().dispose();
        if (getManager() != null) {
            getManager().remove(this);
        }
    }

    private void refreshCursor(Point position) {
        boolean useHand = hotspots.stream().anyMatch(hotspot -> hotspot.contains(this, position));

        refreshCursor(useHand);
    }

    private void refreshCursor(Boolean useHand) {
        getWindow().asComponent().setCursor(Cursor.getPredefinedCursor(useHand ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public Manager getManager() {
        return manager;
    }

    public void setManager(final Manager manager) {
        this.manager = manager;
    }

    public Point getAnchor() {
        return anchor;
    }

    public void setAnchor(Point anchor) {
        this.anchor = anchor;
    }

    public MascotImage getImage() {
        return image;
    }

    public void setImage(final MascotImage image) {
        this.image = image;
    }

    public boolean isLookRight() {
        return lookRight;
    }

    public void setLookRight(final boolean lookRight) {
        this.lookRight = lookRight;
    }

    public Rectangle getBounds() {
        if (getImage() != null) {
            // Central area of the window find the image coordinates and ground coordinates. The centre has already been adjusted for scaling
            final int top = getAnchor().y - getImage().getCenter().y;
            final int left = getAnchor().x - getImage().getCenter().x;

            return new Rectangle(left, top, getImage().getSize().width, getImage().getSize().height);
        } else {
            // as we have no image let's return what we were last frame
            return getWindow().asComponent().getBounds();
        }
    }

    public int getTime() {
        return time;
    }

    private void setTime(final int time) {
        this.time = time;
    }

    public Behavior getBehavior() {
        return behavior;
    }

    public void setBehavior(final Behavior behavior) throws CantBeAliveException {
        this.behavior = behavior;
        this.behavior.init(this);
    }

    public int getCount() {
        return manager != null ? getManager().getCount(imageSet) : 0;
    }

    public int getTotalCount() {
        return manager != null ? getManager().getCount() : 0;
    }

    private boolean isAnimating() {
        return animating && !paused;
    }

    private void setAnimating(final boolean animating) {
        this.animating = animating;
    }

    private TranslucentWindow getWindow() {
        return window;
    }

    public MascotEnvironment getEnvironment() {
        return environment;
    }

    public ArrayList<String> getAffordances() {
        return affordances;
    }

    public ArrayList<Hotspot> getHotspots() {
        return hotspots;
    }

    public void setImageSet(final String set) {
        imageSet = set;
    }

    public String getImageSet() {
        return imageSet;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(final String name) {
        sound = name;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(final boolean paused) {
        this.paused = paused;
    }

    public boolean isDragging() {
        return dragging;
    }

    public void setDragging(final boolean isDragging) {
        dragging = isDragging;
    }

    public boolean isHotspotClicked() {
        return cursor != null;
    }

    public Point getCursorPosition() {
        return cursor;
    }

    public void setCursorPosition(final Point point) {
        cursor = point;

        if (point == null) {
            refreshCursor(false);
        } else {
            refreshCursor(point);
        }
    }
}
