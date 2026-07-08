package database;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet for verifying if a user entered correct email/password during Login.
 */
@WebServlet("/LogInServlet")
public class LogInServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // IMPORTANT: Update these credentials to match the ones in SignUpServlet
    private static final String DB_URL = "jdbc:derby://localhost:1527/wifi";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // 1. Get input from Log in form
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String tac = request.getParameter("TAC");

        // 2. Reject immediately if Terms & Conditions wasn't agreed to.
        if (tac == null || tac.isEmpty()) {
            try (PrintWriter out = response.getWriter()) {
                response.setContentType("text/html;charset=UTF-8");
                out.println("<script>");
                out.println("alert('You must agree to the Terms and Conditions to log in.');");
                out.println("window.location.href='index.html';");
                out.println("</script>");
            }
            return;
        }

        // 3. Check the database and get the appropriate error message
        String errorMessage = getLoginErrorMessage(email, password);

        // 4. Redirect based on the result
        if (errorMessage == null) {
            // No error means credentials are correct
            response.sendRedirect("success.html");
        } else {
            // Redirect back to index.html with the specific error message as a URL parameter
            response.sendRedirect("index.html?error=" + URLEncoder.encode(errorMessage, "UTF-8"));
        }
    }

    /**
     * Validates the user and returns the exact error message required.
     * Returns null if the user is completely valid.
     */
    private String getLoginErrorMessage(String email, String password) {
        boolean emailExists = false;
        boolean passwordExists = false;
        boolean isCorrect = false;

        try {
            Class.forName("org.apache.derby.jdbc.ClientDriver");
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

                // Check if both email and password match for a successful login
                String exactMatchSql = "SELECT 1 FROM wifidb_table WHERE email = ? AND password = ?";
                try (PreparedStatement stmt = conn.prepareStatement(exactMatchSql)) {
                    stmt.setString(1, email);
                    stmt.setString(2, password);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) isCorrect = true;
                    }
                }

                if (isCorrect) {
                    return null; // Return null if there are no errors
                }

                // If not correct, figure out what went wrong
                // Check if the email exists
                String emailSql = "SELECT 1 FROM wifidb_table WHERE email = ?";
                try (PreparedStatement stmt = conn.prepareStatement(emailSql)) {
                    stmt.setString(1, email);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) emailExists = true;
                    }
                }

                // Check if the password exists
                String passSql = "SELECT 1 FROM wifidb_table WHERE password = ?";
                try (PreparedStatement stmt = conn.prepareStatement(passSql)) {
                    stmt.setString(1, password);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) passwordExists = true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "database connection error";
        }

        // Return the required specific error messages based on database lookups
        if (!emailExists && !passwordExists) {
            return "wrong email address and password";
        } else if (!emailExists) {
            return "wrong email address";
        } else {
            // Email exists, but exact match failed, meaning it's the wrong password for this email
            return "wrong password";
        }
    }
}