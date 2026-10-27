
import java.sql.*;
import java.util.Scanner;

public class jdbcdemo {
    public static void main(String[] args) {

        String url = "jdbc:oracle:thin:@//10.3.119.32:1521/freepdb1";
        String username = "SYSTEM";
        String password = "Yuri@2006";

        try (Connection con =
                 DriverManager.getConnection(url, username, password);
             Scanner sc = new Scanner(System.in)) {

            System.out.println("Connected to Oracle Database!");

            String createSQL =
                "CREATE TABLE STUDENTS_JDBC (" +
                "ID NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                "NAME VARCHAR2(50), MARKS NUMBER(3))";

            try (Statement stmt = con.createStatement()) {
                stmt.executeUpdate(createSQL);
                System.out.println("Table created.");
            } catch (SQLException e) {
                if (e.getErrorCode() == 955) {
                    System.out.println("Table already exists.");
                } else {
                    throw e;
                }
            }

            System.out.print("Enter student name: ");
            String name = sc.nextLine();

            System.out.print("Enter marks: ");
            int marks = Integer.parseInt(sc.nextLine());

            if (marks < 0 || marks > 100) {
                System.out.println("Marks must be between 0 and 100.");
                return;
            }

            String insertSQL =
                "INSERT INTO STUDENTS_JDBC (NAME, MARKS) VALUES (?, ?)";

            try (PreparedStatement ps =
                     con.prepareStatement(insertSQL)) {

                ps.setString(1, name);
                ps.setInt(2, marks);

                ps.executeUpdate();
                System.out.println("Student inserted successfully.");
            }

            String selectSQL =
                "SELECT ID, NAME, MARKS FROM STUDENTS_JDBC ORDER BY ID";

            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(selectSQL)) {

                System.out.println("\nID | NAME | MARKS");
                System.out.println("---------------------");

                while (rs.next()) {
                    System.out.println(
                        rs.getInt("ID") + " | " +
                        rs.getString("NAME") + " | " +
                        rs.getInt("MARKS")
                    );
                }
            }

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.out.println("Please enter marks as a valid number.");
        }
    }
}
