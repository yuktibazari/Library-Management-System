
import java.sql.*;
import java.util.Scanner;

public class LibraryManagement {

    static final String URL =
    "jdbc:oracle:thin:@//localhost:1521/FREEPDB1";
    static final String USER = "SYSTEM";
    static final String PASSWORD = "Yuri@2006";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        try (Connection con =
                 DriverManager.getConnection(URL, USER, PASSWORD)) {

            createTables(con);
            System.out.println("Connected to Oracle Database!");

            while (true) {
                System.out.println("\n1. Add Book");
                System.out.println("2. Search Book");
                System.out.println("3. Issue Book");
                System.out.println("4. Return Book");
                System.out.println("5. Display Books");
                System.out.println("6. Exit");
                System.out.print("Enter choice: ");

                int choice = Integer.parseInt(sc.nextLine());

                switch (choice) {
                    case 1: addBook(con); break;
                    case 2: searchBook(con); break;
                    case 3: issueBook(con); break;
                    case 4: returnBook(con); break;
                    case 5: displayBooks(con); break;
                    case 6:
                        System.out.println("Exiting...");
                        return;
                    default:
                        System.out.println("Invalid choice!");
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    static void createTables(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            try {
                st.executeUpdate(
                    "CREATE TABLE LIBRARY_BOOKS_JDBC (" +
                    "BOOK_ID NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "TITLE VARCHAR2(100) NOT NULL, " +
                    "AUTHOR VARCHAR2(100) NOT NULL, " +
                    "TOTAL_COPIES NUMBER NOT NULL, " +
                    "AVAILABLE_COPIES NUMBER NOT NULL)"
                );
            } catch (SQLException e) {
                if (e.getErrorCode() != 955) throw e;
            }

            try {
                st.executeUpdate(
                    "CREATE TABLE BOOK_ISSUES_JDBC (" +
                    "ISSUE_ID NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "BOOK_ID NUMBER REFERENCES LIBRARY_BOOKS_JDBC(BOOK_ID), " +
                    "STUDENT_NAME VARCHAR2(100) NOT NULL, " +
                    "ISSUE_DATE DATE DEFAULT SYSDATE, " +
                    "RETURNED CHAR(1) DEFAULT 'N' NOT NULL)"
                );
            } catch (SQLException e) {
                if (e.getErrorCode() != 955) throw e;
            }
        }
    }

    static void addBook(Connection con) throws SQLException {
        System.out.print("Enter title: ");
        String title = sc.nextLine();

        System.out.print("Enter author: ");
        String author = sc.nextLine();

        System.out.print("Enter total copies: ");
        int copies = Integer.parseInt(sc.nextLine());

        if (copies <= 0) {
            System.out.println("Copies must be greater than zero.");
            return;
        }

        String sql =
            "INSERT INTO LIBRARY_BOOKS_JDBC " +
            "(TITLE, AUTHOR, TOTAL_COPIES, AVAILABLE_COPIES) " +
            "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setInt(3, copies);
            ps.setInt(4, copies);
            ps.executeUpdate();
            System.out.println("Book added successfully!");
        }
    }

    static void searchBook(Connection con) throws SQLException {
        System.out.println("1. Search by title");
        System.out.println("2. Search by author");
        System.out.print("Enter choice: ");
        int choice = Integer.parseInt(sc.nextLine());

        String column;
        if (choice == 1) column = "TITLE";
        else if (choice == 2) column = "AUTHOR";
        else {
            System.out.println("Invalid choice!");
            return;
        }

        System.out.print("Enter title or author: ");
        String search = sc.nextLine();

        String sql =
            "SELECT * FROM LIBRARY_BOOKS_JDBC WHERE UPPER(" +
            column + ") LIKE UPPER(?) ORDER BY BOOK_ID";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + search + "%");
            try (ResultSet rs = ps.executeQuery()) {
                printBooks(rs);
            }
        }
    }

    static void issueBook(Connection con) throws SQLException {
        System.out.print("Enter book ID: ");
        int bookId = Integer.parseInt(sc.nextLine());

        System.out.print("Enter student name: ");
        String student = sc.nextLine();

        boolean oldAutoCommit = con.getAutoCommit();
        con.setAutoCommit(false);

        try {
            int available;

            try (PreparedStatement ps = con.prepareStatement(
                "SELECT AVAILABLE_COPIES FROM LIBRARY_BOOKS_JDBC " +
                "WHERE BOOK_ID = ? FOR UPDATE")) {
                ps.setInt(1, bookId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Book not found!");
                        con.rollback();
                        return;
                    }
                    available = rs.getInt(1);
                }
            }

            if (available <= 0) {
                System.out.println("No copies available!");
                con.rollback();
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO BOOK_ISSUES_JDBC (BOOK_ID, STUDENT_NAME) " +
                "VALUES (?, ?)")) {
                ps.setInt(1, bookId);
                ps.setString(2, student);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(
                "UPDATE LIBRARY_BOOKS_JDBC " +
                "SET AVAILABLE_COPIES = AVAILABLE_COPIES - 1 " +
                "WHERE BOOK_ID = ?")) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }

            con.commit();
            System.out.println("Book issued successfully!");

        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }

    static void returnBook(Connection con) throws SQLException {
        System.out.print("Enter issue ID: ");
        int issueId = Integer.parseInt(sc.nextLine());

        boolean oldAutoCommit = con.getAutoCommit();
        con.setAutoCommit(false);

        try {
            int bookId;

            try (PreparedStatement ps = con.prepareStatement(
                "SELECT BOOK_ID FROM BOOK_ISSUES_JDBC " +
                "WHERE ISSUE_ID = ? AND RETURNED = 'N' FOR UPDATE")) {
                ps.setInt(1, issueId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println(
                            "Invalid issue ID or already returned!"
                        );
                        con.rollback();
                        return;
                    }
                    bookId = rs.getInt(1);
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                "UPDATE BOOK_ISSUES_JDBC SET RETURNED = 'Y' " +
                "WHERE ISSUE_ID = ?")) {
                ps.setInt(1, issueId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(
                "UPDATE LIBRARY_BOOKS_JDBC " +
                "SET AVAILABLE_COPIES = AVAILABLE_COPIES + 1 " +
                "WHERE BOOK_ID = ?")) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }

            con.commit();
            System.out.println("Book returned successfully!");

        } catch (SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }

    static void displayBooks(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT * FROM LIBRARY_BOOKS_JDBC ORDER BY BOOK_ID")) {
            printBooks(rs);
        }
    }

    static void printBooks(ResultSet rs) throws SQLException {
        boolean found = false;

        System.out.printf("%-5s %-25s %-22s %-8s %-10s%n",
            "ID", "TITLE", "AUTHOR", "TOTAL", "AVAILABLE");

        System.out.println(
            "---------------------------------------------------------------------"
        );

        while (rs.next()) {
            found = true;
            System.out.printf("%-5d %-25s %-22s %-8d %-10d%n",
                rs.getInt("BOOK_ID"),
                rs.getString("TITLE"),
                rs.getString("AUTHOR"),
                rs.getInt("TOTAL_COPIES"),
                rs.getInt("AVAILABLE_COPIES"));
        }

        if (!found) System.out.println("No books found.");
    }
}