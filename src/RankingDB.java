import java.io.File;
import java.sql.*;

public class RankingDB {
    public final String filePath = "Minesweeper.db";
    private Connection connection;
    private Statement statement;

    public RankingDB() {
        try {
            String dbFileName = "jdbc:sqlite:Minesweeper.db";
            this.connection = DriverManager.getConnection(dbFileName);
            this.statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace(System.err);
        }
    }

    public void createTable() {
        File file = new File(filePath);
        if(file.exists()) return;
        try {
            statement.executeUpdate("create table player (id integer, name string)");
            statement.executeUpdate("insert into player values(1, 'thomas')");
            statement.executeUpdate("insert into player values(2, 'brezina')");

        } catch (SQLException e) {
            e.printStackTrace(System.err);
        }
    }

    public void printTable() {
        try {
            ResultSet rs = statement.executeQuery("select * from player");
            while (rs.next()) {
                // read the result set
                System.out.println("name = " + rs.getString("name"));
                System.out.println("id = " + rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace(System.err);
        }
    }

    private void updateTable() {
    }

    private void deleteTable() {

    }
}
