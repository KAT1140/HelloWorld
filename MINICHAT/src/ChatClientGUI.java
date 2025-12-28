import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class ChatClientGUI extends JFrame {

    // ═══════════════════════════════════════════════════════════════════
    // 🎨 THEME SYSTEM - Light/Dark Mode
    // ═══════════════════════════════════════════════════════════════════
    private boolean isDarkMode = true;

    // Dark Theme Colors
    private static final Color DARK_BG = new Color(15, 15, 25, 220);
    private static final Color DARK_PANEL = new Color(30, 30, 50, 180);
    private static final Color DARK_LIGHT = new Color(60, 60, 90, 150);
    private static final Color DARK_BORDER = new Color(100, 100, 140, 100);
    private static final Color DARK_TEXT = new Color(255, 255, 255);
    private static final Color DARK_TEXT_DIM = new Color(140, 140, 160);
    private static final Color DARK_MSG_RECEIVED = new Color(45, 45, 70, 200);

    // Light Theme Colors
    private static final Color LIGHT_BG = new Color(240, 243, 250, 240);
    private static final Color LIGHT_PANEL = new Color(255, 255, 255, 200);
    private static final Color LIGHT_LIGHT = new Color(230, 235, 245, 180);
    private static final Color LIGHT_BORDER = new Color(200, 205, 220, 150);
    private static final Color LIGHT_TEXT = new Color(30, 30, 50);
    private static final Color LIGHT_TEXT_DIM = new Color(100, 100, 120);
    private static final Color LIGHT_MSG_RECEIVED = new Color(235, 238, 248, 220);

    // Accent Colors (shared)
    private static final Color ACCENT_PRIMARY = new Color(138, 43, 226);
    private static final Color ACCENT_SECONDARY = new Color(0, 191, 255);
    private static final Color ACCENT_GRADIENT_1 = new Color(123, 104, 238);
    private static final Color ACCENT_GRADIENT_2 = new Color(65, 105, 225);
    private static final Color MSG_SENT_BG = new Color(123, 104, 238, 200);
    private static final Color ONLINE_DOT = new Color(50, 205, 50);

    // Current theme colors (will be updated)
    private Color glassBg, glassPanel, glassLight, glassBorder, textPrimary, textDim, msgReceivedBg;

    // ═══════════════════════════════════════════════════════════════════
    // COMPONENTS
    // ═══════════════════════════════════════════════════════════════════
    private JPanel mainContentPane;
    private JPanel chatPanel;
    private JPanel sidebarPanel;
    private JPanel chatAreaPanel;
    private JTextField inputField;
    private JButton sendButton;
    private JButton themeToggleBtn;
    private JLabel statusLabel;
    private JLabel currentRoomLabel;
    private JScrollPane chatScrollPane;
    private DefaultListModel<String> roomListModel;
    private DefaultListModel<String> userListModel;
    private JList<String> roomList;
    private JList<String> userList;

    // Network
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String currentRoom = null;
    private boolean connected = false;

    // Window dragging
    private Point dragOffset;

    // Server config
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 9999;

    // Background image
    private BufferedImage darkBgImage, lightBgImage;

    public ChatClientGUI() {
        username = showStyledInputDialog();
        if (username == null || username.trim().isEmpty()) {
            username = "User" + (int) (Math.random() * 1000);
        }

        applyTheme();
        createBackgroundImages();
        setupWindow();
        setupTitleBar();
        setupMainContent();
        setupStatusBar();
        setAppIcon();

        setVisible(true);
        connectToServer();
    }

    // ═══════════════════════════════════════════════════════════════════
    // APP ICON
    // ═══════════════════════════════════════════════════════════════════
    private void setAppIcon() {
        // Create a custom chat icon
        BufferedImage icon = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icon.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background circle with gradient
        GradientPaint gradient = new GradientPaint(0, 0, ACCENT_GRADIENT_1, 64, 64, ACCENT_GRADIENT_2);
        g2.setPaint(gradient);
        g2.fillOval(4, 4, 56, 56);

        // Chat bubble shape
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(14, 18, 36, 24, 12, 12);

        // Bubble tail
        int[] xPoints = { 18, 14, 22 };
        int[] yPoints = { 38, 48, 42 };
        g2.fillPolygon(xPoints, yPoints, 3);

        // Dots in bubble
        g2.setColor(ACCENT_PRIMARY);
        g2.fillOval(20, 26, 6, 6);
        g2.fillOval(29, 26, 6, 6);
        g2.fillOval(38, 26, 6, 6);

        g2.dispose();

        setIconImage(icon);
    }

    // ═══════════════════════════════════════════════════════════════════
    // THEME SYSTEM
    // ═══════════════════════════════════════════════════════════════════
    private void applyTheme() {
        if (isDarkMode) {
            glassBg = DARK_BG;
            glassPanel = DARK_PANEL;
            glassLight = DARK_LIGHT;
            glassBorder = DARK_BORDER;
            textPrimary = DARK_TEXT;
            textDim = DARK_TEXT_DIM;
            msgReceivedBg = DARK_MSG_RECEIVED;
        } else {
            glassBg = LIGHT_BG;
            glassPanel = LIGHT_PANEL;
            glassLight = LIGHT_LIGHT;
            glassBorder = LIGHT_BORDER;
            textPrimary = LIGHT_TEXT;
            textDim = LIGHT_TEXT_DIM;
            msgReceivedBg = LIGHT_MSG_RECEIVED;
        }
    }

    private void toggleTheme() {
        isDarkMode = !isDarkMode;
        applyTheme();

        // Update theme button
        themeToggleBtn.setText(isDarkMode ? "🌙" : "☀️");
        themeToggleBtn.setToolTipText(isDarkMode ? "Switch to Light Mode" : "Switch to Dark Mode");

        // Repaint everything
        repaint();

        // Show notification
        addSystemMessage(isDarkMode ? "🌙 Đã chuyển sang Dark Mode" : "☀️ Đã chuyển sang Light Mode");
    }

    private String showStyledInputDialog() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(new Color(30, 30, 45));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel label = new JLabel("✨ Nhập tên của bạn:");
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JTextField field = new JTextField(15);
        field.setBackground(new Color(50, 50, 70));
        field.setForeground(Color.WHITE);
        field.setCaretColor(ACCENT_PRIMARY);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_PRIMARY, 2),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);

        UIManager.put("OptionPane.background", new Color(30, 30, 45));
        UIManager.put("Panel.background", new Color(30, 30, 45));

        int result = JOptionPane.showConfirmDialog(null, panel,
                "💬 MiniChat - Liquid Glass",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        return (result == JOptionPane.OK_OPTION) ? field.getText().trim() : null;
    }

    private void createBackgroundImages() {
        // Dark background
        darkBgImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = darkBgImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint gradient = new GradientPaint(0, 0, new Color(15, 15, 35), 800, 600, new Color(30, 20, 50));
        g2.setPaint(gradient);
        g2.fillRect(0, 0, 800, 600);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        RadialGradientPaint orb1 = new RadialGradientPaint(new Point2D.Float(600, 150), 200,
                new float[] { 0f, 1f }, new Color[] { new Color(138, 43, 226, 150), new Color(138, 43, 226, 0) });
        g2.setPaint(orb1);
        g2.fillOval(400, -50, 400, 400);

        RadialGradientPaint orb2 = new RadialGradientPaint(new Point2D.Float(100, 500), 250,
                new float[] { 0f, 1f }, new Color[] { new Color(0, 191, 255, 100), new Color(0, 191, 255, 0) });
        g2.setPaint(orb2);
        g2.fillOval(-100, 350, 500, 500);
        g2.dispose();

        // Light background
        lightBgImage = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        g2 = lightBgImage.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        gradient = new GradientPaint(0, 0, new Color(235, 240, 255), 800, 600, new Color(245, 245, 255));
        g2.setPaint(gradient);
        g2.fillRect(0, 0, 800, 600);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
        orb1 = new RadialGradientPaint(new Point2D.Float(600, 150), 200,
                new float[] { 0f, 1f }, new Color[] { new Color(138, 43, 226, 80), new Color(138, 43, 226, 0) });
        g2.setPaint(orb1);
        g2.fillOval(400, -50, 400, 400);

        orb2 = new RadialGradientPaint(new Point2D.Float(100, 500), 250,
                new float[] { 0f, 1f }, new Color[] { new Color(0, 191, 255, 60), new Color(0, 191, 255, 0) });
        g2.setPaint(orb2);
        g2.fillOval(-100, 350, 500, 500);
        g2.dispose();
    }

    // ═══════════════════════════════════════════════════════════════════
    // WINDOW SETUP
    // ═══════════════════════════════════════════════════════════════════
    private void setupWindow() {
        setTitle("MiniChat - Liquid Glass");
        setSize(900, 700);
        setUndecorated(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        mainContentPane = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                BufferedImage bg = isDarkMode ? darkBgImage : lightBgImage;
                if (bg != null) {
                    g.drawImage(bg, 0, 0, getWidth(), getHeight(), null);
                }
            }
        };
        mainContentPane.setOpaque(false);
        setContentPane(mainContentPane);

        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 25, 25));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 25, 25));
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════
    // CUSTOM TITLE BAR
    // ═══════════════════════════════════════════════════════════════════
    private void setupTitleBar() {
        JPanel titleBar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Path2D path = new Path2D.Float();
                int w = getWidth(), h = getHeight();
                path.moveTo(25, 0);
                path.lineTo(w - 25, 0);
                path.quadTo(w, 0, w, 25);
                path.lineTo(w, h);
                path.lineTo(0, h);
                path.lineTo(0, 25);
                path.quadTo(0, 0, 25, 0);
                path.closePath();

                g2.setColor(glassPanel);
                g2.fill(path);
                g2.setColor(glassBorder);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(path);
                g2.dispose();
            }
        };
        titleBar.setOpaque(false);
        titleBar.setPreferredSize(new Dimension(getWidth(), 50));

        // Left side - Logo and title
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        leftPanel.setOpaque(false);

        // Custom drawn chat icon instead of emoji
        JPanel logo = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Chat bubble
                g2.setColor(ACCENT_PRIMARY);
                g2.fillRoundRect(2, 4, 20, 14, 8, 8);
                // Bubble tail
                int[] xp = { 5, 2, 8 };
                int[] yp = { 16, 22, 18 };
                g2.fillPolygon(xp, yp, 3);
                // Dots
                g2.setColor(Color.WHITE);
                g2.fillOval(6, 9, 4, 4);
                g2.fillOval(11, 9, 4, 4);
                g2.fillOval(16, 9, 4, 4);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(26, 26);
            }
        };
        logo.setOpaque(false);

        JLabel title = new JLabel("MiniChat") {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(textPrimary);
                g.setFont(getFont());
                g.drawString(getText(), 0, g.getFontMetrics().getAscent());
            }
        };
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JLabel subtitle = new JLabel("Liquid Glass");
        subtitle.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        subtitle.setForeground(ACCENT_SECONDARY);

        leftPanel.add(logo);
        leftPanel.add(title);
        leftPanel.add(subtitle);

        // Right side - Theme toggle + Window controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        controls.setOpaque(false);

        // Theme toggle button - use text instead of emoji
        themeToggleBtn = createThemeButton();
        themeToggleBtn.setToolTipText("Switch to Light Mode");
        themeToggleBtn.addActionListener(e -> toggleTheme());

        JButton minimizeBtn = createWindowButton("min");
        JButton maximizeBtn = createWindowButton("max");
        JButton closeBtn = createWindowButton("close");

        minimizeBtn.addActionListener(e -> setState(Frame.ICONIFIED));
        maximizeBtn.addActionListener(e -> {
            if (getExtendedState() == Frame.MAXIMIZED_BOTH) {
                setExtendedState(Frame.NORMAL);
            } else {
                setExtendedState(Frame.MAXIMIZED_BOTH);
            }
        });
        closeBtn.addActionListener(e -> {
            disconnect();
            System.exit(0);
        });

        controls.add(themeToggleBtn);
        controls.add(minimizeBtn);
        controls.add(maximizeBtn);
        controls.add(closeBtn);

        titleBar.add(leftPanel, BorderLayout.WEST);
        titleBar.add(controls, BorderLayout.EAST);

        // Dragging
        titleBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }
        });
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point current = e.getLocationOnScreen();
                setLocation(current.x - dragOffset.x, current.y - dragOffset.y);
            }
        });

        add(titleBar, BorderLayout.NORTH);
    }

    private JButton createGlassButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isRollover()) {
                    g2.setColor(glassLight);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }

                g2.setColor(getForeground());
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(textPrimary);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setPreferredSize(new Dimension(35, 30));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // Theme toggle button with sun/moon icon
    private JButton createThemeButton() {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isRollover()) {
                    g2.setColor(glassLight);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                if (isDarkMode) {
                    // Draw moon
                    g2.setColor(new Color(255, 220, 100));
                    g2.fillOval(cx - 8, cy - 8, 16, 16);
                    g2.setColor(glassPanel);
                    g2.fillOval(cx - 4, cy - 10, 14, 14);
                } else {
                    // Draw sun
                    g2.setColor(new Color(255, 180, 50));
                    g2.fillOval(cx - 6, cy - 6, 12, 12);
                    g2.setStroke(new BasicStroke(2));
                    for (int i = 0; i < 8; i++) {
                        double angle = i * Math.PI / 4;
                        int x1 = cx + (int) (10 * Math.cos(angle));
                        int y1 = cy + (int) (10 * Math.sin(angle));
                        int x2 = cx + (int) (14 * Math.cos(angle));
                        int y2 = cy + (int) (14 * Math.sin(angle));
                        g2.drawLine(x1, y1, x2, y2);
                    }
                }
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(35, 30));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // Window control buttons (minimize, maximize, close)
    private JButton createWindowButton(String type) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color hoverColor = type.equals("close") ? new Color(220, 50, 50) : glassLight;
                if (getModel().isRollover()) {
                    g2.setColor(hoverColor);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                }

                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                Color iconColor = type.equals("close")
                        ? (getModel().isRollover() ? Color.WHITE : new Color(255, 100, 100))
                        : textPrimary;
                g2.setColor(iconColor);
                g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                switch (type) {
                    case "min":
                        g2.drawLine(cx - 5, cy, cx + 5, cy);
                        break;
                    case "max":
                        g2.drawRect(cx - 5, cy - 5, 10, 10);
                        break;
                    case "close":
                        g2.drawLine(cx - 4, cy - 4, cx + 4, cy + 4);
                        g2.drawLine(cx + 4, cy - 4, cx - 4, cy + 4);
                        break;
                }
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(35, 30));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ═══════════════════════════════════════════════════════════════════
    // MAIN CONTENT (Sidebar + Chat)
    // ═══════════════════════════════════════════════════════════════════
    private void setupMainContent() {
        JPanel mainContent = new JPanel(new BorderLayout(10, 0));
        mainContent.setOpaque(false);
        mainContent.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        sidebarPanel = createSidebar();
        chatAreaPanel = createChatArea();

        mainContent.add(sidebarPanel, BorderLayout.WEST);
        mainContent.add(chatAreaPanel, BorderLayout.CENTER);

        add(mainContent, BorderLayout.CENTER);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(glassPanel);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.setColor(glassBorder);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                g2.dispose();
            }
        };
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        // Rooms section
        JPanel roomsSection = new JPanel(new BorderLayout(0, 8));
        roomsSection.setOpaque(false);

        JLabel roomsTitle = new JLabel("📁 ROOMS");
        roomsTitle.setForeground(ACCENT_SECONDARY);
        roomsTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));

        roomListModel = new DefaultListModel<>();
        roomListModel.addElement("General");
        roomListModel.addElement("Gaming");
        roomListModel.addElement("Music");
        roomListModel.addElement("Random");

        roomList = new JList<>(roomListModel);
        roomList.setOpaque(false);
        roomList.setBackground(new Color(0, 0, 0, 0));
        roomList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roomList.setSelectionBackground(ACCENT_PRIMARY);
        roomList.setCellRenderer(new RoomListCellRenderer());
        roomList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && roomList.getSelectedValue() != null) {
                joinRoom(roomList.getSelectedValue());
            }
        });

        JScrollPane roomScroll = new JScrollPane(roomList);
        roomScroll.setOpaque(false);
        roomScroll.getViewport().setOpaque(false);
        roomScroll.setBorder(null);
        roomScroll.setPreferredSize(new Dimension(180, 150));

        roomsSection.add(roomsTitle, BorderLayout.NORTH);
        roomsSection.add(roomScroll, BorderLayout.CENTER);

        JButton addRoomBtn = createAccentButton("+ New Room");
        addRoomBtn.addActionListener(e -> {
            String roomName = JOptionPane.showInputDialog(this, "Tên phòng mới:");
            if (roomName != null && !roomName.trim().isEmpty()) {
                out.println("/create " + roomName.trim());
            }
        });
        roomsSection.add(addRoomBtn, BorderLayout.SOUTH);

        // Online users section
        JPanel usersSection = new JPanel(new BorderLayout(0, 8));
        usersSection.setOpaque(false);

        JLabel usersTitle = new JLabel("👥 ONLINE");
        usersTitle.setForeground(ACCENT_SECONDARY);
        usersTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setOpaque(false);
        userList.setBackground(new Color(0, 0, 0, 0));
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userList.setCellRenderer(new UserListCellRenderer());
        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.split(":")[0].equals(username)) {
                        String targetUser = selectedUser.split(":")[0];
                        String msg = JOptionPane.showInputDialog(ChatClientGUI.this,
                                "Nhắn riêng cho " + targetUser + ":");
                        if (msg != null && !msg.trim().isEmpty()) {
                            out.println("/pm " + targetUser + " " + msg);
                        }
                    }
                }
            }
        });

        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setOpaque(false);
        userScroll.getViewport().setOpaque(false);
        userScroll.setBorder(null);

        usersSection.add(usersTitle, BorderLayout.NORTH);
        usersSection.add(userScroll, BorderLayout.CENTER);

        JPanel sectionsPanel = new JPanel(new GridLayout(2, 1, 0, 15));
        sectionsPanel.setOpaque(false);
        sectionsPanel.add(roomsSection);
        sectionsPanel.add(usersSection);

        sidebar.add(sectionsPanel, BorderLayout.CENTER);

        return sidebar;
    }

    private JPanel createChatArea() {
        JPanel chatArea = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(glassPanel);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                g2.setColor(glassBorder);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                g2.dispose();
            }
        };
        chatArea.setOpaque(false);
        chatArea.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        currentRoomLabel = new JLabel("💬 Chọn một phòng để bắt đầu chat") {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(textPrimary);
                g.setFont(getFont());
                g.drawString(getText(), 0, g.getFontMetrics().getAscent());
            }
        };
        currentRoomLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        header.add(currentRoomLabel, BorderLayout.WEST);

        // Chat messages panel
        chatPanel = new JPanel();
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setOpaque(false);

        chatScrollPane = new JScrollPane(chatPanel);
        chatScrollPane.setOpaque(false);
        chatScrollPane.getViewport().setOpaque(false);
        chatScrollPane.setBorder(null);
        chatScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel inputArea = createInputArea();

        chatArea.add(header, BorderLayout.NORTH);
        chatArea.add(chatScrollPane, BorderLayout.CENTER);
        chatArea.add(inputArea, BorderLayout.SOUTH);

        addSystemMessage("🎉 Chào mừng đến MiniChat Liquid Glass!");
        addSystemMessage("👆 Chọn một phòng từ sidebar để bắt đầu chat");
        addSystemMessage("🌙 Nhấn nút mặt trăng/mặt trời để đổi theme!");

        return chatArea;
    }

    private JPanel createInputArea() {
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setOpaque(false);

        inputField = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(glassPanel);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                g2.setColor(glassBorder);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        inputField.setOpaque(false);
        inputField.setForeground(textPrimary);
        inputField.setCaretColor(ACCENT_PRIMARY);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        inputField.addActionListener(e -> sendMessage());

        sendButton = new JButton("Send") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gradient = new GradientPaint(0, 0, ACCENT_GRADIENT_1, getWidth(), getHeight(),
                        ACCENT_GRADIENT_2);
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);

                if (getModel().isRollover()) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                    g2.setColor(Color.WHITE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                }

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        sendButton.setOpaque(false);
        sendButton.setContentAreaFilled(false);
        sendButton.setBorderPainted(false);
        sendButton.setFocusPainted(false);
        sendButton.setPreferredSize(new Dimension(80, 45));
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.addActionListener(e -> sendMessage());

        // File attach button
        JButton fileBtn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isRollover()) {
                    g2.setColor(glassLight);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                }

                // Draw paperclip icon
                g2.setColor(ACCENT_PRIMARY);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;

                // Paperclip shape
                g2.drawArc(cx - 4, cy - 10, 8, 10, 0, 180);
                g2.drawLine(cx - 4, cy - 5, cx - 4, cy + 5);
                g2.drawArc(cx - 6, cy + 2, 12, 10, 180, 180);
                g2.drawLine(cx + 6, cy + 7, cx + 6, cy - 2);
                g2.drawArc(cx + 2, cy - 5, 8, 8, 0, 180);

                g2.dispose();
            }
        };
        fileBtn.setOpaque(false);
        fileBtn.setContentAreaFilled(false);
        fileBtn.setBorderPainted(false);
        fileBtn.setFocusPainted(false);
        fileBtn.setPreferredSize(new Dimension(45, 45));
        fileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        fileBtn.setToolTipText("Attach file");
        fileBtn.addActionListener(e -> selectAndSendFile());

        // Button panel for file + send
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(fileBtn);
        buttonPanel.add(sendButton);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        return inputPanel;
    }

    // File selection and sending
    private void selectAndSendFile() {
        if (!connected || currentRoom == null) {
            addSystemMessage("! Join a room first to send files");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select file to send");

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();

            // Check file size (limit to 5MB)
            if (file.length() > 5 * 1024 * 1024) {
                addSystemMessage("! File too large. Max size: 5MB");
                return;
            }

            try {
                // Read file and encode to Base64
                byte[] fileBytes = java.nio.file.Files.readAllBytes(file.toPath());
                String base64 = java.util.Base64.getEncoder().encodeToString(fileBytes);

                // Send file: /file filename:size:base64data
                String fileInfo = file.getName() + ":" + file.length() + ":" + base64;
                out.println("/file " + fileInfo);

                addSystemMessage(">> Sending file: " + file.getName() + " (" + formatFileSize(file.length()) + ")");

            } catch (Exception ex) {
                addSystemMessage("! Error reading file: " + ex.getMessage());
            }
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // Handle received file - show download option
    private void handleReceivedFile(String sender, String fileData) {
        // Parse filename:size:base64data
        String[] parts = fileData.split(":", 3);
        if (parts.length < 3)
            return;

        String fileName = parts[0];
        long fileSize = Long.parseLong(parts[1]);
        String base64Data = parts[2];

        boolean isSent = sender.equals(username);

        // Create file message bubble with download button
        JPanel wrapper = new JPanel(new FlowLayout(isSent ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 5));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JPanel bubble = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bgColor = isSent ? MSG_SENT_BG : msgReceivedBg;
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
            }
        };
        bubble.setOpaque(false);
        bubble.setLayout(new BorderLayout(8, 4));
        bubble.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        // Sender name
        if (!isSent) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setForeground(ACCENT_SECONDARY);
            senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            bubble.add(senderLabel, BorderLayout.NORTH);
        }

        // File info panel
        JPanel filePanel = new JPanel(new BorderLayout(10, 0));
        filePanel.setOpaque(false);

        // File icon
        JPanel fileIcon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Draw file icon
                g2.setColor(ACCENT_PRIMARY);
                g2.fillRoundRect(5, 2, 20, 26, 4, 4);
                // Folded corner
                g2.setColor(new Color(255, 255, 255, 150));
                int[] xp = { 17, 25, 25 };
                int[] yp = { 2, 2, 10 };
                g2.fillPolygon(xp, yp, 3);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(30, 30);
            }
        };
        fileIcon.setOpaque(false);

        // File name and size
        JPanel fileInfo = new JPanel(new GridLayout(2, 1));
        fileInfo.setOpaque(false);
        JLabel nameLabel = new JLabel(fileName);
        nameLabel.setForeground(isSent ? Color.WHITE : textPrimary);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        JLabel sizeLabel = new JLabel(formatFileSize(fileSize));
        sizeLabel.setForeground(isSent ? new Color(200, 200, 255) : textDim);
        sizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        fileInfo.add(nameLabel);
        fileInfo.add(sizeLabel);

        // Download button
        JButton downloadBtn = new JButton("Save") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT_PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 50));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                }
                g2.setColor(Color.WHITE);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        downloadBtn.setOpaque(false);
        downloadBtn.setContentAreaFilled(false);
        downloadBtn.setBorderPainted(false);
        downloadBtn.setFocusPainted(false);
        downloadBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        downloadBtn.setPreferredSize(new Dimension(50, 25));
        downloadBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        downloadBtn.addActionListener(e -> saveFile(fileName, base64Data));

        filePanel.add(fileIcon, BorderLayout.WEST);
        filePanel.add(fileInfo, BorderLayout.CENTER);
        filePanel.add(downloadBtn, BorderLayout.EAST);

        bubble.add(filePanel, BorderLayout.CENTER);

        wrapper.add(bubble);
        chatPanel.add(wrapper);
        chatPanel.revalidate();
        scrollToBottom();
    }

    private void saveFile(String fileName, String base64Data) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File(fileName));
        fileChooser.setDialogTitle("Save file as");

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                byte[] fileBytes = java.util.Base64.getDecoder().decode(base64Data);
                java.nio.file.Files.write(fileChooser.getSelectedFile().toPath(), fileBytes);
                addSystemMessage("[OK] File saved: " + fileChooser.getSelectedFile().getName());
            } catch (Exception ex) {
                addSystemMessage("! Error saving file: " + ex.getMessage());
            }
        }
    }

    private JButton createAccentButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(glassLight);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                if (getModel().isRollover()) {
                    g2.setColor(ACCENT_PRIMARY);
                    g2.setStroke(new BasicStroke(2));
                    g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 15, 15);
                }
                g2.setColor(textPrimary);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), x, y);
                g2.dispose();
            }
        };
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, 35));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ═══════════════════════════════════════════════════════════════════
    // STATUS BAR
    // ═══════════════════════════════════════════════════════════════════
    private void setupStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Path2D path = new Path2D.Float();
                int w = getWidth(), h = getHeight();
                path.moveTo(0, 0);
                path.lineTo(w, 0);
                path.lineTo(w, h - 25);
                path.quadTo(w, h, w - 25, h);
                path.lineTo(25, h);
                path.quadTo(0, h, 0, h - 25);
                path.closePath();

                g2.setColor(glassPanel);
                g2.fill(path);
                g2.setColor(glassBorder);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(path);
                g2.dispose();
            }
        };
        statusBar.setOpaque(false);
        statusBar.setPreferredSize(new Dimension(getWidth(), 35));

        JLabel userIcon = new JLabel("👤");
        JLabel userLabel = new JLabel(username) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(textPrimary);
                g.setFont(getFont());
                g.drawString(getText(), 0, g.getFontMetrics().getAscent());
            }
        };
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JLabel separator = new JLabel("  |  ");
        separator.setForeground(textDim);

        statusLabel = new JLabel("● Đang kết nối...");
        statusLabel.setForeground(new Color(255, 165, 0));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        statusBar.add(userIcon);
        statusBar.add(userLabel);
        statusBar.add(separator);
        statusBar.add(statusLabel);

        add(statusBar, BorderLayout.SOUTH);
    }

    // ═══════════════════════════════════════════════════════════════════
    // NETWORK
    // ═══════════════════════════════════════════════════════════════════
    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket(SERVER_IP, SERVER_PORT);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                connected = true;

                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("● Đã kết nối");
                    statusLabel.setForeground(ONLINE_DOT);
                });

                out.println("USERNAME:" + username);

                String message;
                while ((message = in.readLine()) != null) {
                    handleServerMessage(message);
                }

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("● Không thể kết nối");
                    statusLabel.setForeground(new Color(255, 100, 100));
                    addSystemMessage("❌ Không thể kết nối. Hãy chạy Server trước!");
                });
            }
        }).start();
    }

    private void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("ROOMS:")) {
                updateRoomList(message.substring(6));
            } else if (message.startsWith("ONLINE:")) {
                updateOnlineUsers(message.substring(7));
            } else if (message.startsWith("MSG:")) {
                String[] parts = message.substring(4).split(":", 3);
                if (parts.length >= 3) {
                    String room = parts[0];
                    String sender = parts[1];
                    String content = parts[2];
                    if (room.equals(currentRoom)) {
                        if (sender.equals(username)) {
                            addSentMessage(content);
                        } else {
                            addReceivedMessage(sender, content);
                        }
                    }
                }
            } else if (message.startsWith("SYSTEM:")) {
                addSystemMessage(message.substring(7));
            } else if (message.startsWith("JOINED:")) {
                String room = message.substring(7);
                currentRoom = room;
                currentRoomLabel.setText("💬 #" + room);
                chatPanel.removeAll();
                chatPanel.revalidate();
                chatPanel.repaint();
                addSystemMessage("✅ Đã tham gia phòng " + room);
            } else if (message.startsWith("PM:")) {
                String[] parts = message.substring(3).split(":", 2);
                if (parts.length >= 2) {
                    addPrivateMessage(parts[0], parts[1], false);
                }
            } else if (message.startsWith("PM_SENT:")) {
                String[] parts = message.substring(8).split(":", 2);
                if (parts.length >= 2) {
                    addPrivateMessage(parts[0], parts[1], true);
                }
            } else if (message.startsWith("ROOM_CREATED:")) {
                String newRoom = message.substring(13);
                if (!roomListModel.contains(newRoom)) {
                    roomListModel.addElement(newRoom);
                }
                addSystemMessage("[+] New room: " + newRoom);
            } else if (message.startsWith("FILE:")) {
                // FILE:room:sender:filename:size:base64data
                String[] parts = message.substring(5).split(":", 4);
                if (parts.length >= 4) {
                    String room = parts[0];
                    String sender = parts[1];
                    String fileData = parts[2];
                    if (parts.length > 3) {
                        fileData = parts[2] + ":" + parts[3];
                    }
                    if (room.equals(currentRoom)) {
                        handleReceivedFile(sender, fileData);
                    }
                }
            }
        });
    }

    private void updateRoomList(String rooms) {
        roomListModel.clear();
        if (!rooms.isEmpty()) {
            for (String room : rooms.split(",")) {
                roomListModel.addElement(room);
            }
        }
    }

    private void updateOnlineUsers(String users) {
        userListModel.clear();
        if (!users.isEmpty()) {
            for (String user : users.split(",")) {
                userListModel.addElement(user);
            }
        }
    }

    private void joinRoom(String room) {
        if (connected && out != null) {
            out.println("/join " + room);
        }
    }

    private void disconnect() {
        try {
            if (out != null)
                out.println("/leave");
            if (socket != null)
                socket.close();
        } catch (Exception ignored) {
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (message.isEmpty() || !connected || currentRoom == null)
            return;
        out.println(message);
        inputField.setText("");
    }

    // ═══════════════════════════════════════════════════════════════════
    // MESSAGE BUBBLES
    // ═══════════════════════════════════════════════════════════════════
    private void addSentMessage(String message) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 5));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        JPanel bubble = createMessageBubble(message, MSG_SENT_BG, true, null);
        wrapper.add(bubble);
        chatPanel.add(wrapper);
        chatPanel.revalidate();
        scrollToBottom();
    }

    private void addReceivedMessage(String sender, String message) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        JPanel bubble = createMessageBubble(message, msgReceivedBg, false, sender);
        wrapper.add(bubble);
        chatPanel.add(wrapper);
        chatPanel.revalidate();
        scrollToBottom();
    }

    private void addPrivateMessage(String user, String message, boolean sent) {
        JPanel wrapper = new JPanel(new FlowLayout(sent ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 5));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        Color pmColor = sent ? new Color(255, 140, 0, 200) : new Color(255, 100, 100, 200);
        String prefix = sent ? "📤 To " + user : "📥 From " + user;
        JPanel bubble = createMessageBubble(message, pmColor, sent, prefix);
        wrapper.add(bubble);
        chatPanel.add(wrapper);
        chatPanel.revalidate();
        scrollToBottom();
    }

    private void addSystemMessage(String message) {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
        wrapper.setOpaque(false);
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JLabel label = new JLabel(message);
        label.setForeground(textDim);
        label.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        wrapper.add(label);
        chatPanel.add(wrapper);
        chatPanel.revalidate();
        scrollToBottom();
    }

    private JPanel createMessageBubble(String text, Color bgColor, boolean isSent, String sender) {
        JPanel bubble = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                g2.dispose();
            }
        };
        bubble.setOpaque(false);
        bubble.setLayout(new BorderLayout(0, 3));
        bubble.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        if (sender != null && !isSent) {
            JLabel senderLabel = new JLabel(sender);
            senderLabel.setForeground(ACCENT_SECONDARY);
            senderLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
            bubble.add(senderLabel, BorderLayout.NORTH);
        }

        String wrappedText = "<html><body style='width: 200px'>" + text + "</body></html>";
        JLabel msgLabel = new JLabel(wrappedText);
        msgLabel.setForeground(isSent ? Color.WHITE : textPrimary);
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        bubble.add(msgLabel, BorderLayout.CENTER);

        String time = new SimpleDateFormat("HH:mm").format(new Date());
        JLabel timeLabel = new JLabel(time);
        timeLabel.setForeground(new Color(255, 255, 255, 150));
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        bubble.add(timeLabel, BorderLayout.SOUTH);

        return bubble;
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    // ═══════════════════════════════════════════════════════════════════
    // CUSTOM LIST RENDERERS
    // ═══════════════════════════════════════════════════════════════════
    private class RoomListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setText("  # " + value);
            label.setOpaque(false);
            label.setForeground(isSelected ? Color.WHITE : textPrimary);
            label.setBorder(BorderFactory.createEmptyBorder(8, 5, 8, 5));
            if (isSelected) {
                label.setOpaque(true);
                label.setBackground(
                        new Color(ACCENT_PRIMARY.getRed(), ACCENT_PRIMARY.getGreen(), ACCENT_PRIMARY.getBlue(), 100));
            }
            return label;
        }
    }

    private class UserListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String[] parts = value.toString().split(":");
            String displayName = parts[0];
            label.setText("  ● " + displayName);
            label.setOpaque(false);
            label.setForeground(isSelected ? Color.WHITE : ONLINE_DOT);
            label.setBorder(BorderFactory.createEmptyBorder(6, 5, 6, 5));
            if (displayName.equals(username)) {
                label.setText("  ● " + displayName + " (you)");
                label.setForeground(ACCENT_SECONDARY);
            }
            return label;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // MAIN
    // ═══════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(ChatClientGUI::new);
    }
}
