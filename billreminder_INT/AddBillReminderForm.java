import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AddBillReminderForm extends JFrame {
    private JTextField billNameField;
    private JTextField amountField;
    private JTextField dueDateField;
    private JComboBox<String> frequencyComboBox;
    private JTable billReminderTable;
    private DefaultTableModel billReminderTableModel;
    private int id = 1; 
    private Connection connection;



    
    public AddBillReminderForm() {
        setTitle("Add and View Bill Reminders");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(5, 2));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        inputPanel.add(new JLabel("Bill Name:"));
        billNameField = new JTextField();
        inputPanel.add(billNameField);

        inputPanel.add(new JLabel("Amount:"));
        amountField = new JTextField();
        inputPanel.add(amountField);

        inputPanel.add(new JLabel("Due Date (yyyy-mm-dd):"));
        dueDateField = new JTextField();
        inputPanel.add(dueDateField);

        inputPanel.add(new JLabel("Frequency:"));
        frequencyComboBox = new JComboBox<>(new String[]{"Daily", "Weekly", "Monthly", "Yearly"});
        inputPanel.add(frequencyComboBox);

        JButton addButton = new JButton("Add Bill Reminder");
        addButton.addActionListener(e -> addBillReminder());
        inputPanel.add(addButton);

        JButton viewButton = new JButton("View Bill Reminders");
        viewButton.addActionListener(e -> viewBillReminders());
        inputPanel.add(viewButton);

        panel.add(inputPanel, BorderLayout.NORTH);

        billReminderTableModel = new DefaultTableModel(
                new Object[][]{},
                new String[]{"ID", "Bill Name", "Amount", "Due Date", "Frequency", "Paid"}
        );
        billReminderTable = new JTable(billReminderTableModel);
        JScrollPane scrollPane = new JScrollPane(billReminderTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        JButton paidButton = new JButton("Mark as Paid");
        paidButton.addActionListener(e -> paidButtonActionPerformed(e));
        panel.add(paidButton, BorderLayout.SOUTH);

        add(panel);
        setVisible(true);
    }



    public void addBillReminder() {
        String billName = billNameField.getText();
        Double amount = Double.parseDouble(amountField.getText());
        String dueDateString = dueDateField.getText();
        String frequency = frequencyComboBox.getSelectedItem().toString();

        try {
            // Convert the dueDateString to java.sql.Date
            java.sql.Date dueDate = convertStringToDate(dueDateString);

            try (Connection connection = CheckConnection.getConnection()) {
                // Check if user with id 1 exists
                if (!userExists(connection, id)) {
                    JOptionPane.showMessageDialog(this, "User with ID 1 does not exist. Please create a user first.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String sql = "INSERT INTO BillReminders (bill_name, amount, due_date, frequency, paid, id) VALUES (?, ?, ?, ?, ?, ?)";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, billName);
                statement.setDouble(2, amount);
                statement.setDate(3, dueDate); // Use the converted date
                statement.setString(4, frequency);
                statement.setBoolean(5, false); // Set paid status to false initially
                statement.setInt(6, id);

                int rowsInserted = statement.executeUpdate();
                if (rowsInserted > 0) {
                    JOptionPane.showMessageDialog(this, "Bill reminder added successfully");
                    viewBillReminders(); // Refresh table after adding
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to add bill reminder: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (ParseException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Invalid date format. Please use yyyy-mm-dd.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean userExists(Connection connection, int id) throws SQLException {
        String checkUserSql = "SELECT 1 FROM client_users WHERE id = ?";
        try (PreparedStatement checkUserStmt = connection.prepareStatement(checkUserSql)) {
            checkUserStmt.setInt(1, id);
            try (ResultSet rs = checkUserStmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    private java.sql.Date convertStringToDate(String dateString) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        java.util.Date parsedDate = format.parse(dateString);
        return new java.sql.Date(parsedDate.getTime());
    }

    private void viewBillReminders() {
        try (Connection connection = CheckConnection.getConnection()) {
            String sql = "SELECT * FROM BillReminders WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, id);

            ResultSet resultSet = statement.executeQuery();
            billReminderTableModel.setRowCount(0); // Clear current table data
            while (resultSet.next()) {
                int billId = resultSet.getInt("bill_id");
                String billName = resultSet.getString("bill_name");
                double amount = resultSet.getDouble("amount");
                Date dueDate = resultSet.getDate("due_date");
                String frequency = resultSet.getString("frequency");
                boolean isPaid = resultSet.getBoolean("paid");

                billReminderTableModel.addRow(new Object[]{billId, billName, amount, dueDate, frequency, isPaid ? "Yes" : "No"});
            }

            resultSet.close();
            statement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to retrieve bill reminders: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void paidButtonActionPerformed(ActionEvent evt) {
        int billID = getSelectedBillId();
        if (billID == -1) {
            return; // No bill selected, show error message already handled in getSelectedBillId method
        }

        markBillAsPaid(billID);
    }

    private void markBillAsPaid(int billID) {
        try {
            connection = CheckConnection.getConnection();
            String updateQuery = "UPDATE BillReminders SET paid = true WHERE bill_id = ?";
            PreparedStatement pst = connection.prepareStatement(updateQuery);
            pst.setInt(1, billID);
            int update = pst.executeUpdate();
            if (update > 0) {
                JOptionPane.showMessageDialog(this, "Bill reminder marked as paid successfully.");
                updateBudgetTracker(billID); // Update budget tracker
                viewBillReminders(); // Reload bill reminders after update
            } else {
                JOptionPane.showMessageDialog(this, "Failed to mark bill reminder as paid.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error marking bill reminder as paid.", "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void updateBudgetTracker(int billID) {
        try {
            connection = CheckConnection.getConnection();
            String query = "SELECT amount FROM BillReminders WHERE bill_id = ?";
            PreparedStatement pst = connection.prepareStatement(query);
            pst.setInt(1, billID);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                BigDecimal amount = rs.getBigDecimal("amount");
                updateExpenseTracker(amount);  // Call method to update expense tracker
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating budget tracker.", "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                connection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

// Method to get logged-in user's ID
    private int getUserId() {
    // Replace this with your own method to get the logged-in user's ID
      return id;
    }

    public void updateExpenseTracker(BigDecimal amount) {
     Connection connection = null;
     try {
          connection = CheckConnection.getConnection(); // Use the correct class and method to get the connection

        // Insert a transaction record for the expense
            String insertQuery = "INSERT INTO transaction (id, transaction_date, transaction_type, transaction_category, transaction_notes, money_type, transaction_amount) VALUES (?, CURRENT_DATE, ?, ?, ?, 'expense', ?)";
             PreparedStatement insertPst = connection.prepareStatement(insertQuery);
           insertPst.setInt(1, getUserId());
            insertPst.setString(2, "expense");
           insertPst.setString(3, "Bills");
           insertPst.setString(4,  "Utility bills");
            insertPst.setBigDecimal(5, amount);
            int insert = insertPst.executeUpdate();

        // Update the money balance in client_users table
        String query = "UPDATE client_users SET money_balance = money_balance - ? WHERE id = ?";
        PreparedStatement pst = connection.prepareStatement(query);
        pst.setBigDecimal(1, amount);
        pst.setInt(2, getUserId());
        int update = pst.executeUpdate();

        if (update > 0 && insert > 0) {
            JOptionPane.showMessageDialog(this, "Expense recorded successfully and budget tracker updated.");
        } else {
            JOptionPane.showMessageDialog(this, "Failed to record expense or update budget tracker.", "Error", JOptionPane.ERROR_MESSAGE);
        }

    } catch (SQLException ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error updating budget tracker or recording expense.", "Error", JOptionPane.ERROR_MESSAGE);
    } finally {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}

    

    private int getSelectedBillId() {
        int selectedRow = billReminderTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a bill reminder.", "Error", JOptionPane.ERROR_MESSAGE);
            return -1;
        }

        return (int) billReminderTableModel.getValueAt(selectedRow, 0); // Assuming the first column is the ID column
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AddBillReminderForm::new);
    }
}
