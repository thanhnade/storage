package Model;

public class NFSShare {
    private String path;
    private String clients;
    private String permissions;
    private String options;
    
    public NFSShare() {
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public String getClients() {
        return clients;
    }
    
    public void setClients(String clients) {
        this.clients = clients;
    }
    
    public String getPermissions() {
        return permissions;
    }
    
    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
    
    public String getOptions() {
        return options;
    }
    
    public void setOptions(String options) {
        this.options = options;
    }
} 