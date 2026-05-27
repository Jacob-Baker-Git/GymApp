import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.function.Consumer;

public class StatCardPanel extends RoundedPanel {

    public StatCardPanel(String label, String value, boolean editMode, Consumer<String> onValueChanged) {
        super(14, Theme.CARD_BG());
        setLayout(new BorderLayout());
        setMaximumSize(new Dimension(9999, Scale.dp(56)));
        setPreferredSize(new Dimension(440, Scale.dp(56)));
        setBorder(BorderFactory.createEmptyBorder(Scale.dp(9), Scale.dp(14), Scale.dp(9), Scale.dp(14)));

        JLabel titleLabel = new JLabel(label);
        titleLabel.setFont(Theme.bodyBold());
        titleLabel.setForeground(Theme.TEXT());
        titleLabel.setOpaque(false);
        add(titleLabel, BorderLayout.WEST);

        if (editMode) {
            JTextField editField = new JTextField(value);
            editField.setBackground(Theme.FIELD_BG());
            editField.setForeground(Theme.TEXT());
            editField.setCaretColor(Theme.TEXT());
            editField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER(), 1, true),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
            editField.setPreferredSize(new Dimension(Scale.dp(130), Scale.dp(30)));
            editField.addActionListener(e -> onValueChanged.accept(editField.getText().trim()));
            editField.addFocusListener(new FocusAdapter() {
                @Override public void focusLost(FocusEvent e) {
                    onValueChanged.accept(editField.getText().trim());
                }
            });
            add(editField, BorderLayout.EAST);
        } else {
            String display = (value == null || value.trim().isEmpty()) ? "Tap edit" : value;
            JLabel valueLabel = new JLabel(display);
            valueLabel.setFont(Theme.body());
            valueLabel.setForeground(Theme.TEXT_MUTED());
            valueLabel.setOpaque(false);
            add(valueLabel, BorderLayout.EAST);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Accent bar on left edge
        g2.setPaint(Theme.verticalGradient(Theme.ACCENT_DARK, Scale.dp(5), getHeight()));
        g2.fillRoundRect(Scale.dp(7), Scale.dp(11), Scale.dp(4), getHeight() - Scale.dp(22), Scale.dp(4), Scale.dp(4));
        g2.dispose();
    }
}
