// InventoryManagementSystem.java
// Requires: mysql-connector-java-X.jar and itextpdf-5.5.13.3.jar on the classpath.

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.io.FileOutputStream;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/inventory_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "Vansh@221131";
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        try {
            // Ensure driver is registered (helps when running with manual classpath)
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) { }
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USER, PASS);
        }
        return connection;
    }
}

public class InventoryManagementSystem extends JFrame {
    // fields
    private JTextField txtId, txtName, txtQuantity, txtPrice, txtSearchId;
    private JButton btnAdd, btnUpdate, btnDelete, btnSearch, btnClear, btnRefresh;
    private JButton btnAddToCart, btnClearCart, btnGenerateReceipt;
    private JTable inventoryTable;
    private DefaultTableModel inventoryModel;
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JLabel lblTime, lblTotalItems, lblTotalValue;

    // colors
    private final Color PRIMARY = new Color(33, 47, 60);
    private final Color ACCENT = new Color(39, 174, 96);
    private final Color DANGER = new Color(192, 57, 43);
    private final Color BG = new Color(245, 245, 245);

    @SuppressWarnings("unused")
    private final NumberFormat currency = NumberFormat.getCurrencyInstance();

    public InventoryManagementSystem() {
        setTitle("🛒 THE MALL - POS Cash Counter");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        // Header
        add(createHeader(), BorderLayout.NORTH);

        // Main split
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLeftPanel(), createRightPanel());
        split.setDividerLocation(380);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);

        // load data & clocks
        loadInventory();
        updateStatistics();
        startClock();

        setVisible(true);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // Left: logo + title
        JLabel lblLogo = new JLabel("🛒  THE MALL - CASH COUNTER");
        lblLogo.setForeground(Color.WHITE);
        lblLogo.setFont(new Font("SansSerif", Font.BOLD, 20));
        header.add(lblLogo, BorderLayout.WEST);

        // Right: clock
        lblTime = new JLabel();
        lblTime.setForeground(Color.WHITE);
        lblTime.setFont(new Font("SansSerif", Font.BOLD, 14));
        header.add(lblTime, BorderLayout.EAST);

        return header;
    }

    @SuppressWarnings("unused")
    private JPanel createLeftPanel() {
        JPanel left = new JPanel(new BorderLayout(12, 12));
        left.setBackground(BG);
        left.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Form card
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200,200,200)), BorderFactory.createEmptyBorder(12,12,12,12)
        ));

        JLabel title = new JLabel("ITEM DETAILS");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0,10)));

        txtId = createField();
        txtName = createField();
        txtQuantity = createField();
        txtPrice = createField();

        card.add(makeFieldRow("Item ID:", txtId));
        card.add(Box.createRigidArea(new Dimension(0,6)));
        card.add(makeFieldRow("Name:", txtName));
        card.add(Box.createRigidArea(new Dimension(0,6)));
        card.add(makeFieldRow("Quantity:", txtQuantity));
        card.add(Box.createRigidArea(new Dimension(0,6)));
        card.add(makeFieldRow("Price (₹):", txtPrice));
        card.add(Box.createRigidArea(new Dimension(0,10)));

        // Actions
        JPanel actions = new JPanel(new GridLayout(3,2,8,8));
        actions.setOpaque(false);
        btnAdd = makeButton("ADD ITEM", ACCENT);
        btnUpdate = makeButton("UPDATE", ACCENT.darker());
        btnDelete = makeButton("DELETE", DANGER);
        btnClear = makeButton("CLEAR", Color.GRAY);
        btnSearch = makeButton("SEARCH", new Color(241,196,15));
        btnRefresh = makeButton("REFRESH", PRIMARY.darker());

        actions.add(btnAdd); actions.add(btnUpdate);
        actions.add(btnDelete); actions.add(btnClear);
        actions.add(btnSearch); actions.add(btnRefresh);

        card.add(actions);

        // Quick search card
        JPanel searchCard = new JPanel();
        searchCard.setLayout(new BoxLayout(searchCard, BoxLayout.Y_AXIS));
        searchCard.setBackground(Color.WHITE);
        searchCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200,200,200)), BorderFactory.createEmptyBorder(10,10,10,10)
        ));
        JLabel q = new JLabel("QUICK SEARCH");
        q.setFont(new Font("SansSerif", Font.BOLD, 14));
        q.setForeground(PRIMARY);
        txtSearchId = createField();
        searchCard.add(q);
        searchCard.add(Box.createRigidArea(new Dimension(0,8)));
        searchCard.add(makeFieldRow("Search by ID:", txtSearchId));

        left.add(card, BorderLayout.CENTER);
        left.add(searchCard, BorderLayout.SOUTH);

        // Hook actions
        btnAdd.addActionListener(e -> addItem());
        btnUpdate.addActionListener(e -> updateItem());
        btnDelete.addActionListener(e -> deleteItem());
        btnClear.addActionListener(e -> clearForm());
        btnSearch.addActionListener(e -> quickSearch());
        btnRefresh.addActionListener(e -> { loadInventory(); updateStatistics(); });

        return left;
    }

    private JPanel createRightPanel() {
        JPanel right = new JPanel(new BorderLayout(12,12));
        right.setBackground(BG);
        right.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));

        // Stats on top
        JPanel stats = new JPanel(new GridLayout(1,2,10,10));
        stats.setOpaque(false);
        lblTotalItems = new JLabel("Total Items: 0");
        lblTotalValue = new JLabel("Total Value: ₹0.00");
        lblTotalItems.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTotalValue.setFont(new Font("SansSerif", Font.BOLD, 14));
        stats.add(makeStatCard(lblTotalItems, "ITEMS"));
        stats.add(makeStatCard(lblTotalValue, "VALUE"));
        right.add(stats, BorderLayout.NORTH);

        // Center split: inventory top, cart bottom
        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, inventoryPanel(), cartPanel());
        centerSplit.setDividerLocation(360);
        centerSplit.setResizeWeight(0.6);
        centerSplit.setContinuousLayout(true);
        right.add(centerSplit, BorderLayout.CENTER);

        return right;
    }

    private JPanel inventoryPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200,200,200)),
                BorderFactory.createEmptyBorder(8,8,8,8)
        ));

        inventoryModel = new DefaultTableModel(new String[]{"ID","Name","Qty","Price (₹)"}, 0) {
            public boolean isCellEditable(int r,int c){ return false; }
        };
        inventoryTable = new JTable(inventoryModel);
        styleTable(inventoryTable);
        p.add(new JScrollPane(inventoryTable), BorderLayout.CENTER);

        // When clicked populate form
        inventoryTable.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                int r = inventoryTable.getSelectedRow();
                if(r>=0){
                    txtId.setText(String.valueOf(inventoryModel.getValueAt(r,0)));
                    txtName.setText(String.valueOf(inventoryModel.getValueAt(r,1)));
                    txtQuantity.setText(String.valueOf(inventoryModel.getValueAt(r,2)));
                    txtPrice.setText(String.valueOf(inventoryModel.getValueAt(r,3)));
                }
            }
        });

        return p;
    }

    @SuppressWarnings("unused")
    private JPanel cartPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200,200,200)),
                BorderFactory.createEmptyBorder(8,8,8,8)
        ));

        cartModel = new DefaultTableModel(new String[]{"Item","Qty","Rate (₹)","Total (₹)"}, 0) {
            public boolean isCellEditable(int r,int c){ return false; }
        };
        cartTable = new JTable(cartModel);
        styleTable(cartTable);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setOpaque(false);
        btnAddToCart = makeButton("ADD TO CART", ACCENT);
        btnClearCart = makeButton("CLEAR CART", Color.GRAY);
        btnGenerateReceipt = makeButton("GENERATE E-RECEIPT", PRIMARY.darker());
        top.add(btnAddToCart); top.add(btnClearCart); top.add(btnGenerateReceipt);

        p.add(top, BorderLayout.NORTH);
        p.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        btnAddToCart.addActionListener(e -> addSelectedToCart());
        btnClearCart.addActionListener(e -> { cartModel.setRowCount(0); });
        btnGenerateReceipt.addActionListener(e -> showReceiptDialog());

        return p;
    }

    private void styleTable(JTable t) {
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setRowHeight(28);
        JTableHeader h = t.getTableHeader();
        h.setFont(new Font("SansSerif", Font.BOLD, 12));
        h.setBackground(PRIMARY);
        h.setForeground(Color.WHITE);
        DefaultTableCellRenderer c = new DefaultTableCellRenderer();
        c.setHorizontalAlignment(JLabel.CENTER);
        for (int i=0;i<t.getColumnCount();i++) t.getColumnModel().getColumn(i).setCellRenderer(c);
    }

    private JTextField createField() {
        JTextField f = new JTextField();
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200,200,200)),
                BorderFactory.createEmptyBorder(4,6,4,6)
        ));
        return f;
    }

    private JPanel makeFieldRow(String label, JTextField field) {
        JPanel row = new JPanel();
        row.setLayout(new BorderLayout(6,6));
        row.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return row;
    }

    private JPanel makeStatCard(JLabel label, String title) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        JLabel t = new JLabel(title);
        t.setFont(new Font("SansSerif", Font.PLAIN, 12));
        t.setForeground(Color.DARK_GRAY);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        card.add(t, BorderLayout.NORTH);
        card.add(label, BorderLayout.CENTER);
        return card;
    }

    private JButton makeButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(6,10,6,10));
        return b;
    }

    private void startClock() {
        @SuppressWarnings("unused")
        Timer timer = new Timer(1000, e -> {
            lblTime.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        });
        timer.start();
    }

    // ---- DB actions ----
    private void loadInventory() {
        inventoryModel.setRowCount(0);
        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement pst = con.prepareStatement("SELECT * FROM items ORDER BY id");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                inventoryModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        String.format("%.2f", rs.getDouble("price"))
                });
            }
        } catch (SQLException ex) {
            showError("Load error: " + ex.getMessage());
        }
    }

    private void addItem() {
        String name = txtName.getText().trim();
        String q = txtQuantity.getText().trim();
        String p = txtPrice.getText().trim();
        if (name.isEmpty() || q.isEmpty() || p.isEmpty()) { showError("Fill all fields"); return; }
        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement pst = con.prepareStatement("INSERT INTO items(name,quantity,price) VALUES(?,?,?)");
            pst.setString(1, name);
            pst.setInt(2, Integer.parseInt(q));
            pst.setDouble(3, Double.parseDouble(p));
            pst.executeUpdate();
            showSuccess("Item added");
            loadInventory(); updateStatistics(); clearForm();
        } catch (Exception ex) { showError(ex.getMessage()); }
    }

    private void updateItem() {
        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement pst = con.prepareStatement("UPDATE items SET name=?,quantity=?,price=? WHERE id=?");
            pst.setString(1, txtName.getText().trim());
            pst.setInt(2, Integer.parseInt(txtQuantity.getText().trim()));
            pst.setDouble(3, Double.parseDouble(txtPrice.getText().trim()));
            pst.setInt(4, Integer.parseInt(txtId.getText().trim()));
            int updated = pst.executeUpdate();
            if (updated>0) { showSuccess("Updated"); loadInventory(); updateStatistics(); clearForm(); }
            else showError("No such ID");
        } catch (Exception ex) { showError(ex.getMessage()); }
    }

    private void deleteItem() {
        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement pst = con.prepareStatement("DELETE FROM items WHERE id=?");
            pst.setInt(1, Integer.parseInt(txtId.getText().trim()));
            int deleted = pst.executeUpdate();
            if (deleted>0) { showSuccess("Deleted"); loadInventory(); updateStatistics(); clearForm(); }
            else showError("No such ID");
        } catch (Exception ex) { showError(ex.getMessage()); }
    }

    private void quickSearch() {
        String s = txtSearchId.getText().trim();
        if (s.isEmpty()) { showError("Enter ID"); return; }
        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement pst = con.prepareStatement("SELECT * FROM items WHERE id=?");
            pst.setInt(1, Integer.parseInt(s));
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                txtId.setText(String.valueOf(rs.getInt("id")));
                txtName.setText(rs.getString("name"));
                txtQuantity.setText(String.valueOf(rs.getInt("quantity")));
                txtPrice.setText(String.valueOf(rs.getDouble("price")));
            } else showError("Not found");
        } catch (Exception ex) { showError(ex.getMessage()); }
    }

    private void clearForm() {
        txtId.setText(""); txtName.setText(""); txtQuantity.setText(""); txtPrice.setText(""); txtSearchId.setText("");
    }

    private void updateStatistics() {
        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement pst = con.prepareStatement("SELECT COUNT(*) AS c, COALESCE(SUM(quantity*price),0) AS v FROM items");
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                lblTotalItems.setText("Total Items: " + rs.getInt("c"));
                lblTotalValue.setText("Total Value: ₹" + String.format("%.2f", rs.getDouble("v")));
            }
        } catch (Exception ex) { showError(ex.getMessage()); }
    }

    // ---- Cart and Checkout ----
    private void addSelectedToCart() {
        int r = inventoryTable.getSelectedRow();
        if (r < 0) { showError("Select an inventory item"); return; }
        String id = String.valueOf(inventoryModel.getValueAt(r,0));
        String name = String.valueOf(inventoryModel.getValueAt(r,1));
        int stock = Integer.parseInt(String.valueOf(inventoryModel.getValueAt(r,2)));
        double rate = Double.parseDouble(String.valueOf(inventoryModel.getValueAt(r,3)));
        String qtyStr = JOptionPane.showInputDialog(this, "Quantity:", "1");
        if (qtyStr == null) return;
        try {
            int qty = Integer.parseInt(qtyStr.trim());
            if (qty <= 0) throw new NumberFormatException();
            if (qty > stock) { showError("Not enough stock"); return; }
            double total = qty * rate;
            cartModel.addRow(new Object[]{ name + " (ID:" + id + ")", qty, String.format("%.2f", rate), String.format("%.2f", total) });
        } catch (NumberFormatException ex) { showError("Invalid quantity"); }
    }

    private void performCheckoutAndUpdateDB() {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                for (int i=0;i<cartModel.getRowCount();i++) {
                    String itemWithId = String.valueOf(cartModel.getValueAt(i,0));
                    int id = Integer.parseInt(itemWithId.substring(itemWithId.indexOf("ID:")+3, itemWithId.indexOf(")")));
                    int qty = Integer.parseInt(String.valueOf(cartModel.getValueAt(i,1)));
                    PreparedStatement pst = con.prepareStatement("UPDATE items SET quantity = quantity - ? WHERE id = ? AND quantity >= ?");
                    pst.setInt(1, qty);
                    pst.setInt(2, id);
                    pst.setInt(3, qty);
                    int updated = pst.executeUpdate();
                    if (updated == 0) {
                        con.rollback();
                        showError("Stock insufficient for ID: " + id + ". Checkout aborted.");
                        return;
                    }
                }
                con.commit();
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
            // remove out of stock
            try (PreparedStatement p2 = con.prepareStatement("DELETE FROM items WHERE quantity <= 0")) { p2.executeUpdate(); }
        } catch (SQLException ex) { showError(ex.getMessage()); }
    }

    // Receipt dialog & PDF save
    @SuppressWarnings("unused")
    private void showReceiptDialog() {
        if (cartModel.getRowCount() == 0) { showError("Cart is empty"); return; }

        JPanel receiptPanel = buildReceiptPanel(); // visual JPanel
        JDialog dlg = new JDialog(this, "E-Receipt Preview", true);
        dlg.setSize(420,700);
        dlg.setLayout(new BorderLayout());
        dlg.add(new JScrollPane(receiptPanel), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("💾 Save as PDF");
        JButton btnConfirm = new JButton("Confirm & Checkout");
        JButton btnCancel = new JButton("Cancel");
        bottom.add(btnSave); bottom.add(btnCancel); bottom.add(btnConfirm);
        dlg.add(bottom, BorderLayout.SOUTH);

        btnCancel.addActionListener(e -> dlg.dispose());
        btnConfirm.addActionListener(e -> {
            performCheckoutAndUpdateDB();
            showSuccess("Checkout successful!");
            cartModel.setRowCount(0);
            loadInventory();
            updateStatistics();
            dlg.dispose();
        });

        btnSave.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File("Receipt.pdf"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                java.io.File file = chooser.getSelectedFile();
                try {
                    saveReceiptToPDF(file);
                    showSuccess("Saved to: " + file.getAbsolutePath());
                } catch (Exception ex) {
                    showError("PDF save failed: " + ex.getMessage());
                }
            }
        });

        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private JPanel buildReceiptPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));

        // Header with unicode logo
        JLabel logo = new JLabel("🛒 THE MALL SUPERMARKET", SwingConstants.CENTER);
        logo.setFont(new Font("SansSerif", Font.BOLD, 16));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(logo);

        JLabel addr = new JLabel("123 Market Road, City | Phone: 9999999999", SwingConstants.CENTER);
        addr.setFont(new Font("SansSerif", Font.PLAIN, 11));
        addr.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(addr);

        p.add(Box.createRigidArea(new Dimension(0,8)));
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        StringBuilder sb = new StringBuilder();
        sb.append("-------------------------------------------------\n");
        sb.append(String.format("%-26s %5s %9s\n", "Item", "Qty", "Total"));
        sb.append("-------------------------------------------------\n");
        double subtotal = 0;
        for (int i=0;i<cartModel.getRowCount();i++) {
            String item = String.valueOf(cartModel.getValueAt(i,0));
            String qty = String.valueOf(cartModel.getValueAt(i,1));
            double tot = Double.parseDouble(String.valueOf(cartModel.getValueAt(i,3)));
            sb.append(String.format("%-26s %5s %9.2f\n", item, qty, tot));
            subtotal += tot;
        }
        sb.append("-------------------------------------------------\n");
        double tax = subtotal * 0.12; // example GST
        double grand = subtotal + tax;
        sb.append(String.format("%-26s %5s %9.2f\n", "Subtotal", "", subtotal));
        sb.append(String.format("%-26s %5s %9.2f\n", "Tax (12%)", "", tax));
        sb.append(String.format("%-26s %5s %9.2f\n", "TOTAL", "", grand));
        sb.append("-------------------------------------------------\n");
        sb.append("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")) + "\n");
        sb.append("\nThank you for shopping with us!\nVisit Again ❤️");

        area.setText(sb.toString());
        p.add(new JScrollPane(area));
        return p;
    }

    private void saveReceiptToPDF(java.io.File file) throws Exception {
        Document doc = new Document(PageSize.A6.rotate(), 20, 20, 20, 20); // compact receipt
        PdfWriter.getInstance(doc, new FileOutputStream(file));
        doc.open();

        // Fonts
        Font headerF = new Font(Font.MONOSPACED, 14, Font.BOLD);
        Font mono = new Font(Font.MONOSPACED, 10, Font.PLAIN);

        // Header
        Paragraph h = new Paragraph("🛒 THE MALL SUPERMARKET");
        h.setAlignment(Element.ALIGN_CENTER);
        doc.add(h);
        Paragraph sub = new Paragraph("123 Market Road, City | Ph: 9999999999");
        sub.setAlignment(Element.ALIGN_CENTER);
        doc.add(sub);
        doc.add(Chunk.NEWLINE);

        // Body (monospaced table-like)
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-26s %5s %9s\n", "Item", "Qty", "Total"));
        sb.append("-------------------------------------------------\n");
        double subtotal = 0;
        for (int i=0;i<cartModel.getRowCount();i++) {
            String item = String.valueOf(cartModel.getValueAt(i,0));
            String qty = String.valueOf(cartModel.getValueAt(i,1));
            double tot = Double.parseDouble(String.valueOf(cartModel.getValueAt(i,3)));
            sb.append(String.format("%-26s %5s %9.2f\n", item, qty, tot));
            subtotal += tot;
        }
        sb.append("-------------------------------------------------\n");
        double tax = subtotal * 0.12;
        double grand = subtotal + tax;
        sb.append(String.format("%-26s %5s %9.2f\n", "Subtotal", "", subtotal));
        sb.append(String.format("%-26s %5s %9.2f\n", "Tax (12%)", "", tax));
        sb.append(String.format("%-26s %5s %9.2f\n", "TOTAL", "", grand));
        sb.append("\nDate: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")) + "\n");
        sb.append("\nThank you for shopping with us!\n");

        // Add as monospaced paragraph
        Paragraph body = new Paragraph(sb.toString());
        doc.add(body);

        // Footer centered
        Paragraph footer = new Paragraph("Visit Again ❤️");
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);

        doc.close();
    }

    private void showError(String s) { JOptionPane.showMessageDialog(this, s, "Error", JOptionPane.ERROR_MESSAGE); }
    private void showSuccess(String s) { JOptionPane.showMessageDialog(this, s, "Success", JOptionPane.INFORMATION_MESSAGE); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InventoryManagementSystem());
    }
}
