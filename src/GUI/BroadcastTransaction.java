package GUI;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

public class BroadcastTransaction extends JDialog {
    private JPanel BroadcastTransactionPanel;
    private JButton exampleButton1;
    private JButton exampleButton2;
    private JButton exampleButton3;
    private JButton btnBack; // Dichiarazione del nuovo pulsante

    public BroadcastTransaction(JFrame parent) {
        super(null, java.awt.Dialog.ModalityType.TOOLKIT_MODAL);

        // Applica il bordo arrotondato ai pulsanti
        JButton[] buttons = {
            exampleButton1,
            exampleButton2,
            exampleButton3,
            btnBack // Aggiungi il nuovo pulsante all'array
        };

        for (JButton button : buttons) {
            if (button != null) {
                button.setBorder(new RoundedBorder(15)); // Raggio di 15
                button.setBackground(new Color(45, 137, 239)); // Colore blu moderno
                button.setForeground(Color.WHITE); // Testo bianco
                button.setFocusPainted(false);
                button.setContentAreaFilled(true);
                button.setOpaque(false);
            }
        }

        // Listener per i pulsanti
        exampleButton1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(parent,
                        "Example Button 1 clicked!",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        exampleButton2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(parent,
                        "Example Button 2 clicked!",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        exampleButton3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(parent,
                        "Example Button 3 clicked!",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        btnBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose(); // Chiude la finestra corrente
                new Dashboard(parent); // Mostra la schermata iniziale
            }
        });

        setTitle("BroadcastTransaction");
        ImageIcon icon = new ImageIcon("src/GUI/images/icons8-blockchain-2.png");
        setIconImage(icon.getImage());
        setContentPane(BroadcastTransactionPanel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setMinimumSize(new Dimension(1000, 600));
        setModal(true);
        setLocationRelativeTo(parent);
        setVisible(true);
    }

    // Classe interna per il bordo arrotondato
    private static class RoundedBorder extends AbstractBorder {
        private int radius;

        RoundedBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D round = new RoundRectangle2D.Float(x, y, width - 1, height - 1, radius, radius);
            g2.setColor(c.getForeground());
            g2.draw(round);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        BroadcastTransaction myBroadcastTransaction = new BroadcastTransaction(null);
    }
}
