import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.Consumer;

public class StatCardPanel extends RoundedPanel {
    public StatCardPanel(String label, String value, boolean editMode, Consumer<String> onValueChanged) {
        super(15, Theme.CARD_BG);
        setLayout(new BorderLayout());
        setMaximumSize(new Dimension(440, 55));
        setPreferredSize(new Dimension(440, 55));
        setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel(label);
        titleLabel.setFont(Theme.BODY_BOLD);
        titleLabel.setForeground(Color.WHITE);
        add(titleLabel, BorderLayout.WEST);

        if (editMode) {
            JTextField editField = new JTextField(value);
            editField.setBackground(Theme.FIELD_BG);
            editField.setForeground(Color.WHITE);
            editField.setCaretColor(Color.WHITE);
            editField.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            editField.setPreferredSize(new Dimension(125, 28));
            editField.addActionListener(e -> onValueChanged.accept(editField.getText().trim()));
            editField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    onValueChanged.accept(editField.getText().trim());
                }
            });
            add(editField, BorderLayout.EAST);
        } else {
            JLabel valueLabel = new JLabel(value == null || value.trim().isEmpty() ? "Tap edit" : value);
            valueLabel.setFont(Theme.BODY);
            valueLabel.setForeground(Theme.TEXT_MUTED);
            add(valueLabel, BorderLayout.EAST);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(Theme.verticalGradient(Theme.ACCENT_DARK, 6, getHeight()));
        g2.fillRoundRect(8, 12, 5, getHeight() - 24, 5, 5);
        g2.dispose();
    }
}