package bean;

/**
 * 
 * @author LD
 *
 */
public class User {
    private String name;  // name
    private String ip;        // ip

    public User(String name, String ip) {
        this.name = name;
        this.ip = ip;
    }

    public User(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

}
