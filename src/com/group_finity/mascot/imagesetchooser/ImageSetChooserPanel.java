package com.group_finity.mascot.imagesetchooser;

import org.netbeans.lib.awtextra.AbsoluteConstraints;
import org.netbeans.lib.awtextra.AbsoluteLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Displays information about an image set. Used by {@link ImageSetChooser}.
 */
public class ImageSetChooserPanel extends JPanel {

    public ImageSetChooserPanel() {
        initComponents();
    }

    public ImageSetChooserPanel(String name, String actions,
                                String behaviors, String imageLocation) {
        initComponents();

        this.name.setText(name);
        actionsFile.setText(actions);
        behaviorsFile.setText(behaviors);
        try {
            BufferedImage img = ImageIO.read(new File(imageLocation));
            image.setIcon(new ImageIcon(img.getScaledInstance(60, 60, Image.SCALE_DEFAULT)));
        } catch (IOException e) {
            // Doesn't matter, the image just won't show
        }
    }

    public void setCheckbox(boolean value) {
        checkbox.setSelected(value);
    }

    public String getImageSetName() {
        return name.getText();
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        checkbox = new JCheckBox();
        name = new JLabel();
        actionsFile = new JLabel();
        behaviorsFile = new JLabel();
        image = new JLabel();

        setBorder(BorderFactory.createEtchedBorder());
        setMinimumSize(new Dimension(248, 80));
        setPreferredSize(new Dimension(248, 80));
        setLayout(new AbsoluteLayout());

        checkbox.setOpaque(false);
        add(checkbox, new AbsoluteConstraints(10, 30, -1, -1));

        name.setText("Builder");
        add(name, new AbsoluteConstraints(110, 10, -1, -1));

        actionsFile.setText("img/Builder/conf/actions.xml");
        add(actionsFile, new AbsoluteConstraints(110, 30, -1, -1));

        behaviorsFile.setText("img/Builder/conf/behaviors.xml");
        add(behaviorsFile, new AbsoluteConstraints(110, 50, -1, -1));

        image.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0)));
        add(image, new AbsoluteConstraints(40, 10, 60, 60));
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JLabel actionsFile;
    private JLabel behaviorsFile;
    private JCheckBox checkbox;
    private JLabel image;
    private JLabel name;
    // End of variables declaration//GEN-END:variables

}
