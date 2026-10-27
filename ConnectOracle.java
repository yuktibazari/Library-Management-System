
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class ConnectOracle {
    public static void main(String[] args) {

        String url = "jdbc:oracle:thin:@//10.3.119.32:1521/freepdb1";
        String user = "system";
        String password = "Yuri@2006";

        String sql = "SELECT 'Connected' AS STATUS, "
                   + "'Oracle Database' AS DATABASE_NAME FROM DUAL";

        try (Connection con =
                 DriverManager.getConnection(url, user, password);
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("+------------+------------------+");
            System.out.printf("| %-10s | %-16s |%n",
                    "STATUS", "DATABASE_NAME");
            System.out.println("+------------+------------------+");

            while (rs.next()) {
                System.out.printf("| %-10s | %-16s |%n",
                        rs.getString("STATUS"),
                        rs.getString("DATABASE_NAME"));
            }

            System.out.println("+------------+------------------+");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
