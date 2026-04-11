import java.sql.*;

public class RankingDB {
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

    public void createTables() {
        try {
            statement.executeUpdate("create table if not exists beginner (id integer primary key autoincrement, name string, game_time integer, played_at text)");
            statement.executeUpdate("create table if not exists intermediate (id integer primary key autoincrement, name string, game_time integer, played_at text)");
            statement.executeUpdate("create table if not exists expert (id integer primary key autoincrement, name string, game_time integer, played_at text)");
        } catch (SQLException e) {
            e.printStackTrace(System.err);
        }
    }

    public void saveGameResult(String tableName, String name, int gameTime) {
        try {
            PreparedStatement ps = connection.prepareStatement(
                    "insert into " + tableName + " (name, game_time, played_at) values (?, ?, datetime('now','localtime'))",
                    Statement.RETURN_GENERATED_KEYS
            );

            ps.setString(1, name);
            ps.setInt(2, gameTime);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace(System.err);
        }
    }

    public void printTable(String tableName) {
        try {
            ResultSet rs = statement.executeQuery("select name, game_time, played_at from " + tableName + " order by game_time limit 10");
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            String[] columnNames = new String[columnCount];

            // table columns are 1-indexed bruh
            for(int i = 1; i <= columnCount; i++) {
              columnNames[i-1] = metaData.getColumnName(i);
            }

            int NAME_WIDTH = 15;
            int GAME_TIME_WIDTH = 12;
            int PLAYED_TIME_WIDTH = 19;

            String headerTop = String.format("~ %-"+ NAME_WIDTH +"s ~ %-"+ GAME_TIME_WIDTH +"s ~ %-"+ PLAYED_TIME_WIDTH +"s ~", repeat(NAME_WIDTH), repeat(GAME_TIME_WIDTH), repeat(PLAYED_TIME_WIDTH));
            IO.println(headerTop);
            String header = String.format("| %-"+ NAME_WIDTH +"s | %-"+ GAME_TIME_WIDTH +"s | %-"+ PLAYED_TIME_WIDTH +"s |", columnNames[0], columnNames[1], columnNames[2]);
            IO.println(header);
            IO.println(headerTop);

            int rowCount = 0;
            while (rs.next()) {
                // read the result set
                String row = String.format("| %-"+ NAME_WIDTH +"s | %-"+ GAME_TIME_WIDTH +"d | %-"+ PLAYED_TIME_WIDTH +"s |", rs.getString("name"), rs.getInt("game_time"), rs.getString("played_at"));
                IO.println(row);
                rowCount++;
            }

            for(int i = rowCount; i < 10; i++) {
                String emptyRow = String.format(
                        "| %-"+ NAME_WIDTH +"s | %-"+ GAME_TIME_WIDTH +"s | %-"+ PLAYED_TIME_WIDTH +"s |",
                        "", "", ""
                );
                IO.println(emptyRow);
            }

            IO.println(headerTop);
        } catch (SQLException e) {
            e.printStackTrace(System.err);
        }
    }

    public int getRows(String tableName) {
        try {
            ResultSet rs = statement.executeQuery("select count(*) from " + tableName);
           if(rs.next()) {
               return rs.getInt(1);
           }
        }  catch (SQLException e) {
            e.printStackTrace(System.err);
        }
        return 0;
    }

    private String repeat(int count) {
        return String.valueOf('-').repeat(Math.max(0, count));
    }

    public void closeConnection() {
        try {
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace(System.err);
        }
    }

    private void resetTable() {

    }
}
