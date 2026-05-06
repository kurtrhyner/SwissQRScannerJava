package ch.qrscanner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Transparentes Vollbild-Overlay zum Aufziehen eines Bildschirmbereichs.
 * Funktioniert auf Windows, macOS und Linux.
 */
public class ScreenSelector extends JWindow {

    private Point startPt, endPt;
    private boolean selecting = false;
    private BufferedImage screenshot;
    private Rectangle screenRect;
    private final Consumer<BufferedImage> onSelected;
    private final Runnable onCancelled;

    public ScreenSelector(Consumer<BufferedImage> onSelected, Runnable onCancelled) {
        this.onSelected  = onSelected;
        this.onCancelled = onCancelled;
        setup();
    }

    private void setup() {
        // Gesamtbildschirm-Geometrie
        Rectangle combined = new Rectangle();
        for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getScreenDevices()) {
            combined = combined.union(
                gd.getDefaultConfiguration().getBounds()
            );
        }
        screenRect = combined;

        setBackground(new Color(0, 0, 0, 0));
        setBounds(combined);
        setAlwaysOnTop(true);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;

                // Halbtransparentes Overlay
                g2.setColor(new Color(0, 0, 0, 80));
                g2.fillRect(0, 0, getWidth(), getHeight());

                if (startPt != null && endPt != null) {
                    Rectangle sel = getSelection();
                    // Aufgehellter Auswahlbereich
                    g2.setColor(new Color(255, 255, 255, 40));
                    g2.fill(sel);
                    // Rahmen
                    g2.setColor(new Color(0, 120, 215));
                    g2.setStroke(new BasicStroke(2f));
                    g2.draw(sel);
                    // Grösse anzeigen
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    g2.drawString(sel.width + " × " + sel.height,
                            sel.x + 4, sel.y > 20 ? sel.y - 6 : sel.y + sel.height + 16);
                }

                // Anweisung
                String hint = "Bereich mit QR-Code markieren   |   ESC = Abbrechen";
                g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
                FontMetrics fm = g2.getFontMetrics();
                int tw = fm.stringWidth(hint);
                int tx = (getWidth() - tw) / 2;
                int ty = getHeight() - 50;
                g2.setColor(new Color(0, 0, 0, 170));
                g2.fillRoundRect(tx - 16, ty - 20, tw + 32, 32, 8, 8);
                g2.setColor(Color.WHITE);
                g2.drawString(hint, tx, ty);
            }
        };
        panel.setOpaque(false);
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        panel.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    startPt = e.getPoint(); endPt = e.getPoint();
                    selecting = true; panel.repaint();
                }
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (!selecting) return;
                selecting = false; endPt = e.getPoint();
                Rectangle sel = getSelection();
                if (sel.width < 10 || sel.height < 10) {
                    dispose(); onCancelled.run(); return;
                }
                // Screenshot-Ausschnitt
                Rectangle abs = new Rectangle(
                    screenRect.x + sel.x, screenRect.y + sel.y,
                    sel.width, sel.height
                );
                BufferedImage crop = screenshot.getSubimage(
                    sel.x, sel.y,
                    Math.min(sel.width,  screenshot.getWidth()  - sel.x),
                    Math.min(sel.height, screenshot.getHeight() - sel.y)
                );
                dispose();
                onSelected.accept(crop);
            }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                if (selecting) { endPt = e.getPoint(); panel.repaint(); }
            }
        });

        // ESC-Taste
        panel.setFocusable(true);
        panel.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    dispose(); onCancelled.run();
                }
            }
        });

        setContentPane(panel);
    }

    public void start() {
        // Screenshot VOR dem Anzeigen des Overlays
        try {
            Robot robot = new Robot();
            screenshot = robot.createScreenCapture(screenRect);
        } catch (AWTException ex) {
            screenshot = new BufferedImage(screenRect.width, screenRect.height,
                    BufferedImage.TYPE_INT_RGB);
        }
        setVisible(true);
        getContentPane().requestFocusInWindow();
    }

    private Rectangle getSelection() {
        if (startPt == null || endPt == null) return new Rectangle();
        return new Rectangle(
            Math.min(startPt.x, endPt.x),
            Math.min(startPt.y, endPt.y),
            Math.abs(endPt.x - startPt.x),
            Math.abs(endPt.y - startPt.y)
        );
    }
}
